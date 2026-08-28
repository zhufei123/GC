package com.recycle.admin.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 管理端查看某回收站的自主报价（对照平台指导价） */
@Data
public class AdminStorePriceVO {

    private Long skuId;
    private String skuName;
    private String unit;
    private String categoryName;
    /** 本站报价 */
    private BigDecimal price;
    /** 1报价中 0停报 */
    private Integer status;
    /** 平台指导价 */
    private BigDecimal guidePrice;
}
