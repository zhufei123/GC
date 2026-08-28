package com.recycle.app.vo;

import com.recycle.common.entity.member.WalletLedger;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class WalletVO {

    private BigDecimal balance;
    /** 最近流水 */
    private List<WalletLedger> list;
}
