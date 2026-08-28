package com.recycle.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SkuPageVO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String image;
    private String unit;
    private String description;
    private Integer sort;
    private Integer status;
    /** 当前生效价，null=暂无报价 */
    private BigDecimal price;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    public LocalDateTime getCreatedAt() {
        return createTime;
    }
}
