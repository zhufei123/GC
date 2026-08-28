package com.recycle.common.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志（无 update_time/deleted）
 */
@Data
@TableName("sys_log")
public class SysLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String module;
    private String type;
    private String description;
    private String operator;
    private Long operatorId;
    private String method;
    private String path;
    private String params;
    private Integer resultCode;
    private Integer costMs;
    private String ip;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    public LocalDateTime getCreatedAt() {
        return createTime;
    }

    public String getTitle() {
        return description;
    }

    public String getUri() {
        return path;
    }

    public String getAdminName() {
        return operator;
    }

    /** 与前端约定：0 成功 */
    public Integer getStatus() {
        return resultCode;
    }
}
