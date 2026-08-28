package com.recycle.common.entity.trade;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.recycle.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 回收订单（状态：PENDING/ACCEPTED/SERVING/WEIGHED/COMPLETED/CANCELLED）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recycle_order")
public class RecycleOrder extends BaseEntity {

    private String orderNo;
    private Long userId;
    private Long stationId;
    /** PICKUP/DROPOFF */
    private String type;
    private String status;
    private String receiver;
    private String phone;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate appointDate;

    private String appointPeriod;
    private BigDecimal estimateAmount;
    private BigDecimal actualAmount;
    /** JSON 数组字符串 */
    private String photosCustomer;
    /** JSON 数组字符串 */
    private String photosWeigh;
    private String remark;
    /** customer/admin */
    private String cancelBy;
    private String cancelReason;
    private String requestId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime acceptedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime servedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime weighedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime completedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime cancelledAt;
}
