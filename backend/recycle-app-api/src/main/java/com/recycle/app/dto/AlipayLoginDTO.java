package com.recycle.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AlipayLoginDTO {

    /** 已配置 oauth 时走 alipay.system.oauth.token；未配置 mock 模式下 authCode 即 openidAlipay */
    @NotBlank(message = "authCode 不能为空")
    private String authCode;

    /** user | boss，默认 user */
    private String client = "user";
}
