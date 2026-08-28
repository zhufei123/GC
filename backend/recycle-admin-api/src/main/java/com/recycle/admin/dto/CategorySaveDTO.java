package com.recycle.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategorySaveDTO {

    private Long parentId = 0L;

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private String icon;
    private Integer sort = 0;
    private Integer status = 1;
}
