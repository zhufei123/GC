package com.recycle.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderCancelDTO {

    @NotBlank(message = "取消原因不能为空")
    private String reason;
}
