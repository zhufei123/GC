package com.recycle.app.pay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付宝登录 + 转账 + 消息模板配置（app.alipay）。
 * 官方文档：
 * - oauth.token 网关公共参数 app_id，响应含 user_id 与 open_id：
 *   https://opendocs.alipay.com/mini/84bc7352_alipay.system.oauth.token
 * - 小程序模板消息 to_user_id 或 to_open_id（新商户推荐 open_id）：
 *   https://opendocs.alipay.com/mini/6430ce5a_alipay.open.app.mini.templatemessage.send
 * - 单笔转账 identity + identity_type（ALIPAY_OPEN_ID 优先）：
 *   https://opendocs.alipay.com/open/02byuo
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
    /** 网关签名算法，官方默认 RSA2 */
    private String signType = "RSA2";
    /** 网关 charset，官方默认 utf-8 */
    private String charset = "utf-8";
    /** templateKey -> 支付宝消息模板 id */
    private Map<String, String> messageTemplates = new LinkedHashMap<>();
    /** 模板消息跳转小程序页面 */
    private String messagePage = "pages-customer/order/detail";
    private Map<String, String> messagePages = new LinkedHashMap<>();
    /** alipay.fund.trans.uni.transfer 的 product_code */
    private String transferProductCode = "TRANS_ACCOUNT_NO_PWD";
    /** 优先 ALIPAY_OPEN_ID；无 open_id 时回退 ALIPAY_USER_ID */
    private String transferIdentityType = "ALIPAY_OPEN_ID";
    /** 转账异步通知地址 */
    private String notifyUrl = "";
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

    public String resolveMessagePage(String templateKey) {
        if (messagePages != null && StringUtils.hasText(templateKey)) {
            String page = messagePages.get(templateKey);
            if (StringUtils.hasText(page)) {
                return page;
            }
        }
        return StringUtils.hasText(messagePage) ? messagePage : "pages-customer/order/detail";
    }
}
