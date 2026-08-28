package com.recycle.app.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 门店报价单条目
 */
@Data
public class StorePriceVO {

    private Long skuId;
    private String skuName;
    private String unit;
    private String categoryName;
    /** 门店报价 */
    private BigDecimal price;
    /** 1报价中 0停报 */
    private Integer status;
    /** 平台指导价 */
    private BigDecimal guidePrice;
}
