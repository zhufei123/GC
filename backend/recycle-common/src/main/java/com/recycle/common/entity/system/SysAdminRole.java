package com.recycle.common.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理员-角色关联（复合主键，仅按条件读写）
 */
@Data
@TableName("sys_admin_role")
public class SysAdminRole implements Serializable {

    private Long adminId;
    private Long roleId;
}
