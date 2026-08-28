package com.recycle.app.pay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 微信小程序登录 + 商家转账配置（app.wx）
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.wx")
public class WxPayProperties {

    private String appid = "";
    private String secret = "";
    private String mchId = "";
    private String apiV3Key = "";
    /** true：转账进程内直接成功，不发起外部 HTTP（默认） */
    private boolean mock = true;

    /** appid+secret 齐备才走真实 jscode2session */
    public boolean loginConfigured() {
        return StringUtils.hasText(appid) && StringUtils.hasText(secret);
    }

    /** 商户参数齐备才允许走真实转账 */
    public boolean mchConfigured() {
        return StringUtils.hasText(mchId) && StringUtils.hasText(apiV3Key);
    }
}
