package com.recycle.app.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BossStoreUpdateDTO {

    private String name;
    private String phone;
    private String contactName;
    private String province;
    private String city;
    private String district;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String businessHours;
    /** 1营业 0休息 */
    private Integer businessStatus;
    private List<Long> categoryIds;
    private List<String> photos;
}
