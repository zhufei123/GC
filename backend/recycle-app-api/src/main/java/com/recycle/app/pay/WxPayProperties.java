package com.recycle.app.pay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信小程序登录 + 商家转账 + 订阅消息配置（app.wx）。
 * 官方文档：
 * - code2session 必填 appid+secret+js_code：
 *   https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-login/code2Session.html
 * - 订阅消息 send 必填 touser/template_id/data/miniprogram_state/lang：
 *   https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/mp-message-management/subscribe-message/sendMessage.html
 * - 商家转账 transfer-bills 必填 appid+openid+transfer_scene_id+out_bill_no+transfer_amount：
 *   https://pay.weixin.qq.com/doc/v3/merchant/4012716434
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
    /**
     * 订阅消息跳转页，官方 page 字段；默认订单详情。
     * 真机页路径须与小程序分包一致：pages-customer/order/detail
     */
    private String subscribePage = "pages-customer/order/detail";
    /** 按模板覆盖跳转页 */
    private Map<String, String> subscribePages = new LinkedHashMap<>();
    /** 订阅消息 miniprogram_state：developer / trial / formal */
    private String miniprogramState = "formal";
    /** 订阅消息 lang，官方必填，默认 zh_CN */
    private String lang = "zh_CN";
    /**
     * 商家转账 transfer_scene_id（微信商户平台报备的转账场景 ID），真实打款必填。
     * https://pay.weixin.qq.com/doc/v3/merchant/4012716434
     */
    private String transferSceneId = "";
    /** 用户收款感知文案（user_recv_perception），如「回收货款」 */
    private String userRecvPerception = "回收货款";
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

    public String resolveSubscribePage(String templateKey) {
        if (subscribePages != null && StringUtils.hasText(templateKey)) {
            String page = subscribePages.get(templateKey);
            if (StringUtils.hasText(page)) {
                return page;
            }
        }
        return StringUtils.hasText(subscribePage) ? subscribePage : "pages-customer/order/detail";
    }
}
