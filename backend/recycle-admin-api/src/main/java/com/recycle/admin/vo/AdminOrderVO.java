package com.recycle.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminOrderVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long stationId;
    private String stationName;
    private String type;
    private String status;
    private String receiver;
    /** 列表脱敏 */
    private String phone;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate appointDate;

    private String appointPeriod;
    private BigDecimal estimateAmount;
    private BigDecimal actualAmount;
    private List<String> images;
    private List<String> weighImages;
    private String remark;
    private String cancelBy;
    private String cancelReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    public LocalDateTime getCreatedAt() {
        return createTime;
    }

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

    private List<ItemVO> estimateItems;
    private List<ItemVO> actualItems;
    private List<TimelineVO> timeline;

    @Data
    public static class ItemVO {
        private Long skuId;
        private String skuName;
        private String unit;
        private BigDecimal weight;
        private BigDecimal price;
        private BigDecimal amount;
    }

    @Data
    public static class TimelineVO {
        private String status;
        private String label;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime time;
    }
}
