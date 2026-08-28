package com.recycle.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WxLoginDTO {

    /** 已配置 appid+secret 时走 jscode2session；未配置 mock 模式下 code 即 openid */
    @NotBlank(message = "code 不能为空")
    private String code;

    /** user | boss，默认 user */
    private String client = "user";
}
