package com.recycle.app.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CompleteDTO {

    @NotNull(message = "确认金额不能为空")
    private BigDecimal confirmAmount;
}
