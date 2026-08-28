package com.recycle.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminCancelDTO {

    @NotBlank(message = "取消原因不能为空")
    private String reason;
}
