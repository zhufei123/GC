package com.recycle.app.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.recycle.app.pay.AlipayPayProperties;
import com.recycle.app.pay.AlipayTransferClient;
import com.recycle.app.pay.WxPayClient;
import com.recycle.app.pay.WxPayProperties;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.entity.member.User;
import com.recycle.common.entity.member.WalletLedger;
import com.recycle.common.entity.trade.PayoutOrder;
import com.recycle.common.entity.trade.RecycleOrder;
import com.recycle.common.mapper.PayoutOrderMapper;
import com.recycle.common.mapper.UserMapper;
import com.recycle.common.mapper.WalletLedgerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * C2B 打款：完成订单时按 payMethod 向客户付款并落 payout_order。
 * mock 模式（默认）下微信/支付宝转账进程内直接成功，不发起外部 HTTP。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutService {

    public static final String OFFLINE = "OFFLINE";
    public static final String WX_TRANSFER = "WX_TRANSFER";
    public static final String ALIPAY_TRANSFER = "ALIPAY_TRANSFER";
    public static final String WALLET = "WALLET";
    public static final Set<String> CHANNELS = Set.of(OFFLINE, WX_TRANSFER, ALIPAY_TRANSFER, WALLET);

    private final PayoutOrderMapper payoutOrderMapper;
    private final UserMapper userMapper;
    private final WalletLedgerMapper walletLedgerMapper;
    private final WxPayClient wxPayClient;
    private final AlipayTransferClient alipayTransferClient;
    private final WxPayProperties wxProps;
    private final AlipayPayProperties alipayProps;

    /** 按渠道打款并落打款单，返回终态（或中间态）payout */
    @Transactional
    public PayoutOrder payout(RecycleOrder order, String channel) {
        User user = userMapper.selectById(order.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        PayoutOrder payout = new PayoutOrder();
        payout.setPayoutNo(genPayoutNo());
        payout.setOrderId(order.getId());
        payout.setUserId(user.getId());
        payout.setStationId(order.getStationId());
        payout.setChannel(channel);
        payout.setAmount(order.getActualAmount());

        switch (channel) {
            case OFFLINE ->
                // 线下现金：站点当面付清，直接成功
                payout.setStatus("SUCCESS");
            case WALLET ->
                // 平台钱包：入账在打款单落库后执行（同一事务）
                payout.setStatus("SUCCESS");
            case WX_TRANSFER -> {
                if (!StringUtils.hasText(user.getOpenidWx())) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "用户未绑定微信");
                }
                payout.setOpenid(user.getOpenidWx());
                if (wxProps.isMock() || !wxProps.mchConfigured()) {
                    // mock：先 WAIT_USER_CONFIRM（对齐真实商家转账用户确认收款流程），随后自动 SUCCESS
                    payout.setStatus("WAIT_USER_CONFIRM");
                    payout.setPackageInfo("MOCK_PACKAGE_" + payout.getPayoutNo());
                } else {
                    WxPayClient.TransferResult result = wxPayClient.transferToUser(
                            payout.getPayoutNo(), user.getOpenidWx(), payout.getAmount(), "回收订单打款");
                    payout.setStatus(result.status());
                    payout.setChannelBillNo(result.channelBillNo());
                    payout.setPackageInfo(result.packageInfo());
                }
            }
            case ALIPAY_TRANSFER -> {
                if (!StringUtils.hasText(user.getOpenidAlipay())) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "用户未绑定支付宝");
                }
                payout.setOpenid(user.getOpenidAlipay());
                if (alipayProps.isMock() || !alipayProps.configured()) {
                    payout.setStatus("SUCCESS");
                    payout.setChannelBillNo("MOCKALI" + payout.getPayoutNo());
                } else {
                    String billNo = alipayTransferClient.transferToUser(
                            payout.getPayoutNo(), user.getOpenidAlipay(), payout.getAmount(), "回收订单打款");
                    payout.setStatus("SUCCESS");
                    payout.setChannelBillNo(billNo);
                }
            }
            default -> throw new BizException(ErrorCode.PARAM_ERROR, "不支持的打款方式");
        }

        try {
            payoutOrderMapper.insert(payout);
        } catch (DuplicateKeyException e) {
            // uk_payout_order(order_id, deleted)：一单一付
            throw new BizException(ErrorCode.ORDER_STATUS_ILLEGAL, "该订单已存在打款单");
        }

        // mock 微信转账：用户确认收款自动完成
        if (WX_TRANSFER.equals(channel) && "WAIT_USER_CONFIRM".equals(payout.getStatus())
                && (wxProps.isMock() || !wxProps.mchConfigured())) {
            payout.setStatus("SUCCESS");
            payout.setChannelBillNo("MOCKWX" + payout.getPayoutNo());
            payoutOrderMapper.updateById(payout);
        }

        if (WALLET.equals(channel)) {
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, user.getId())
                    .setSql("balance = balance + {0}", payout.getAmount()));
            WalletLedger ledger = new WalletLedger();
            ledger.setUserId(user.getId());
            ledger.setAmount(payout.getAmount());
            ledger.setBizType("ORDER");
            ledger.setBizId(order.getId());
            ledger.setRemark("回收订单打款入账 " + order.getOrderNo());
            walletLedgerMapper.insert(ledger);
        }
        return payout;
    }

    private String genPayoutNo() {
        return "PO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }
}
