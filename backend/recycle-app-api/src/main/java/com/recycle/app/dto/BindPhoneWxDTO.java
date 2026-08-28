package com.recycle.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BindPhoneWxDTO {

    /** 小程序 getPhoneNumber 返回的 code */
    @NotBlank(message = "code 不能为空")
    private String code;
}
