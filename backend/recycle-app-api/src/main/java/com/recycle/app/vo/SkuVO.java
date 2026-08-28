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
    /** 平台指导价（今日生效），null 表示暂无指导价 */
    private BigDecimal price;

    /** 最近一次调价方向：UP/DOWN/FLAT，无调价记录为 null */
    private String trend;

    /** 最近一次调价差额（newPrice - oldPrice），无调价记录为 null */
    private BigDecimal priceDiff;

    /** 指导价别名（与 price 相同，语义更明确） */
    public BigDecimal getGuidePrice() {
        return price;
    }
}
