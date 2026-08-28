package com.recycle.app.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 门店城市（C 端城市选择器用），坐标取该城市 id 最小的门店
 */
@Data
public class StoreCityVO {

    private String city;
    private BigDecimal longitude;
    private BigDecimal latitude;
}
