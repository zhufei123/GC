package com.recycle.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.recycle.app.dto.AlipayLoginDTO;
import com.recycle.app.dto.BindPhoneAlipayDTO;
import com.recycle.app.dto.BindPhoneDTO;
import com.recycle.app.dto.BindPhoneWxDTO;
import com.recycle.app.dto.PhoneLoginDTO;
import com.recycle.app.dto.SmsCodeDTO;
import com.recycle.app.dto.WxLoginDTO;
import com.recycle.app.pay.AlipayOauthClient;
import com.recycle.app.pay.AlipayPayProperties;
import com.recycle.app.pay.WxApiClient;
import com.recycle.app.pay.WxPayProperties;
import com.recycle.app.vo.AppLoginVO;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.entity.member.User;
import com.recycle.common.entity.store.RecycleStation;
import com.recycle.common.mapper.RecycleStationMapper;
import com.recycle.common.mapper.UserMapper;
import com.recycle.common.satoken.StpKit;
import com.recycle.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppAuthService {

    private static final String SMS_CODE_KEY = "recycle:sms:code:";
    private static final String SMS_LIMIT_KEY = "recycle:sms:limit:";
    private static final String MOCK_CODE = "123456";
    /** H5 联调专用 mock code，真实配置下必须拒绝 */
    private static final String H5_MOCK_WX_CODE = "h5-mock-wx";
    private static final String H5_MOCK_ALIPAY_CODE = "h5-mock-alipay";
    private static final String WX_CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";

    private final UserMapper userMapper;
    private final RecycleStationMapper stationMapper;
    private final StringRedisTemplate redisTemplate;
    private final WxPayProperties wxProps;
    private final AlipayPayProperties alipayProps;
    private final WxApiClient wxApiClient;
    private final AlipayOauthClient alipayOauthClient;
    private final RestClient restClient = RestClient.create();

    @Value("${app.sms.mock:true}")
    private boolean smsMock;

    public void sendSmsCode(SmsCodeDTO dto) {
        String limitKey = SMS_LIMIT_KEY + dto.getPhone();
        Boolean first = redisTemplate.opsForValue().setIfAbsent(limitKey, "1", Duration.ofSeconds(60));
        if (Boolean.FALSE.equals(first)) {
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS, "验证码 60 秒内只能发送一次");
        }
        // 骨架 mock：固定 123456，不真实下发短信
        redisTemplate.opsForValue().set(SMS_CODE_KEY + dto.getPhone(), MOCK_CODE, Duration.ofMinutes(5));
        log.info("[mock sms] phone={} code={}", dto.getPhone(), MOCK_CODE);
    }

    public AppLoginVO phoneLogin(PhoneLoginDTO dto) {
        verifySmsCode(dto.getPhone(), dto.getSmsCode());
        boolean boss = "boss".equalsIgnoreCase(dto.getClient());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        boolean isNew = false;
        if (user == null) {
            if (boss) {
                throw new BizException(ErrorCode.NOT_BOSS);
            }
            user = registerCustomer(dto.getPhone());
            isNew = true;
        }
        AppLoginVO vo = loginAs(user, dto.getClient());
        if (!boss) {
            vo.setIsNewUser(isNew);
        }
        return vo;
    }

    public AppLoginVO wxLogin(WxLoginDTO dto) {
        String openid;
        String unionid = null;
        if (wxProps.loginConfigured()) {
            if (H5_MOCK_WX_CODE.equals(dto.getCode())) {
                throw new BizException(ErrorCode.PARAM_ERROR, "已配置微信登录，禁止 mock code");
            }
            Map<String, Object> session = jscode2session(dto.getCode());
            openid = (String) session.get("openid");
            unionid = (String) session.get("unionid");
        } else {
            // 未配置 appid+secret：mock 模式，code 即 openid（H5 联调）
            openid = dto.getCode();
        }
        boolean boss = "boss".equalsIgnoreCase(dto.getClient());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenidWx, openid));
        boolean isNew = false;
        if (user == null) {
            if (boss) {
                throw new BizException(ErrorCode.NOT_BOSS);
            }
            user = new User();
            user.setOpenidWx(openid);
            user.setUnionidWx(unionid);
            user.setNickname("微信用户" + openid.substring(Math.max(0, openid.length() - 4)));
            user.setRole("customer");
            user.setStatus(1);
            user.setBalance(BigDecimal.ZERO);
            user.setRecyclerStatus("none");
            userMapper.insert(user);
            isNew = true;
        } else if (StringUtils.hasText(unionid) && !unionid.equals(user.getUnionidWx())) {
            user.setUnionidWx(unionid);
            userMapper.updateById(user);
        }
        AppLoginVO vo = loginAs(user, dto.getClient());
        if (!boss) {
            vo.setIsNewUser(isNew);
        }
        return vo;
    }

    /** 小程序 jscode2session 换 openid/unionid */
    private Map<String, Object> jscode2session(String jsCode) {
        String body;
        try {
            body = restClient.get()
                    .uri(WX_CODE2SESSION_URL, wxProps.getAppid(), wxProps.getSecret(), jsCode)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.warn("[wx-login] jscode2session http error", e);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "微信登录服务暂不可用，请稍后重试");
        }
        Map<String, Object> result = JsonUtils.toMap(body);
        Object openid = result.get("openid");
        if (openid == null || !StringUtils.hasText(openid.toString())) {
            log.warn("[wx-login] jscode2session failed: {}", body);
            Object errmsg = result.get("errmsg");
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "微信登录失败：" + (errmsg == null ? "code 无效" : errmsg));
        }
        return result;
    }

    public AppLoginVO alipayLogin(AlipayLoginDTO dto) {
        String openid;
        if (alipayProps.oauthConfigured()) {
            if (H5_MOCK_ALIPAY_CODE.equals(dto.getAuthCode())) {
                throw new BizException(ErrorCode.PARAM_ERROR, "已配置支付宝登录，禁止 mock authCode");
            }
            openid = alipayOauthClient.oauthToken(dto.getAuthCode());
        } else {
            // 未配置 appId+私钥：mock 模式，authCode 即 openidAlipay（H5 联调）
            openid = dto.getAuthCode();
        }
        boolean boss = "boss".equalsIgnoreCase(dto.getClient());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenidAlipay, openid));
        boolean isNew = false;
        if (user == null) {
            if (boss) {
                throw new BizException(ErrorCode.NOT_BOSS);
            }
            user = new User();
            user.setOpenidAlipay(openid);
            user.setNickname("支付宝用户" + openid.substring(Math.max(0, openid.length() - 4)));
            user.setRole("customer");
            user.setStatus(1);
            user.setBalance(BigDecimal.ZERO);
            user.setRecyclerStatus("none");
            userMapper.insert(user);
            isNew = true;
        }
        AppLoginVO vo = loginAs(user, dto.getClient());
        if (!boss) {
            vo.setIsNewUser(isNew);
        }
        return vo;
    }

    /** 统一登录：boss 端要求 recycler 身份并返回 storeId，否则登 USER 端 */
    private AppLoginVO loginAs(User user, String client) {
        checkEnabled(user);
        if ("boss".equalsIgnoreCase(client)) {
            if (!"recycler".equals(user.getRole())) {
                throw new BizException(ErrorCode.NOT_BOSS);
            }
            StpKit.BOSS.login(user.getId());
            AppLoginVO vo = buildLoginVO(user, StpKit.BOSS.getTokenValue());
            RecycleStation station = stationMapper.selectOne(
                    new LambdaQueryWrapper<RecycleStation>().eq(RecycleStation::getOwnerUserId, user.getId()));
            if (station != null) {
                vo.setStoreId(station.getId());
            }
            return vo;
        }
        StpKit.USER.login(user.getId());
        return buildLoginVO(user, StpKit.USER.getTokenValue());
    }

    /** 三方登录后补绑手机号（短信验证码通道） */
    @Transactional
    public AppLoginVO bindPhone(Long userId, BindPhoneDTO dto) {
        verifySmsCode(dto.getPhone(), dto.getSmsCode());
        return bindVerifiedPhone(userId, dto.getPhone());
    }

    /** 微信手机号快速验证通道：code 换手机号后绑定（未配置 appid+secret 时提示走短信） */
    @Transactional
    public AppLoginVO bindPhoneWx(Long userId, BindPhoneWxDTO dto) {
        String phone = wxApiClient.getPhoneNumber(dto.getCode());
        return bindVerifiedPhone(userId, phone);
    }

    /** 支付宝加密手机号通道：encryptKey 解密后绑定（未配置时提示走短信） */
    @Transactional
    public AppLoginVO bindPhoneAlipay(Long userId, BindPhoneAlipayDTO dto) {
        if (!alipayProps.oauthConfigured()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "当前为开发 mock，请用短信绑定手机号");
        }
        if (!StringUtils.hasText(alipayProps.getEncryptKey())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "未配置解密密钥，请用短信绑定手机号");
        }
        if (!StringUtils.hasText(dto.getEncryptedData())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "encryptedData 不能为空");
        }
        String phone = decryptAlipayPhone(dto.getEncryptedData());
        return bindVerifiedPhone(userId, phone);
    }

    /** 支付宝小程序加密数据：AES/CBC（密钥 base64，IV 全零），明文 JSON 的 mobile 字段 */
    private String decryptAlipayPhone(String encryptedData) {
        String plain;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(Base64.getDecoder().decode(alipayProps.getEncryptKey()), "AES"),
                    new IvParameterSpec(new byte[16]));
            plain = new String(cipher.doFinal(Base64.getDecoder().decode(encryptedData)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("[alipay-bind] decrypt phone failed", e);
            throw new BizException(ErrorCode.PARAM_ERROR, "解密支付宝手机号失败");
        }
        Map<String, Object> result = JsonUtils.toMap(plain);
        Object mobile = result.get("mobile");
        if (mobile == null || !StringUtils.hasText(mobile.toString())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "解密支付宝手机号失败");
        }
        return mobile.toString();
    }

    /**
     * 手机号已验证后绑定：号码已注册时合并账号——openid 挪到手机号账号并改登该账号，
     * 临时 openid 账号逻辑删除；双方同渠道 openid 冲突时拒绝，不静默丢弃三方身份。
     */
    @Transactional
    public AppLoginVO bindVerifiedPhone(Long userId, String phone) {
        User current = userMapper.selectById(userId);
        if (current == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if (StringUtils.hasText(current.getPhone())) {
            if (current.getPhone().equals(phone)) {
                return buildLoginVO(current, StpKit.USER.getTokenValue());
            }
            throw new BizException(ErrorCode.PARAM_ERROR, "当前账号已绑定手机号");
        }
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (existing == null) {
            current.setPhone(phone);
            userMapper.updateById(current);
            return buildLoginVO(current, StpKit.USER.getTokenValue());
        }
        checkEnabled(existing);
        if (StringUtils.hasText(current.getOpenidWx()) && StringUtils.hasText(existing.getOpenidWx())
                && !existing.getOpenidWx().equals(current.getOpenidWx())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该手机号已绑定其他微信账号");
        }
        if (StringUtils.hasText(current.getOpenidAlipay()) && StringUtils.hasText(existing.getOpenidAlipay())
                && !existing.getOpenidAlipay().equals(current.getOpenidAlipay())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该手机号已绑定其他支付宝账号");
        }
        // 先清空临时账号 openid（openid_wx/openid_alipay 唯一键），再挂到手机号账号
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, current.getId())
                .set(User::getOpenidWx, null)
                .set(User::getUnionidWx, null)
                .set(User::getOpenidAlipay, null));
        boolean changed = false;
        if (!StringUtils.hasText(existing.getOpenidWx()) && StringUtils.hasText(current.getOpenidWx())) {
            existing.setOpenidWx(current.getOpenidWx());
            changed = true;
        }
        if (!StringUtils.hasText(existing.getUnionidWx()) && StringUtils.hasText(current.getUnionidWx())) {
            existing.setUnionidWx(current.getUnionidWx());
            changed = true;
        }
        if (!StringUtils.hasText(existing.getOpenidAlipay()) && StringUtils.hasText(current.getOpenidAlipay())) {
            existing.setOpenidAlipay(current.getOpenidAlipay());
            changed = true;
        }
        if (changed) {
            userMapper.updateById(existing);
        }
        // 临时账号逻辑删除，改登手机号账号
        userMapper.deleteById(current.getId());
        StpKit.USER.logout(userId);
        StpKit.USER.login(existing.getId());
        return buildLoginVO(existing, StpKit.USER.getTokenValue());
    }

    public void logout() {
        if (StpKit.USER.isLogin()) {
            StpKit.USER.logout();
        }
        if (StpKit.BOSS.isLogin()) {
            StpKit.BOSS.logout();
        }
    }

    private void verifySmsCode(String phone, String smsCode) {
        String stored = redisTemplate.opsForValue().get(SMS_CODE_KEY + phone);
        boolean ok = (stored != null && stored.equals(smsCode)) || (smsMock && MOCK_CODE.equals(smsCode));
        if (!ok) {
            throw new BizException(ErrorCode.SMS_CODE_ERROR);
        }
        redisTemplate.delete(SMS_CODE_KEY + phone);
    }

    private User registerCustomer(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        user.setRole("customer");
        user.setStatus(1);
        user.setBalance(BigDecimal.ZERO);
        user.setRecyclerStatus("none");
        userMapper.insert(user);
        return user;
    }

    private void checkEnabled(User user) {
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }
    }

    private AppLoginVO buildLoginVO(User user, String token) {
        AppLoginVO vo = new AppLoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setRole(user.getRole());
        vo.setHasPhone(StringUtils.hasText(user.getPhone()));
        vo.setNickname(user.getNickname());
        vo.setRecyclerStatus(user.getRecyclerStatus());
        return vo;
    }
}
