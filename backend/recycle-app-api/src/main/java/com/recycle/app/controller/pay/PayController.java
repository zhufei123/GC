package com.recycle.app.controller.pay;

import com.recycle.app.service.PayoutService;
import com.recycle.app.support.CurrentUser;
import com.recycle.common.core.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "App-支付")
@RestController
@RequestMapping("/app-api/pay")
@RequiredArgsConstructor
public class PayController {

    private final PayoutService payoutService;

    @Operation(summary = "C 端确认收款（H5/mock 对标 wx.requestMerchantTransfer 成功回调）")
    @PostMapping("/wx-confirm/{orderId}")
    public R<Void> wxConfirm(@PathVariable Long orderId) {
        payoutService.confirmUserReceive(CurrentUser.id(), orderId);
        return R.ok();
    }
}
