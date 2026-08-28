package com.recycle.app.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkbenchVO {

    private Long storeId;
    private String storeName;
    private Integer businessStatus;
    private String auditStatus;
    private Long pendingPoolCount;
    private Long todayAcceptedCount;
    private Long todayCompletedCount;
    private BigDecimal todayAmount;
    private Long servingCount;
}
