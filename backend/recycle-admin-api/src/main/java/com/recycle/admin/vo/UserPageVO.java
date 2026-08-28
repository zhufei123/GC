package com.recycle.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserPageVO {

    private Long id;
    /** 脱敏手机号 */
    private String phone;
    private String nickname;
    private String avatar;
    private String role;
    private Integer status;
    private String recyclerStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
