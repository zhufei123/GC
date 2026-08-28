package com.recycle.common.entity.member;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知记录（notify_log 无 update_time 列，不继承 BaseEntity）
 */
@Data
@TableName("notify_log")
public class NotifyLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    /** WX/ALIPAY/INAPP */
    private String channel;
    private String templateKey;
    private String bizType;
    private Long bizId;
    private String title;
    private String content;
    /** SENT/FAILED */
    private String status;
    private String error;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;
}
