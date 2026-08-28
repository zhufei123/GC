package com.recycle.common.entity.member;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recycle.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_address")
public class UserAddress extends BaseEntity {

    private Long userId;
    private String receiver;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String street;
    private String detail;
    private BigDecimal longitude;
    private BigDecimal latitude;
    /** 1默认 0否 */
    private Integer isDefault;
}
