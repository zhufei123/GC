package com.recycle.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.common.entity.trade.PayoutOrder;
import com.recycle.common.mapper.PayoutOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 打款对账巡检：仅告警不改状态——超时单应通过渠道查单/回调落终态，不自动置成功。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayoutReconcileJob {

    private final PayoutOrderMapper payoutOrderMapper;

    @Scheduled(fixedDelay = 3600_000, initialDelay = 60_000)
    public void alertStalePayout() {
        // WAIT_USER_CONFIRM 有效期长（微信确认收款 24h 过期），24h 未确认才告警
        alertStale("WAIT_USER_CONFIRM", LocalDateTime.now().minusHours(24));
        // PROCESSING 是渠道受理后的短暂中间态，1h 未落终态即视为卡单（回调丢失/渠道异常）
        alertStale("PROCESSING", LocalDateTime.now().minusHours(1));
    }

    private void alertStale(String status, LocalDateTime createdBefore) {
        List<PayoutOrder> stale = payoutOrderMapper.selectList(new LambdaQueryWrapper<PayoutOrder>()
                .eq(PayoutOrder::getStatus, status)
                .lt(PayoutOrder::getCreateTime, createdBefore)
                .last("LIMIT 100"));
        for (PayoutOrder payout : stale) {
            log.warn("[payout-reconcile] stale {} payoutNo={} orderId={} amount={} createTime={}",
                    status, payout.getPayoutNo(), payout.getOrderId(), payout.getAmount(), payout.getCreateTime());
        }
    }
}
