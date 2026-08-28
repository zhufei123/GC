package com.recycle.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusDTO {

    @NotNull(message = "status 不能为空")
    private Integer status;
}
