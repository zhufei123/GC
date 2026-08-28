package com.recycle.common.entity.recycle;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.recycle.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SKU 指导价（effective_at <= now 的最新一条为当前价）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sku_price")
public class SkuPrice extends BaseEntity {

    private Long skuId;
    private String cityCode;
    private BigDecimal price;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime effectiveAt;

    private Integer status;
}
