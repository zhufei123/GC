package com.recycle.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class AdminSaveDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String nickname;

    /** 新增必填；编辑忽略 */
    private String password;

    private String phone;
    private String avatar;
    private List<Long> roleIds;
    private Integer status = 1;
}
