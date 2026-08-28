package com.recycle.app.pay;

import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 支付宝小程序消息客户端（占位实现，不伪造签名）。
 *
 * TODO 正式接入 alipay.open.app.mini.templatemessage.send：
 *  to_user_id=支付宝 user_id, user_template_id=消息模板 id（配在 app.alipay.message-templates），
 *  page + data（模板字段 keyword），RSA2 签名同 AlipayOauthClient。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayMessageClient {

    private final AlipayPayProperties props;

    public void send(String alipayUserId, String templateId, String page, Map<String, Object> data) {
        if (!props.oauthConfigured()) {
            log.info("[alipay-message] not configured, skip send userId={} templateId={}", alipayUserId, templateId);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "支付宝消息未配置 appId+私钥");
        }
        throw new BizException(ErrorCode.SYSTEM_ERROR,
                "支付宝消息 alipay.open.app.mini.templatemessage.send 尚未接入");
    }
}
