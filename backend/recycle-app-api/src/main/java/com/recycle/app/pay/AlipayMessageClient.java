package com.recycle.app.pay;

import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 支付宝小程序消息客户端（占位实现，不伪造签名）。
 *
 * 正式接入 alipay.open.app.mini.templatemessage.send：
 *  网关公共参数 app_id；业务参数 to_user_id 或 to_open_id（新商户推荐 to_open_id）、
 *  user_template_id、page、data。
 *  https://opendocs.alipay.com/mini/6430ce5a_alipay.open.app.mini.templatemessage.send
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayMessageClient {

    private final AlipayPayProperties props;

    public void send(String toUserId, String toOpenId, String templateId, String page, Map<String, Object> data) {
        if (!props.oauthConfigured()) {
            log.info("[alipay-message] not configured, skip send toUserId={} toOpenId={} templateId={}",
                    toUserId, toOpenId, templateId);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "支付宝消息未配置 appId+私钥");
        }
        if (!StringUtils.hasText(toUserId) && !StringUtils.hasText(toOpenId)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "支付宝消息缺少 to_user_id/to_open_id");
        }
        throw new BizException(ErrorCode.SYSTEM_ERROR,
                "支付宝消息 alipay.open.app.mini.templatemessage.send 尚未接入"
                        + "（appId=" + props.getAppId()
                        + ", page=" + page
                        + ", to_open_id=" + (StringUtils.hasText(toOpenId) ? "set" : "empty")
                        + ", to_user_id=" + (StringUtils.hasText(toUserId) ? "set" : "empty") + "）");
    }
}
