package com.recycle.app.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StoreNearbyVO {

    private Long id;
    private String name;
    private String address;
    private String phone;
    private String businessHours;
    private Integer businessStatus;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private BigDecimal distanceKm;
    private List<Long> categoryIds;
    private List<String> photos;
}
