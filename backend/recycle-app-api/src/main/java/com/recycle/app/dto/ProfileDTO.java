package com.recycle.app.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileDTO {

    @Size(max = 32, message = "昵称最长 32 字")
    private String nickname;

    @Size(max = 512, message = "头像地址过长")
    private String avatar;
}
