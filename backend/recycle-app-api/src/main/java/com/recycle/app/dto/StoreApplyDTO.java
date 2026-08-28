package com.recycle.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StoreApplyDTO {

    @NotBlank(message = "门店名称不能为空")
    private String storeName;

    @NotBlank(message = "联系人不能为空")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    private String province;
    private String city;
    private String district;

    @NotBlank(message = "详细地址不能为空")
    private String detail;

    private BigDecimal longitude;
    private BigDecimal latitude;
    private String licenseImage;
    private List<String> storeImages;
    private List<Long> categoryIds;
}
