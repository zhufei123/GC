package com.recycle.app.controller.pay;

import com.recycle.app.pay.AlipayPayProperties;
import com.recycle.app.pay.WxPayProperties;
import com.recycle.app.service.PayoutService;
import com.recycle.common.util.JsonUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 支付渠道异步回调（已在 SaTokenConfig 放行）。
 * mock 模式接受无签名的模拟回调按 payoutNo/out_bill_no 落终态；
 * mock=false 时未验签一律 403 拒绝（真实验签接入前不处理任何回调）。
 *
 * 报文兼容（mock 也按真实报文形状解析）：
 * - 微信 V3：顶层信封 {id,event_type,resource:{...}}，resource 为明文 JSON 时直接取
 *   out_bill_no/state/transfer_bill_no/fail_reason；resource 仅有 ciphertext（真实加密报文）
 *   在 mock 下无法解密，仅记录不落状态。
 * - 支付宝：application/x-www-form-urlencoded（out_biz_no=..&status=SUCCESS&order_id=..），
 *   兼容 JSON 与表单两种格式。
 * - 简化 mock：{"payoutNo":"PO..","state":"SUCCESS"}。
 */
@Slf4j
@Tag(name = "App-支付回调")
@RestController
@RequestMapping("/app-api/pay/notify")
@RequiredArgsConstructor
public class PayNotifyController {

    /** 渠道终态失败字面量（微信 FAIL/CANCELLED，支付宝 FAIL/REFUND，通用 FAILED/CLOSED） */
    private static final Set<String> FAIL_STATES =
            Set.of("FAIL", "FAILED", "CANCELLED", "CANCELED", "CLOSED", "REFUND");

    private final PayoutService payoutService;
    private final WxPayProperties wxProps;
    private final AlipayPayProperties alipayProps;

    @Operation(summary = "微信商家转账回调（mock 接受模拟回调；正式接入需 V3 验签+解密）")
    @PostMapping("/wx")
    public Map<String, String> wxNotify(@RequestBody(required = false) String body,
                                        HttpServletRequest request, HttpServletResponse response) {
        boolean signed = StringUtils.hasText(request.getHeader("Wechatpay-Signature"));
        if (!wxProps.isMock()) {
            // 真实模式：验签未接入前拒绝一切回调（X-Mock-Notify 等旁路头无效）
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            log.warn("[pay-notify] wx notify refused: mock=false, signature {} but verification not implemented",
                    signed ? "present" : "missing");
            return Map.of("code", "FAIL", "message", "未验签，拒绝处理");
        }
        NotifyResult result = parseNotify(body, request);
        log.info("[pay-notify] wx transfer callback payoutNo={} state={}", result.payoutNo(), result.state());
        if (result.payoutNo() == null || result.terminalSuccess() == null) {
            // 无单号或非终态（ACCEPTED/PROCESSING/WAIT_USER_CONFIRM/TRANSFERING 等）：应答成功不落状态
            return Map.of("code", "SUCCESS", "message", "成功");
        }
        boolean ok = payoutService.applyChannelResult(
                result.payoutNo(), result.terminalSuccess(), result.billNo(), result.failReason());
        if (!ok) {
            // 打款单不存在（可能业务事务未提交）：回 5xx 让渠道按重试策略再通知
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return Map.of("code", "FAIL", "message", "打款单不存在");
        }
        return Map.of("code", "SUCCESS", "message", "成功");
    }

    @Operation(summary = "支付宝转账回调（mock 接受模拟回调；正式接入需 RSA 验签）")
    @PostMapping("/alipay")
    public String alipayNotify(@RequestBody(required = false) String body,
                               HttpServletRequest request, HttpServletResponse response) {
        if (!alipayProps.isMock()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            log.warn("[pay-notify] alipay notify refused: mock=false, RSA verification not implemented");
            return "fail";
        }
        NotifyResult result = parseNotify(body, request);
        log.info("[pay-notify] alipay transfer callback payoutNo={} state={}", result.payoutNo(), result.state());
        if (result.payoutNo() == null || result.terminalSuccess() == null) {
            // 无单号或非终态（DEALING 等）：应答成功等终态通知
            return "success";
        }
        boolean ok = payoutService.applyChannelResult(
                result.payoutNo(), result.terminalSuccess(), result.billNo(), result.failReason());
        return ok ? "success" : "fail";
    }

    /** payoutNo/state/billNo/failReason；terminalSuccess：true 成功 false 失败 null 非终态不落状态 */
    private record NotifyResult(String payoutNo, String state, Boolean terminalSuccess,
                                String billNo, String failReason) {
    }

    /** 兼容 JSON / form-urlencoded / 微信 V3 信封（resource 明文）三种报文形状 */
    private NotifyResult parseNotify(String body, HttpServletRequest request) {
        // 回调体含 openid 等敏感字段，完整报文仅 debug 级输出
        log.debug("[pay-notify] raw callback body: {}", body);
        Map<String, Object> payload = extractPayload(body, request);
        String payoutNo = firstText(payload,
                "payoutNo", "out_bill_no", "outBillNo", "out_biz_no", "outBizNo");
        String state = firstText(payload, "state", "status");
        Boolean terminalSuccess = null;
        if (state != null) {
            String upper = state.toUpperCase(Locale.ROOT);
            if ("SUCCESS".equals(upper)) {
                terminalSuccess = Boolean.TRUE;
            } else if (FAIL_STATES.contains(upper)) {
                terminalSuccess = Boolean.FALSE;
            }
        }
        String billNo = firstText(payload,
                "transfer_bill_no", "transferBillNo", "channelBillNo", "order_id", "orderId");
        String failReason = firstText(payload, "fail_reason", "failReason", "sub_msg", "subMsg");
        return new NotifyResult(payoutNo, state, terminalSuccess, billNo, failReason);
    }

    private Map<String, Object> extractPayload(String body, HttpServletRequest request) {
        Map<String, Object> parsed = JsonUtils.toMap(body);
        Map<String, Object> payload = parsed == null ? new HashMap<>() : new HashMap<>(parsed);
        if (payload.isEmpty() && StringUtils.hasText(body) && body.contains("=")) {
            // 支付宝真实回调为 form-urlencoded
            payload.putAll(parseFormUrlEncoded(body));
        }
        if (payload.isEmpty()) {
            // 容器已按表单解析掉请求体时从 parameter 兜底
            request.getParameterMap().forEach((k, v) -> {
                if (v != null && v.length > 0) {
                    payload.put(k, v[0]);
                }
            });
        }
        // 微信 V3 信封：业务字段在 resource 内；明文 JSON 直接展开（mock），密文无法解密仅记录
        Object resource = payload.get("resource");
        if (resource instanceof Map<?, ?> res) {
            if (res.containsKey("ciphertext") && !res.containsKey("out_bill_no")) {
                log.warn("[pay-notify] encrypted wx resource received but mock mode cannot decrypt, ignored");
            } else {
                res.forEach((k, v) -> {
                    if (k != null && v != null) {
                        payload.put(k.toString(), v);
                    }
                });
            }
        }
        return payload;
    }

    private Map<String, Object> parseFormUrlEncoded(String body) {
        Map<String, Object> result = new HashMap<>();
        for (String pair : body.split("&")) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            result.put(key, value);
        }
        return result;
    }

    private String firstText(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null && StringUtils.hasText(value.toString())) {
                return value.toString();
            }
        }
        return null;
    }
}
