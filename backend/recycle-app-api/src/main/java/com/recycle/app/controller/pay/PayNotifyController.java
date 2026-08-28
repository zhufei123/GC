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

import java.util.Map;

/**
 * 支付渠道异步回调（已在 SaTokenConfig 放行）。
 * mock 模式接受无签名的模拟回调按 payoutNo/out_bill_no 落终态；
 * mock=false 时未验签一律 403 拒绝（真实验签接入前不处理任何回调）。
 */
@Slf4j
@Tag(name = "App-支付回调")
@RestController
@RequestMapping("/app-api/pay/notify")
@RequiredArgsConstructor
public class PayNotifyController {

    private final PayoutService payoutService;
    private final WxPayProperties wxProps;
    private final AlipayPayProperties alipayProps;

    @Operation(summary = "微信商家转账回调（mock 接受模拟回调；正式接入需 V3 验签+解密）")
    @PostMapping("/wx")
    public Map<String, String> wxNotify(@RequestBody(required = false) String body,
                                        HttpServletRequest request, HttpServletResponse response) {
        log.info("[pay-notify] wx transfer callback: {}", body);
        boolean signed = StringUtils.hasText(request.getHeader("Wechatpay-Signature"));
        if (!wxProps.isMock()) {
            // 真实模式：验签未接入前拒绝一切回调（X-Mock-Notify 等旁路头无效）
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            log.warn("[pay-notify] wx notify refused: mock=false, signature {} but verification not implemented",
                    signed ? "present" : "missing");
            return Map.of("code", "FAIL", "message", "未验签，拒绝处理");
        }
        boolean ok = applyFromBody(body);
        if (!ok) {
            return Map.of("code", "FAIL", "message", "打款单不存在");
        }
        return Map.of("code", "SUCCESS", "message", "成功");
    }

    @Operation(summary = "支付宝转账回调（mock 接受模拟回调；正式接入需 RSA 验签）")
    @PostMapping("/alipay")
    public String alipayNotify(@RequestBody(required = false) String body, HttpServletResponse response) {
        log.info("[pay-notify] alipay transfer callback: {}", body);
        if (!alipayProps.isMock()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            log.warn("[pay-notify] alipay notify refused: mock=false, RSA verification not implemented");
            return "fail";
        }
        boolean ok = applyFromBody(body);
        return ok ? "success" : "fail";
    }

    /** 从回调 JSON 提取 payoutNo/out_bill_no 与终态，落打款单 */
    private boolean applyFromBody(String body) {
        Map<String, Object> payload = JsonUtils.toMap(body);
        String payoutNo = firstText(payload, "payoutNo", "out_bill_no", "outBillNo", "out_biz_no");
        if (!StringUtils.hasText(payoutNo)) {
            log.info("[pay-notify] no payoutNo/out_bill_no in body, ignored");
            return true;
        }
        String state = firstText(payload, "state", "status");
        boolean success = state == null || "SUCCESS".equalsIgnoreCase(state);
        String billNo = firstText(payload, "transfer_bill_no", "channelBillNo", "order_id", "orderId");
        String failReason = firstText(payload, "fail_reason", "failReason");
        return payoutService.applyChannelResult(payoutNo, success, billNo, failReason);
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
