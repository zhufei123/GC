package com.recycle.admin.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardSummaryVO {

    private Long todayOrderCount;
    private BigDecimal todayWeightKg;
    private BigDecimal todayAmount;
    private Long totalUserCount;
    private Long totalStoreCount;
    private Long pendingApplyCount;
}
