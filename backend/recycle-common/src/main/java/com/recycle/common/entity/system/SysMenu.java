package com.recycle.common.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recycle.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单权限（type: DIR/MENU/BUTTON）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    private Long parentId;
    /** 路由 name */
    private String name;
    /** 显示名 */
    private String title;
    /** DIR/MENU/BUTTON */
    private String type;
    private String path;
    private String component;
    private String icon;
    private String perms;
    private Integer sort;
    private Integer visible;
}
