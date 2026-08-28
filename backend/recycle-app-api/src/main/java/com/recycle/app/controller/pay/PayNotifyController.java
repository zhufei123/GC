package com.recycle.app.controller.pay;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 支付渠道异步回调（已在 SaTokenConfig 放行，正式接入必须验签后再处理）。
 * mock 模式下渠道转账进程内直接成功，不会有回调进来，此处仅记录日志占位。
 */
@Slf4j
@Tag(name = "App-支付回调")
@RestController
@RequestMapping("/app-api/pay/notify")
public class PayNotifyController {

    @Operation(summary = "微信商家转账回调（占位，接入后需 V3 验签+解密）")
    @PostMapping("/wx")
    public Map<String, String> wxNotify(@RequestBody(required = false) String body) {
        log.info("[pay-notify] wx transfer callback: {}", body);
        return Map.of("code", "SUCCESS", "message", "成功");
    }

    @Operation(summary = "支付宝转账回调（占位，接入后需 RSA 验签）")
    @PostMapping("/alipay")
    public String alipayNotify(@RequestBody(required = false) String body) {
        log.info("[pay-notify] alipay transfer callback: {}", body);
        return "success";
    }
}
