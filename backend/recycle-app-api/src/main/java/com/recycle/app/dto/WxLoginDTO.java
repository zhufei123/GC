package com.recycle.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WxLoginDTO {

    /** 骨架 mock：直接把 code 当 openid */
    @NotBlank(message = "code 不能为空")
    private String code;
}
