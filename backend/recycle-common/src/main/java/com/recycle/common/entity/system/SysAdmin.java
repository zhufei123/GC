package com.recycle.common.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.recycle.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台管理员
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_admin")
public class SysAdmin extends BaseEntity {

    private String username;

    @JsonIgnore
    private String password;

    private String nickname;
    private String phone;
    private String avatar;
    private Integer status;
    /** 1 超级管理员 */
    private Integer superAdmin;
}
