package com.recycle.common.entity.store;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recycle.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 回收站（门店）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recycle_station")
public class RecycleStation extends BaseEntity {

    private Long ownerUserId;
    private String name;
    /** JSON 数组字符串 */
    private String photos;
    private String phone;
    private String contactName;
    private String province;
    private String city;
    private String district;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String businessHours;
    /** 1营业 0休息 */
    private Integer businessStatus;
    /** pending/approved/rejected */
    private String auditStatus;
    /** 账号启停 1/0 */
    private Integer status;
    /** JSON 数组字符串 */
    private String categoryIds;
}
