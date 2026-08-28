package com.recycle.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CosSignDTO {

    /** 白名单 avatar/order/weigh/apply/banner/sku/icon/notice */
    @NotBlank(message = "scene 不能为空")
    private String scene;

    /** jpg/jpeg/png/webp */
    @NotBlank(message = "ext 不能为空")
    private String ext;

    /** 最多 9 */
    private Integer count = 1;
}
