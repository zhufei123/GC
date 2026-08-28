package com.recycle.common.entity.member;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recycle.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * C/B 端用户（role: customer/recycler）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("`user`")
public class User extends BaseEntity {

    private String openidWx;
    private String unionidWx;
    private String openidAlipay;
    private String phone;
    private String nickname;
    private String avatar;
    /** customer/recycler */
    private String role;
    /** 1启用 0禁用 */
    private Integer status;
    /** 钱包预留 */
    private BigDecimal balance;
    /** none/pending/approved/rejected */
    private String recyclerStatus;
}
