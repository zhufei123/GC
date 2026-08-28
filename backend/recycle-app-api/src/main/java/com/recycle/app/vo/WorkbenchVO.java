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
    /** 待上门（已接单未开始服务） */
    private long pendingCount;

    public long getPoolCount() {
        return pendingPoolCount;
    }

    public long getTodayOrderCount() {
        return todayAcceptedCount;
    }
}
