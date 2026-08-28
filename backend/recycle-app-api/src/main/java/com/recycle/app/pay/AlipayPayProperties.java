package com.recycle.app.pay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 支付宝登录 + 转账配置（app.alipay）
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.alipay")
public class AlipayPayProperties {

    private String appId = "";
    /** true：转账进程内直接成功，不发起外部 HTTP（默认） */
    private boolean mock = true;

    public boolean configured() {
        return StringUtils.hasText(appId);
    }
}
