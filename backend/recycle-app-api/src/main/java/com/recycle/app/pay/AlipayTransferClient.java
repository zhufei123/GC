package com.recycle.app.pay;

import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 支付宝单笔转账客户端（占位实现，不伪造签名）。
 *
 * TODO 正式接入支付宝「alipay.fund.trans.uni.transfer」单笔转账到支付宝账户：
 *  1. 配置 app-id、应用私钥 private-key、支付宝公钥 alipay-public-key；
 *  2. biz_content: out_biz_no=payoutNo, trans_amount, product_code=TRANS_ACCOUNT_NO_PWD,
 *     payee_info.identity + identity_type（优先 ALIPAY_OPEN_ID，回退 ALIPAY_USER_ID）；
 *  3. 同步响应 status=SUCCESS 即打款成功，order_id 记入 channel_bill_no。
 *  https://opendocs.alipay.com/open/02byuo
 */
@Component
@RequiredArgsConstructor
public class AlipayTransferClient {

    private final AlipayPayProperties props;

    /**
     * 发起支付宝转账。当前为占位：未实现真实签名与请求，直接抛错提示配置商户后接入。
     */
    public String transferToUser(String payoutNo, String identity, String identityType,
                                 String productCode, BigDecimal amount, String remark) {
        // 占位：绝不伪造签名请求支付宝；接入前请保持 app.alipay.mock=true 走进程内 mock
        throw new BizException(ErrorCode.PARAM_ERROR,
                "支付宝转账 alipay.fund.trans.uni.transfer 尚未接入（appId=" + props.getAppId()
                        + ", identityType=" + blankToDash(identityType)
                        + ", productCode=" + blankToDash(productCode)
                        + ", identity=" + (StringUtils.hasText(identity) ? "set" : "empty")
                        + ", out_biz_no=" + payoutNo
                        + ", amount=" + amount
                        + ", remark=" + remark
                        + "），请使用 mock 模式");
    }

    /**
     * 查询转账单状态。正式接入应调 alipay.fund.trans.common.query（out_biz_no=payoutNo）。
     */
    public String queryTransferBill(String payoutNo) {
        throw new BizException(ErrorCode.PARAM_ERROR,
                "支付宝转账查单 alipay.fund.trans.common.query 尚未接入，请使用 mock 模式");
    }

    private static String blankToDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }
}
