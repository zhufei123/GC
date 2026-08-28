package com.recycle.common.entity.member;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包流水（wallet_ledger 无 update_time/deleted 列，不继承 BaseEntity；amount 正数入账、负数出账）
 */
@Data
@TableName("wallet_ledger")
public class WalletLedger implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private BigDecimal amount;
    /** 业务类型，如 ORDER（回收订单打款入账） */
    private String bizType;
    private Long bizId;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
