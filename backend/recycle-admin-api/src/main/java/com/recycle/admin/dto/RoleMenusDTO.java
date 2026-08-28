package com.recycle.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RoleMenusDTO {

    @NotNull(message = "menuIds 不能为空")
    private List<Long> menuIds;
}
