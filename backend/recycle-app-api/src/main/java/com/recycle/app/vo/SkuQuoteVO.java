package com.recycle.app.vo;

import lombok.Data;

import java.math.BigDecimal;

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
    private BigDecimal distanceKm;
    private String businessHours;
    private Integer businessStatus;
    private Boolean openNow;
}
