package com.recycle.common.entity.recycle;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 调价记录（无 update_time/deleted）
 */
@Data
@TableName("sku_price_log")
public class SkuPriceLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long skuId;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime effectiveAt;

    private String reason;
    private Long operatorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
