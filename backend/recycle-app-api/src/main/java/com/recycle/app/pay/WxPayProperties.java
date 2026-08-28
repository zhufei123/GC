package com.recycle.app.pay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信小程序登录 + 商家转账 + 订阅消息配置（app.wx）
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.wx")
public class WxPayProperties {

    private String appid = "";
    private String secret = "";
    private String mchId = "";
    private String apiV3Key = "";
    /** 商户 API 证书序列号（V3 请求签名必需） */
    private String mchSerialNo = "";
    /** 商户 API 私钥 PEM 内容 */
    private String privateKeyPem = "";
    /** 商家转账回调地址 */
    private String notifyUrl = "";
    /** templateKey(如 ORDER_ACCEPTED/ORDER_WEIGHED/ORDER_COMPLETED) -> 订阅消息模板 id */
    private Map<String, String> subscribeTemplates = new LinkedHashMap<>();
    /** true：转账进程内直接成功，不发起外部 HTTP（默认） */
    private boolean mock = true;

    /** appid+secret 齐备才走真实 jscode2session */
    public boolean loginConfigured() {
        return StringUtils.hasText(appid) && StringUtils.hasText(secret);
    }

    /** 商户参数齐备才允许走真实转账：V3 签名需要 mchId+apiV3Key+证书序列号 */
    public boolean mchConfigured() {
        return StringUtils.hasText(mchId) && StringUtils.hasText(apiV3Key) && StringUtils.hasText(mchSerialNo);
    }
}
