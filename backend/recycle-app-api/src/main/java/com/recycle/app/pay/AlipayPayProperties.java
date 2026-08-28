package com.recycle.app.pay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付宝登录 + 转账 + 消息模板配置（app.alipay）
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.alipay")
public class AlipayPayProperties {

    private String appId = "";
    /** 应用私钥 */
    private String privateKey = "";
    /** 支付宝公钥（验签用） */
    private String alipayPublicKey = "";
    private String gateway = "https://openapi.alipay.com/gateway.do";
    /** 小程序 AES 解密密钥（手机号等加密数据） */
    private String encryptKey = "";
    /** templateKey -> 支付宝消息模板 id */
    private Map<String, String> messageTemplates = new LinkedHashMap<>();
    /** true：转账进程内直接成功，不发起外部 HTTP（默认） */
    private boolean mock = true;

    public boolean configured() {
        return StringUtils.hasText(appId);
    }

    /** appId+私钥齐备才走真实 alipay.system.oauth.token */
    public boolean oauthConfigured() {
        return StringUtils.hasText(appId) && StringUtils.hasText(privateKey);
    }

    /** 转账需要在 OAuth 基础上再配支付宝公钥验签 */
    public boolean transferConfigured() {
        return oauthConfigured() && StringUtils.hasText(alipayPublicKey);
    }
}
