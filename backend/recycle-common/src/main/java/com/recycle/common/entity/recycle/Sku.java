package com.recycle.common.entity.recycle;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recycle.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 回收 SKU
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sku")
public class Sku extends BaseEntity {

    private Long categoryId;
    private String name;
    private String image;
    /** kg/piece */
    private String unit;
    private String description;
    private Integer sort;
    private Integer status;
}
