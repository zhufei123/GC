package com.recycle.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MenuSaveDTO {

    private Long parentId = 0L;

    @NotBlank(message = "路由 name 不能为空")
    private String name;

    @NotBlank(message = "显示名不能为空")
    private String title;

    /** DIR/MENU/BUTTON */
    @NotBlank(message = "类型不能为空")
    private String type;

    private String path;
    private String component;
    private String icon;
    private String perms;
    private Integer sort = 0;
    private Integer visible = 1;
}
