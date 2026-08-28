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
    public void alertStaleWaitConfirm() {
        List<PayoutOrder> stale = payoutOrderMapper.selectList(new LambdaQueryWrapper<PayoutOrder>()
                .eq(PayoutOrder::getStatus, "WAIT_USER_CONFIRM")
                .lt(PayoutOrder::getCreateTime, LocalDateTime.now().minusHours(24))
                .last("LIMIT 100"));
        for (PayoutOrder payout : stale) {
            log.warn("[payout-reconcile] WAIT_USER_CONFIRM over 24h payoutNo={} orderId={} amount={} createTime={}",
                    payout.getPayoutNo(), payout.getOrderId(), payout.getAmount(), payout.getCreateTime());
        }
    }
}
