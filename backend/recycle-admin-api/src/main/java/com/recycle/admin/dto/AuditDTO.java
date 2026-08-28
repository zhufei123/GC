package com.recycle.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuditDTO {

    @NotNull(message = "pass 不能为空")
    private Boolean pass;

    private String remark;
}
