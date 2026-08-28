package com.recycle.app.pay;

import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * 微信小程序服务端 API：stable_token（Redis 缓存）+ 手机号快速验证。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxApiClient {

    private static final String STABLE_TOKEN_KEY = "recycle:wx:stable_token";
    private static final String STABLE_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/stable_token";
    private static final String GET_PHONE_URL =
            "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token={token}";

    private final WxPayProperties props;
    private final StringRedisTemplate redisTemplate;
    private final RestClient restClient = RestClient.create();

    /** 稳定版接口调用凭证，Redis 缓存至过期前 5 分钟 */
    public String stableToken() {
        String cached = redisTemplate.opsForValue().get(STABLE_TOKEN_KEY);
        if (StringUtils.hasText(cached)) {
            return cached;
        }
        String body;
        try {
            body = restClient.post()
                    .uri(STABLE_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("grant_type", "client_credential",
                            "appid", props.getAppid(),
                            "secret", props.getSecret()))
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.warn("[wx-api] stable_token http error", e);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "微信服务暂不可用，请稍后重试");
        }
        Map<String, Object> result = JsonUtils.toMap(body);
        Object token = result.get("access_token");
        if (token == null || !StringUtils.hasText(token.toString())) {
            log.warn("[wx-api] stable_token failed: {}", body);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "获取微信凭证失败：" + result.get("errmsg"));
        }
        long expiresIn = result.get("expires_in") instanceof Number n ? n.longValue() : 7200L;
        redisTemplate.opsForValue().set(STABLE_TOKEN_KEY, token.toString(),
                Duration.ofSeconds(Math.max(60, expiresIn - 300)));
        return token.toString();
    }

    /** 手机号快速验证：code 换纯手机号（purePhoneNumber） */
    public String getPhoneNumber(String code) {
        if (!props.loginConfigured()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "当前为开发 mock，请用短信绑定手机号");
        }
        String body;
        try {
            body = restClient.post()
                    .uri(GET_PHONE_URL, stableToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("code", code))
                    .retrieve()
                    .body(String.class);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[wx-api] getuserphonenumber http error", e);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "微信服务暂不可用，请稍后重试");
        }
        Map<String, Object> result = JsonUtils.toMap(body);
        Object errcode = result.get("errcode");
        if (errcode instanceof Number n && n.intValue() != 0) {
            log.warn("[wx-api] getuserphonenumber failed: {}", body);
            throw new BizException(ErrorCode.PARAM_ERROR, "获取微信手机号失败：" + result.get("errmsg"));
        }
        if (result.get("phone_info") instanceof Map<?, ?> phoneInfo) {
            Object phone = phoneInfo.get("purePhoneNumber");
            if (phone == null) {
                phone = phoneInfo.get("phoneNumber");
            }
            if (phone != null && StringUtils.hasText(phone.toString())) {
                return phone.toString();
            }
        }
        log.warn("[wx-api] getuserphonenumber no phone_info: {}", body);
        throw new BizException(ErrorCode.PARAM_ERROR, "获取微信手机号失败");
    }
}
