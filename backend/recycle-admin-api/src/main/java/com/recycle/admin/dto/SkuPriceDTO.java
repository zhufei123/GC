package com.recycle.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SkuPriceDTO {

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0", message = "价格不能为负")
    private BigDecimal price;

    /** 为空则立即生效 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime effectiveAt;

    private String reason;
}
