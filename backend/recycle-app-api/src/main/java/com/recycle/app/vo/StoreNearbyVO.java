package com.recycle.app.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StoreNearbyVO {

    private Long id;
    private String name;
    private String address;
    private String phone;
    private String businessHours;
    private Integer businessStatus;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private BigDecimal distanceKm;
    private List<Long> categoryIds;
    private List<String> photos;

    /** 亮点价：纸类/塑料报价的最高价，无则取最高的一条报价 */
    private BigDecimal highlightPrice;
    /** 报价中 SKU 数 */
    private Integer quotedCount;
    /** 营业中且处于营业时段 */
    private Boolean openNow;
    /** 报价 TOP3（按价格降序） */
    private List<PriceBriefVO> prices;

    @Data
    public static class PriceBriefVO {
        private String skuName;
        private BigDecimal price;
    }
}
