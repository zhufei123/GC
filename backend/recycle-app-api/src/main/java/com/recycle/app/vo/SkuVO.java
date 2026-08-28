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

    /** 指导价别名（与 price 相同，语义更明确） */
    public BigDecimal getGuidePrice() {
        return price;
    }
}
