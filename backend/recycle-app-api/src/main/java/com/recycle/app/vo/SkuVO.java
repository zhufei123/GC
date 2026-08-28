package com.recycle.app.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuVO {

    private Long id;
    private Long categoryId;
    private String name;
    private String image;
    private String unit;
    private String description;
    private Integer sort;
    /** 今日生效价，null 表示暂无报价 */
    private BigDecimal price;
}
