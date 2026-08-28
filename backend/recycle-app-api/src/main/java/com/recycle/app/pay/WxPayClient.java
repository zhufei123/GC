package com.recycle.app.pay;

import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 微信商家转账客户端（占位实现，不伪造签名）。
 *
 * TODO 正式接入微信支付 V3「商家转账到零钱（用户确认收款）」：
 *  1. 配置商户号 mch-id、APIv3 密钥 api-v3-key、商户 API 证书序列号与私钥；
 *  2. POST https://api.mch.weixin.qq.com/v3/fund-app/mch-transfer/transfer-bills
 *     发起转账（out_bill_no=payoutNo, openid, transfer_amount 单位分），响应 state 为
 *     WAIT_USER_CONFIRM 时取 package_info 下发给小程序 requestMerchantTransfer 拉起确认收款；
 *  3. 通过回调 /app-api/pay/notify/wx 或主动查单同步 SUCCESS/FAIL 终态。
 *  可引入 com.github.wechatpay-apiv3:wechatpay-java SDK，或按 V3 规范自行实现签名。
 */
@Component
@RequiredArgsConstructor
public class WxPayClient {

    private final WxPayProperties props;

    /**
     * 发起商家转账。当前为占位：未实现真实签名与请求，直接抛错提示配置商户后接入。
     */
    public TransferResult transferToUser(String payoutNo, String openid, BigDecimal amount, String remark) {
        // 占位：绝不伪造签名请求微信；接入前请保持 app.wx.mock=true 走进程内 mock
        throw new BizException(ErrorCode.PARAM_ERROR,
                "未配置商户：微信商家转账尚未接入（mchId=" + props.getMchId() + "），请使用 mock 模式");
    }

    public record TransferResult(String status, String channelBillNo, String packageInfo) {
    }
}
