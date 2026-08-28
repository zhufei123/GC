package com.recycle.common.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色-菜单关联（复合主键，仅按条件读写）
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu implements Serializable {

    private Long roleId;
    private Long menuId;
}
