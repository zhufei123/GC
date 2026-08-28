package com.recycle.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class WeighDTO {

    @NotEmpty(message = "实收明细不能为空")
    @Valid
    private List<WeighItem> items;

    private List<String> images;

    private String remark;

    @Data
    public static class WeighItem {

        @NotNull(message = "skuId 不能为空")
        private Long skuId;

        @NotNull(message = "重量不能为空")
        @DecimalMin(value = "0", inclusive = false, message = "重量必须大于 0")
        private BigDecimal weight;
    }
}
