package com.recycle.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminCosSignDTO {

    @NotBlank(message = "scene 不能为空")
    private String scene;

    @NotBlank(message = "ext 不能为空")
    private String ext;

    private Integer count = 1;
}
