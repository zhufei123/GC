package com.recycle.app.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StoreDetailVO {

    private Long id;
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
    private Integer businessStatus;
    private Boolean openNow;
    private List<Long> categoryIds;
    private List<String> photos;
    /** 带经纬度查询时返回 */
    private BigDecimal distanceKm;
    /** 报价中 SKU 数 */
    private Integer quotedCount;
}
