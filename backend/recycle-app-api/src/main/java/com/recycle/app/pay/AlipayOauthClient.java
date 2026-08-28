package com.recycle.app.pay;

import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 支付宝 OAuth：alipay.system.oauth.token 以 authCode 换 user_id（RSA2 自签，不依赖 SDK）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayOauthClient {

    private final AlipayPayProperties props;
    private final RestClient restClient = RestClient.create();

    /** authCode 换支付宝 user_id（作为 openid_alipay 存储） */
    public String oauthToken(String authCode) {
        if (!props.oauthConfigured()) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "支付宝 OAuth 未配置 appId+私钥");
        }
        Map<String, String> params = new TreeMap<>();
        params.put("app_id", props.getAppId());
        params.put("method", "alipay.system.oauth.token");
        params.put("format", "JSON");
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("version", "1.0");
        params.put("grant_type", "authorization_code");
        params.put("code", authCode);
        params.put("sign", sign(params));

        String body;
        try {
            String form = params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
            body = restClient.post()
                    .uri(props.getGateway())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.warn("[alipay-oauth] http error", e);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "支付宝登录服务暂不可用，请稍后重试");
        }
        Map<String, Object> result = JsonUtils.toMap(body);
        if (result.get("alipay_system_oauth_token_response") instanceof Map<?, ?> response) {
            Object userId = response.get("user_id");
            if (userId != null && StringUtils.hasText(userId.toString())) {
                return userId.toString();
            }
        }
        log.warn("[alipay-oauth] oauth.token failed: {}", body);
        throw new BizException(ErrorCode.PARAM_ERROR, "支付宝登录失败：authCode 无效");
    }

    /** RSA2（SHA256withRSA）对排序后的 k=v&... 明文签名 */
    private String sign(Map<String, String> sortedParams) {
        String content = sortedParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        try {
            String pem = props.getPrivateKey()
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            PrivateKey key = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new BizException(ErrorCode.SIGN_FAILED, "支付宝签名失败，请检查应用私钥配置");
        }
    }
}
