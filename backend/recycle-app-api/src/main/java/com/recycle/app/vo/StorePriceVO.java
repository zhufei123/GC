package com.recycle.app.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    /** 报价最后更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
