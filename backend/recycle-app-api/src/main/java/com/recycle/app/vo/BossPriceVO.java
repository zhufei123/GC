package com.recycle.app.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * B 端报价管理行：全部上架 SKU + 指导价 + 本店报价
 */
@Data
public class BossPriceVO {

    private Long skuId;
    private String skuName;
    private String unit;
    private String categoryName;
    /** 平台指导价 */
    private BigDecimal guidePrice;
    /** 本店报价，null 表示未报价 */
    private BigDecimal stationPrice;
    /** 1报价中 0停报，null 表示未报价 */
    private Integer status;
    private String remark;

    /** 前端字段别名：price = 本店报价 */
    public BigDecimal getPrice() {
        return stationPrice;
    }
}
