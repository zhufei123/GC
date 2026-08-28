package com.recycle.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.admin.vo.DashboardSummaryVO;
import com.recycle.common.entity.member.User;
import com.recycle.common.entity.store.RecycleStation;
import com.recycle.common.entity.store.StationApply;
import com.recycle.common.entity.trade.OrderItem;
import com.recycle.common.entity.trade.RecycleOrder;
import com.recycle.common.mapper.OrderItemMapper;
import com.recycle.common.mapper.RecycleOrderMapper;
import com.recycle.common.mapper.RecycleStationMapper;
import com.recycle.common.mapper.StationApplyMapper;
import com.recycle.common.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final RecycleOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final RecycleStationMapper stationMapper;
    private final StationApplyMapper applyMapper;

    public DashboardSummaryVO summary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        DashboardSummaryVO vo = new DashboardSummaryVO();
        vo.setTodayOrderCount(orderMapper.selectCount(new LambdaQueryWrapper<RecycleOrder>()
                .ge(RecycleOrder::getCreateTime, todayStart)));

        List<RecycleOrder> todayCompleted = orderMapper.selectList(new LambdaQueryWrapper<RecycleOrder>()
                .eq(RecycleOrder::getStatus, "COMPLETED")
                .ge(RecycleOrder::getCompletedAt, todayStart));
        vo.setTodayAmount(todayCompleted.stream()
                .map(RecycleOrder::getActualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal todayWeight = BigDecimal.ZERO;
        if (!todayCompleted.isEmpty()) {
            List<Long> orderIds = todayCompleted.stream().map(RecycleOrder::getId).toList();
            todayWeight = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                            .in(OrderItem::getOrderId, orderIds)
                            .eq(OrderItem::getItemType, "ACTUAL"))
                    .stream().map(OrderItem::getWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        vo.setTodayWeightKg(todayWeight);

        vo.setTotalUserCount(userMapper.selectCount(null));
        vo.setTotalStoreCount(stationMapper.selectCount(null));
        vo.setPendingApplyCount(applyMapper.selectCount(new LambdaQueryWrapper<StationApply>()
                .eq(StationApply::getAuditStatus, "pending")));
        return vo;
    }
}
