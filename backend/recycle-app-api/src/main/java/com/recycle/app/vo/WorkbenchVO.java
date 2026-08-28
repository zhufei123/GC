package com.recycle.app.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkbenchVO {

    private Long storeId;
    private String storeName;
    private Integer businessStatus;
    private String auditStatus;
    private long pendingPoolCount;
    private long todayAcceptedCount;
    private long todayCompletedCount;
    private BigDecimal todayAmount;
    private long servingCount;
}
