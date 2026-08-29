package com.recycle.app.pay;

import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 微信商家转账客户端（占位实现，不伪造签名）。
 *
 * TODO 正式接入微信支付 V3「商家转账到零钱（用户确认收款）」：
 *  POST https://api.mch.weixin.qq.com/v3/fund-app/mch-transfer/transfer-bills
 *  官方必填：appid、openid、transfer_scene_id、out_bill_no、transfer_amount（分）、transfer_remark。
 *  appid 必须与颁发 openid 的小程序一致。
 *  https://pay.weixin.qq.com/doc/v3/merchant/4012716434
 */
@Component
@RequiredArgsConstructor
public class WxPayClient {

    private final WxPayProperties props;

    /**
     * 发起商家转账。当前为占位：未实现真实签名与请求，直接抛错提示配置商户后接入。
     */
    public TransferResult transferToUser(String payoutNo, String appid, String openid,
                                         String transferSceneId, String notifyUrl,
                                         BigDecimal amount, String remark) {
        // 占位：绝不伪造签名请求微信；接入前请保持 app.wx.mock=true 走进程内 mock
        throw new BizException(ErrorCode.PARAM_ERROR,
                "微信商家转账 transfer-bills 尚未接入（mchId=" + props.getMchId()
                        + ", appid=" + blankToDash(appid)
                        + ", sceneId=" + blankToDash(transferSceneId)
                        + ", notifyUrl=" + blankToDash(notifyUrl)
                        + ", openid=" + (StringUtils.hasText(openid) ? "set" : "empty")
                        + ", out_bill_no=" + payoutNo
                        + ", amount=" + amount
                        + ", remark=" + remark
                        + "），请使用 mock 模式");
    }

    /**
     * 查询转账单状态。正式接入应 GET
     * https://api.mch.weixin.qq.com/v3/fund-app/mch-transfer/transfer-bills/out-bill-no/{payoutNo}
     */
    public TransferResult queryTransferBill(String payoutNo) {
        throw new BizException(ErrorCode.PARAM_ERROR,
                "微信商家转账查单 transfer-bills/out-bill-no 尚未接入，请使用 mock 模式");
    }

    private static String blankToDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    public record TransferResult(String status, String channelBillNo, String packageInfo) {
    }
}
