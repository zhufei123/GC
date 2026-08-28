package com.recycle.app.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 某 SKU 的附近门店报价
 */
@Data
public class SkuQuoteVO {

    private Long stationId;
    private String stationName;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private BigDecimal price;
    private String unit;
    private BigDecimal distanceKm;
    private String businessHours;
    private Integer businessStatus;
    private Boolean openNow;
    /** 报价最后更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
