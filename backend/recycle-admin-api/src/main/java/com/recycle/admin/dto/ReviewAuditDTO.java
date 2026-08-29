package com.recycle.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewAuditDTO {

    /** APPROVED / REJECTED */
    @NotBlank(message = "审核结果不能为空")
    private String status;

    private String remark;
}
