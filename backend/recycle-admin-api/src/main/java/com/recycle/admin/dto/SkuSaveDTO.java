package com.recycle.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuSaveDTO {

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotBlank(message = "名称不能为空")
    private String name;

    private String image;
    private String unit = "kg";
    private String description;
    private Integer sort = 0;
    private Integer status = 1;

    /** 新增时可带初始价（立即生效） */
    private BigDecimal price;
}
