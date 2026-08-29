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
import java.time.LocalDateTime;
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
        String sessionKey = null;
        if (wxProps.loginConfigured()) {
            if (H5_MOCK_WX_CODE.equals(dto.getCode())) {
                throw new BizException(ErrorCode.PARAM_ERROR, "已配置微信登录，禁止 mock code");
            }
            Map<String, Object> session = jscode2session(dto.getCode());
            openid = str(session.get("openid"));
            unionid = str(session.get("unionid"));
            sessionKey = str(session.get("session_key"));
        } else {
            // 未配置 appid+secret：mock 模式，code 即 openid（H5 联调）
            openid = dto.getCode();
        }
        String wxAppid = resolveWxAppid();
        boolean boss = "boss".equalsIgnoreCase(dto.getClient());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenidWx, openid));
        boolean isNew = false;
        if (user == null) {
            if (boss) {
                throw new BizException(ErrorCode.NOT_BOSS);
            }
            user = new User();
            user.setOpenidWx(openid);
            user.setWxAppid(wxAppid);
            user.setUnionidWx(unionid);
            applyWxSession(user, sessionKey);
            user.setNickname("微信用户" + openid.substring(Math.max(0, openid.length() - 4)));
            user.setRole("customer");
            user.setStatus(1);
            user.setBalance(BigDecimal.ZERO);
            user.setRecyclerStatus("none");
            applyClientProfile(user, dto.getNickname(), dto.getAvatar(), dto.getGender(),
                    dto.getCity(), dto.getProvince(), dto.getCountry(), dto.getLanguage());
            userMapper.insert(user);
            isNew = true;
        } else {
            boolean changed = applyWxIdentity(user, wxAppid, unionid, sessionKey);
            changed |= applyClientProfile(user, dto.getNickname(), dto.getAvatar(), dto.getGender(),
                    dto.getCity(), dto.getProvince(), dto.getCountry(), dto.getLanguage());
            if (changed) {
                userMapper.updateById(user);
            }
        }
        AppLoginVO vo = loginAs(user, dto.getClient());
        if (!boss) {
            vo.setIsNewUser(isNew);
        }
        return vo;
    }

    /**
     * 客户端携带的可选资料落库：只以非空值更新，空值不覆盖已有资料。
     * 手机号不在登录接口写入（可被伪造）；正式号码走 bind-phone / bind-phone-wx / bind-phone-alipay。
     */
    private boolean applyClientProfile(User user, String nickname, String avatar, Integer gender,
                                       String city, String province, String country, String language) {
        boolean changed = false;
        if (StringUtils.hasText(nickname) && !nickname.equals(user.getNickname())) {
            user.setNickname(nickname);
            changed = true;
        }
        if (StringUtils.hasText(avatar) && !avatar.equals(user.getAvatar())) {
            user.setAvatar(avatar);
            changed = true;
        }
        if (gender != null && gender > 0 && !gender.equals(user.getGender())) {
            user.setGender(gender);
            changed = true;
        }
        if (StringUtils.hasText(city) && !city.equals(user.getCity())) {
            user.setCity(city);
            changed = true;
        }
        if (StringUtils.hasText(province) && !province.equals(user.getProvince())) {
            user.setProvince(province);
            changed = true;
        }
        if (StringUtils.hasText(country) && !country.equals(user.getCountry())) {
            user.setCountry(country);
            changed = true;
        }
        if (StringUtils.hasText(language) && !language.equals(user.getLanguage())) {
            user.setLanguage(language);
            changed = true;
        }
        return changed;
    }

    /** 颁发 openid 的小程序 appid；未配置官方密钥时记 mock，避免真实接口缺 appid */
    private String resolveWxAppid() {
        return StringUtils.hasText(wxProps.getAppid()) ? wxProps.getAppid() : "mock";
    }

    private boolean applyWxIdentity(User user, String wxAppid, String unionid, String sessionKey) {
        boolean changed = false;
        if (StringUtils.hasText(wxAppid) && !wxAppid.equals(user.getWxAppid())) {
            user.setWxAppid(wxAppid);
            changed = true;
        }
        if (StringUtils.hasText(unionid) && !unionid.equals(user.getUnionidWx())) {
            user.setUnionidWx(unionid);
            changed = true;
        }
        changed |= applyWxSession(user, sessionKey);
        return changed;
    }

    /** session_key 仅服务端保存，禁止写入 AppLoginVO / UserMeVO */
    private boolean applyWxSession(User user, String sessionKey) {
        if (!StringUtils.hasText(sessionKey) || sessionKey.equals(user.getWxSessionKey())) {
            return false;
        }
        user.setWxSessionKey(sessionKey);
        user.setWxSessionAt(LocalDateTime.now());
        return true;
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
        String userId = null;
        String openId = null;
        String accessToken = null;
        String refreshToken = null;
        Integer expiresIn = null;
        if (alipayProps.oauthConfigured()) {
            if (H5_MOCK_ALIPAY_CODE.equals(dto.getAuthCode())) {
                throw new BizException(ErrorCode.PARAM_ERROR, "已配置支付宝登录，禁止 mock authCode");
            }
            AlipayOauthClient.OauthToken token = alipayOauthClient.oauthToken(dto.getAuthCode());
            userId = token.userId();
            openId = token.openId();
            accessToken = token.accessToken();
            refreshToken = token.refreshToken();
            expiresIn = token.expiresIn();
        } else {
            // 未配置 appId+私钥：mock。2088+16 位视为 user_id，否则视为 open_id
            String code = dto.getAuthCode();
            if (looksLikeAlipayUserId(code)) {
                userId = code;
            } else {
                openId = code;
            }
        }
        String alipayAppId = resolveAlipayAppId();
        boolean boss = "boss".equalsIgnoreCase(dto.getClient());
        User user = findAlipayUser(openId, userId);
        boolean isNew = false;
        if (user == null) {
            if (boss) {
                throw new BizException(ErrorCode.NOT_BOSS);
            }
            user = new User();
            applyAlipayIdentity(user, alipayAppId, openId, userId, accessToken, refreshToken, expiresIn);
            String display = StringUtils.hasText(openId) ? openId : userId;
            user.setNickname("支付宝用户" + display.substring(Math.max(0, display.length() - 4)));
            user.setRole("customer");
            user.setStatus(1);
            user.setBalance(BigDecimal.ZERO);
            user.setRecyclerStatus("none");
            applyClientProfile(user, dto.getNickname(), dto.getAvatar(), dto.getGender(),
                    dto.getCity(), dto.getProvince(), null, null);
            userMapper.insert(user);
            isNew = true;
        } else {
            boolean changed = applyAlipayIdentity(user, alipayAppId, openId, userId, accessToken, refreshToken, expiresIn);
            changed |= applyClientProfile(user, dto.getNickname(), dto.getAvatar(), dto.getGender(),
                    dto.getCity(), dto.getProvince(), null, null);
            if (changed) {
                userMapper.updateById(user);
            }
        }
        AppLoginVO vo = loginAs(user, dto.getClient());
        if (!boss) {
            vo.setIsNewUser(isNew);
        }
        return vo;
    }

    /** 先 open_id，再 user_id，再兼容历史把 user_id 写入 openid_alipay 的记录 */
    private User findAlipayUser(String openId, String userId) {
        if (StringUtils.hasText(openId)) {
            User byOpenId = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenidAlipay, openId));
            if (byOpenId != null) {
                return byOpenId;
            }
        }
        if (StringUtils.hasText(userId)) {
            User byUserId = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getAlipayUserId, userId));
            if (byUserId != null) {
                return byUserId;
            }
            return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenidAlipay, userId));
        }
        return null;
    }

    private String resolveAlipayAppId() {
        return StringUtils.hasText(alipayProps.getAppId()) ? alipayProps.getAppId() : "mock";
    }

    /**
     * openid_alipay 存 open_id（新商户推荐）；alipay_user_id 存 2088 uid。
     * access_token / refresh_token 仅服务端，禁止下发前端。
     */
    private boolean applyAlipayIdentity(User user, String appId, String openId, String userId,
                                        String accessToken, String refreshToken, Integer expiresIn) {
        boolean changed = false;
        if (StringUtils.hasText(appId) && !appId.equals(user.getAlipayAppId())) {
            user.setAlipayAppId(appId);
            changed = true;
        }
        String openidAlipay = StringUtils.hasText(openId) ? openId : userId;
        if (StringUtils.hasText(openidAlipay) && !openidAlipay.equals(user.getOpenidAlipay())) {
            user.setOpenidAlipay(openidAlipay);
            changed = true;
        }
        if (StringUtils.hasText(userId) && !userId.equals(user.getAlipayUserId())) {
            user.setAlipayUserId(userId);
            changed = true;
        }
        if (StringUtils.hasText(accessToken) && !accessToken.equals(user.getAlipayAccessToken())) {
            user.setAlipayAccessToken(accessToken);
            changed = true;
        }
        if (StringUtils.hasText(refreshToken) && !refreshToken.equals(user.getAlipayRefreshToken())) {
            user.setAlipayRefreshToken(refreshToken);
            changed = true;
        }
        if (expiresIn != null && expiresIn > 0) {
            LocalDateTime expireAt = LocalDateTime.now().plusSeconds(expiresIn);
            if (user.getAlipayTokenExpireAt() == null || !expireAt.equals(user.getAlipayTokenExpireAt())) {
                user.setAlipayTokenExpireAt(expireAt);
                changed = true;
            }
        }
        return changed;
    }

    /** 支付宝 user_id：2088 开头共 16 位数字 */
    private static boolean looksLikeAlipayUserId(String value) {
        return StringUtils.hasText(value) && value.matches("2088\\d{12}");
    }

    private static String str(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
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
        // 先清空临时账号身份字段（openid_wx/openid_alipay 唯一键），再挂到手机号账号
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, current.getId())
                .set(User::getOpenidWx, null)
                .set(User::getWxAppid, null)
                .set(User::getUnionidWx, null)
                .set(User::getWxSessionKey, null)
                .set(User::getWxSessionAt, null)
                .set(User::getOpenidAlipay, null)
                .set(User::getAlipayAppId, null)
                .set(User::getAlipayUserId, null)
                .set(User::getAlipayAccessToken, null)
                .set(User::getAlipayRefreshToken, null)
                .set(User::getAlipayTokenExpireAt, null));
        boolean changed = false;
        if (!StringUtils.hasText(existing.getOpenidWx()) && StringUtils.hasText(current.getOpenidWx())) {
            existing.setOpenidWx(current.getOpenidWx());
            changed = true;
        }
        if (!StringUtils.hasText(existing.getWxAppid()) && StringUtils.hasText(current.getWxAppid())) {
            existing.setWxAppid(current.getWxAppid());
            changed = true;
        }
        if (!StringUtils.hasText(existing.getUnionidWx()) && StringUtils.hasText(current.getUnionidWx())) {
            existing.setUnionidWx(current.getUnionidWx());
            changed = true;
        }
        if (!StringUtils.hasText(existing.getWxSessionKey()) && StringUtils.hasText(current.getWxSessionKey())) {
            existing.setWxSessionKey(current.getWxSessionKey());
            existing.setWxSessionAt(current.getWxSessionAt());
            changed = true;
        }
        if (!StringUtils.hasText(existing.getOpenidAlipay()) && StringUtils.hasText(current.getOpenidAlipay())) {
            existing.setOpenidAlipay(current.getOpenidAlipay());
            changed = true;
        }
        if (!StringUtils.hasText(existing.getAlipayAppId()) && StringUtils.hasText(current.getAlipayAppId())) {
            existing.setAlipayAppId(current.getAlipayAppId());
            changed = true;
        }
        if (!StringUtils.hasText(existing.getAlipayUserId()) && StringUtils.hasText(current.getAlipayUserId())) {
            existing.setAlipayUserId(current.getAlipayUserId());
            changed = true;
        }
        if (!StringUtils.hasText(existing.getAlipayAccessToken()) && StringUtils.hasText(current.getAlipayAccessToken())) {
            existing.setAlipayAccessToken(current.getAlipayAccessToken());
            existing.setAlipayRefreshToken(current.getAlipayRefreshToken());
            existing.setAlipayTokenExpireAt(current.getAlipayTokenExpireAt());
            changed = true;
        }
        // 三方资料一并带到存活账号，消息身份（openid/头像等）不因合并丢失
        if (!StringUtils.hasText(existing.getAvatar()) && StringUtils.hasText(current.getAvatar())) {
            existing.setAvatar(current.getAvatar());
            changed = true;
        }
        if (!StringUtils.hasText(existing.getNickname()) && StringUtils.hasText(current.getNickname())) {
            existing.setNickname(current.getNickname());
            changed = true;
        }
        if ((existing.getGender() == null || existing.getGender() == 0)
                && current.getGender() != null && current.getGender() > 0) {
            existing.setGender(current.getGender());
            changed = true;
        }
        if (!StringUtils.hasText(existing.getCity()) && StringUtils.hasText(current.getCity())) {
            existing.setCity(current.getCity());
            changed = true;
        }
        if (!StringUtils.hasText(existing.getProvince()) && StringUtils.hasText(current.getProvince())) {
            existing.setProvince(current.getProvince());
            changed = true;
        }
        if (!StringUtils.hasText(existing.getCountry()) && StringUtils.hasText(current.getCountry())) {
            existing.setCountry(current.getCountry());
            changed = true;
        }
        if (!StringUtils.hasText(existing.getLanguage()) && StringUtils.hasText(current.getLanguage())) {
            existing.setLanguage(current.getLanguage());
            changed = true;
        }
        if (Integer.valueOf(1).equals(current.getSubscribeWx()) && !Integer.valueOf(1).equals(existing.getSubscribeWx())) {
            existing.setSubscribeWx(1);
            changed = true;
        }
        if (Integer.valueOf(1).equals(current.getSubscribeAlipay()) && !Integer.valueOf(1).equals(existing.getSubscribeAlipay())) {
            existing.setSubscribeAlipay(1);
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
        vo.setPhoneMasked(maskPhone(user.getPhone()));
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setHasWx(StringUtils.hasText(user.getOpenidWx()));
        vo.setHasAlipay(StringUtils.hasText(user.getOpenidAlipay()));
        vo.setRecyclerStatus(user.getRecyclerStatus());
        return vo;
    }

    /** 138****0001；openid 不出前端，手机号只回脱敏值 */
    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        if (phone.length() < 8) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
