package com.recycle.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BossPriceSaveDTO {

    @NotEmpty(message = "报价明细不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "skuId 不能为空")
        private Long skuId;

        /** 报价中(status=1)必须 > 0；停报行未填价时可为 0 */
        private BigDecimal price;

        /** 1报价中 0停报，缺省 1 */
        private Integer status;

        private String remark;
    }
}
