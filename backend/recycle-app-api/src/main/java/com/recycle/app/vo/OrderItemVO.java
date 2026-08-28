package com.recycle.app.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemVO {

    private Long skuId;
    private String skuName;
    private String unit;
    private BigDecimal weight;
    private BigDecimal price;
    private BigDecimal amount;

    public BigDecimal getEstimateWeight() {
        return weight;
    }
}
