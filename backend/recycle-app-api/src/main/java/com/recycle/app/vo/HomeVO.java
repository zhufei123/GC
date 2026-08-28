package com.recycle.app.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HomeVO {

    private List<BannerVO> banners;
    private List<HotCategoryVO> hotCategories;
    private List<NoticeVO> notices;

    @Data
    public static class BannerVO {
        private Long id;
        private String title;
        private String image;
        private String linkType;
        private String linkUrl;
    }

    @Data
    public static class HotCategoryVO {
        private Long id;
        private String name;
        private String icon;
    }

    @Data
    public static class NoticeVO {
        private Long id;
        private String title;
        private Integer pinned;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime publishTime;
    }
}
