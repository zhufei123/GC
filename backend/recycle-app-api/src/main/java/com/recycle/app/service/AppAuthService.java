package com.recycle.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.app.dto.PhoneLoginDTO;
import com.recycle.app.dto.SmsCodeDTO;
import com.recycle.app.dto.WxLoginDTO;
import com.recycle.app.vo.AppLoginVO;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.entity.member.User;
import com.recycle.common.entity.store.RecycleStation;
import com.recycle.common.mapper.RecycleStationMapper;
import com.recycle.common.mapper.UserMapper;
import com.recycle.common.satoken.StpKit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppAuthService {

    private static final String SMS_CODE_KEY = "recycle:sms:code:";
    private static final String SMS_LIMIT_KEY = "recycle:sms:limit:";
    private static final String MOCK_CODE = "123456";

    private final UserMapper userMapper;
    private final RecycleStationMapper stationMapper;
    private final StringRedisTemplate redisTemplate;

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
        if (boss) {
            if (user == null || !"recycler".equals(user.getRole())) {
                throw new BizException(ErrorCode.NOT_BOSS);
            }
            checkEnabled(user);
            StpKit.BOSS.login(user.getId());
            AppLoginVO vo = buildLoginVO(user, StpKit.BOSS.getTokenValue());
            RecycleStation station = stationMapper.selectOne(
                    new LambdaQueryWrapper<RecycleStation>().eq(RecycleStation::getOwnerUserId, user.getId()));
            if (station != null) {
                vo.setStoreId(station.getId());
            }
            return vo;
        }
        boolean isNew = false;
        if (user == null) {
            user = registerCustomer(dto.getPhone());
            isNew = true;
        }
        checkEnabled(user);
        StpKit.USER.login(user.getId());
        AppLoginVO vo = buildLoginVO(user, StpKit.USER.getTokenValue());
        vo.setIsNewUser(isNew);
        return vo;
    }

    public AppLoginVO wxLogin(WxLoginDTO dto) {
        // 骨架 mock：code 即 openid，不调 code2session
        String openid = dto.getCode();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenidWx, openid));
        boolean isNew = false;
        if (user == null) {
            user = new User();
            user.setOpenidWx(openid);
            user.setNickname("微信用户" + openid.substring(Math.max(0, openid.length() - 4)));
            user.setRole("customer");
            user.setStatus(1);
            user.setBalance(BigDecimal.ZERO);
            user.setRecyclerStatus("none");
            userMapper.insert(user);
            isNew = true;
        }
        checkEnabled(user);
        StpKit.USER.login(user.getId());
        AppLoginVO vo = buildLoginVO(user, StpKit.USER.getTokenValue());
        vo.setIsNewUser(isNew);
        return vo;
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
