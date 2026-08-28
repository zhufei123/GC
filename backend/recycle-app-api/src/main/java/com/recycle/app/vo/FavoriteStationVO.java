package com.recycle.app.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FavoriteStationVO {

    private Long id;
    private String name;
    private String address;
    private String phone;
    private String businessHours;
    private Integer businessStatus;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private List<String> photos;
    /** 门店已下线/停用时为 false，前端提示 */
    private Boolean available;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime favoritedAt;
}
