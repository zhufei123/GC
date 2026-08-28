package com.recycle.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddressDTO {

    @NotBlank(message = "联系人不能为空")
    private String receiver;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "省份不能为空")
    private String province;

    @NotBlank(message = "城市不能为空")
    private String city;

    @NotBlank(message = "区县不能为空")
    private String district;

    private String street;

    @NotBlank(message = "详细地址不能为空")
    private String detail;

    private BigDecimal longitude;
    private BigDecimal latitude;
    private Boolean isDefault;
}
