package com.recycle.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AlipayLoginDTO {

    /** mock：authCode 即 openidAlipay */
    @NotBlank(message = "authCode 不能为空")
    private String authCode;
}
