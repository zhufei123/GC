package com.recycle.common.entity.trade;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细（item_type: ESTIMATE 预估 / ACTUAL 实收，均带价格快照；无 update_time/deleted）
 */
@Data
@TableName("order_item")
public class OrderItem implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;
    /** ESTIMATE/ACTUAL */
    private String itemType;
    private Long skuId;
    private String skuName;
    private String unit;
    private BigDecimal weight;
    private BigDecimal price;
    private BigDecimal amount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
