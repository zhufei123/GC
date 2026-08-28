package com.recycle.common.entity.content;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.recycle.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notice")
public class Notice extends BaseEntity {

    private String title;
    private String content;
    private Integer pinned;
    /** draft/published/offline */
    private String publishStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishTime;

    /** 管理端表单用 1 发布 / 0 下线，映射 publishStatus */
    public Integer getStatus() {
        return "published".equals(publishStatus) ? 1 : 0;
    }

    public void setStatus(Integer status) {
        if (status == null) {
            return;
        }
        this.publishStatus = status == 1 ? "published" : "offline";
    }

    public LocalDateTime getPublishedAt() {
        return publishTime != null ? publishTime : getCreateTime();
    }
}
