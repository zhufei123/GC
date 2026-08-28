package com.recycle.admin.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardSummaryVO {

    private long todayOrderCount;
    private BigDecimal todayWeightKg;
    private BigDecimal todayAmount;
    private long totalUserCount;
    private long totalStoreCount;
    private long pendingApplyCount;
}
