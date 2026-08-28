package com.recycle.common.entity.recycle;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recycle.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 两级分类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class Category extends BaseEntity {

    private Long parentId;
    private String name;
    private String icon;
    private Integer sort;
    private Integer status;
}
