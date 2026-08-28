package com.recycle.app.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserStatsVO {

    private Long completedOrders;
    private BigDecimal totalWeightKg;
    private BigDecimal totalAmount;
    /** 按每回收 1kg 减碳 0.8kg 估算 */
    private BigDecimal co2SavedKg;
}
