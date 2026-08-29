package com.recycle.app.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RegionCityVO {

    private String name;
    private BigDecimal longitude;
    private BigDecimal latitude;
}
