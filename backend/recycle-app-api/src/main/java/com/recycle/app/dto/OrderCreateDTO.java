package com.recycle.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrderCreateDTO {

    /** PICKUP 上门 / DROPOFF 到店 */
    @Pattern(regexp = "PICKUP|DROPOFF", message = "订单类型无效")
    private String type = "PICKUP";

    private Long addressId;

    /** 到店单门店 */
    private Long storeId;

    private LocalDate appointDate;

    private String appointPeriod;

    @NotEmpty(message = "预估明细不能为空")
    @Valid
    private List<EstimateItem> estimateItems;

    private List<String> images;

    private String remark;

    /** 客户端幂等键 */
    private String requestId;

    @Data
    public static class EstimateItem {

        @NotNull(message = "skuId 不能为空")
        private Long skuId;

        @NotNull(message = "预估重量不能为空")
        @DecimalMin(value = "0", inclusive = false, message = "预估重量必须大于 0")
        private BigDecimal estimateWeight;
    }
}
