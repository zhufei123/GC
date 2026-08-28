package com.recycle.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.recycle.common.mapper.RecycleOrderMapper;
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
 * 先落库（uk_payout_order 锁单）再调渠道；mock 模式（默认）不发起外部 HTTP：
 * 微信转账停在 WAIT_USER_CONFIRM 等 C 端确认收款，支付宝（无用户确认环节）直接成功。
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
    private final RecycleOrderMapper recycleOrderMapper;
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
        payout.setPayoutNo(genPayoutNo(order.getId()));
        payout.setOrderId(order.getId());
        payout.setUserId(user.getId());
        payout.setStationId(order.getStationId());
        payout.setChannel(channel);
        payout.setAmount(order.getActualAmount());

        boolean wxMock = false;
        boolean callWxChannel = false;
        boolean callAlipayChannel = false;
        switch (channel) {
            case OFFLINE ->
                // 线下现金：站点当面付清，直接成功
                payout.setStatus("SUCCESS");
            case WALLET ->
                // 平台钱包：入账在打款单落库后执行（同一事务）
                payout.setStatus("SUCCESS");
            case WX_TRANSFER -> {
                String wxOpenid = user.getOpenidWx();
                if (!StringUtils.hasText(wxOpenid)) {
                    if (wxProps.isMock()) {
                        wxOpenid = "mock-wx-" + user.getId();
                    } else {
                        throw new BizException(ErrorCode.PARAM_ERROR, "用户未绑定微信");
                    }
                }
                payout.setOpenid(wxOpenid);
                if (wxProps.isMock()) {
                    // mock：停在 WAIT_USER_CONFIRM，等 C 端确认收款（H5 走 /pay/wx-confirm）
                    wxMock = true;
                    payout.setStatus("WAIT_USER_CONFIRM");
                    payout.setPackageInfo("MOCK_PACKAGE_" + payout.getPayoutNo());
                } else {
                    if (!wxProps.mchConfigured()) {
                        throw new BizException(ErrorCode.PARAM_ERROR, "未配置商户号，无法线上打款");
                    }
                    payout.setStatus("PROCESSING");
                    callWxChannel = true;
                }
            }
            case ALIPAY_TRANSFER -> {
                String aliOpenid = user.getOpenidAlipay();
                if (!StringUtils.hasText(aliOpenid)) {
                    if (alipayProps.isMock()) {
                        aliOpenid = "mock-alipay-" + user.getId();
                    } else {
                        throw new BizException(ErrorCode.PARAM_ERROR, "用户未绑定支付宝");
                    }
                }
                payout.setOpenid(aliOpenid);
                if (alipayProps.isMock()) {
                    // 支付宝无用户确认环节，mock 直接成功
                    payout.setStatus("SUCCESS");
                    payout.setChannelBillNo("MOCKALI" + payout.getPayoutNo());
                } else {
                    if (!alipayProps.transferConfigured()) {
                        throw new BizException(ErrorCode.PARAM_ERROR, "未配置商户号，无法线上打款");
                    }
                    payout.setStatus("PROCESSING");
                    callAlipayChannel = true;
                }
            }
            default -> throw new BizException(ErrorCode.PARAM_ERROR, "不支持的打款方式");
        }

        // 先落库：uk_payout_order(order_id, deleted) 锁单，再调渠道
        try {
            payoutOrderMapper.insert(payout);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.ORDER_STATUS_ILLEGAL, "该订单已存在打款单");
        }

        if (callWxChannel) {
            WxPayClient.TransferResult result = wxPayClient.transferToUser(
                    payout.getPayoutNo(), payout.getOpenid(), payout.getAmount(), "回收订单打款");
            payout.setStatus(result.status());
            payout.setChannelBillNo(result.channelBillNo());
            payout.setPackageInfo(result.packageInfo());
            payoutOrderMapper.updateById(payout);
        }
        if (callAlipayChannel) {
            String billNo = alipayTransferClient.transferToUser(
                    payout.getPayoutNo(), payout.getOpenid(), payout.getAmount(), "回收订单打款");
            payout.setStatus("SUCCESS");
            payout.setChannelBillNo(billNo);
            payoutOrderMapper.updateById(payout);
        }
        if (wxMock) {
            log.info("[payout] mock wx transfer WAIT_USER_CONFIRM payoutNo={} orderId={}",
                    payout.getPayoutNo(), order.getId());
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

    /**
     * C 端确认收款（H5/mock 对标小程序 wx.requestMerchantTransfer 成功回调）：
     * WAIT_USER_CONFIRM → SUCCESS，并同步订单打款状态。
     */
    @Transactional
    public PayoutOrder confirmUserReceive(Long userId, Long orderId) {
        PayoutOrder payout = payoutOrderMapper.selectOne(new LambdaQueryWrapper<PayoutOrder>()
                .eq(PayoutOrder::getOrderId, orderId));
        if (payout == null || !payout.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, "打款单不存在");
        }
        if (!"WAIT_USER_CONFIRM".equals(payout.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ILLEGAL, "打款单当前状态不可确认收款");
        }
        payout.setStatus("SUCCESS");
        if (!StringUtils.hasText(payout.getChannelBillNo())) {
            payout.setChannelBillNo("MOCKWX" + payout.getPayoutNo());
        }
        payoutOrderMapper.updateById(payout);
        recycleOrderMapper.update(null, new LambdaUpdateWrapper<RecycleOrder>()
                .eq(RecycleOrder::getId, orderId)
                .set(RecycleOrder::getPayoutStatus, "SUCCESS")
                .set(RecycleOrder::getPaidAt, LocalDateTime.now()));
        return payout;
    }

    /** 渠道回调落终态：仅 WAIT_USER_CONFIRM/PROCESSING 可流转，其余状态忽略（幂等） */
    @Transactional
    public boolean applyChannelResult(String payoutNo, boolean success, String channelBillNo, String failReason) {
        PayoutOrder payout = payoutOrderMapper.selectOne(new LambdaQueryWrapper<PayoutOrder>()
                .eq(PayoutOrder::getPayoutNo, payoutNo));
        if (payout == null) {
            log.warn("[payout] notify for unknown payoutNo={}", payoutNo);
            return false;
        }
        if (!"WAIT_USER_CONFIRM".equals(payout.getStatus()) && !"PROCESSING".equals(payout.getStatus())) {
            log.info("[payout] notify ignored, payoutNo={} status={}", payoutNo, payout.getStatus());
            return true;
        }
        payout.setStatus(success ? "SUCCESS" : "FAILED");
        if (StringUtils.hasText(channelBillNo)) {
            payout.setChannelBillNo(channelBillNo);
        }
        if (!success && StringUtils.hasText(failReason)) {
            payout.setFailReason(failReason);
        }
        payoutOrderMapper.updateById(payout);
        recycleOrderMapper.update(null, new LambdaUpdateWrapper<RecycleOrder>()
                .eq(RecycleOrder::getId, payout.getOrderId())
                .set(RecycleOrder::getPayoutStatus, payout.getStatus())
                .set(success, RecycleOrder::getPaidAt, LocalDateTime.now()));
        return true;
    }

    /** PO + 时间戳 + 订单 id 后 4 位 + 随机 4 位，降低同秒碰撞概率 */
    private String genPayoutNo(Long orderId) {
        String idSuffix = String.format("%04d", orderId % 10_000);
        return "PO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + idSuffix
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));
    }
}
