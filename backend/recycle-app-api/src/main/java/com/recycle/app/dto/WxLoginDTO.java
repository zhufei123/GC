package com.recycle.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WxLoginDTO {

    /** 已配置 appid+secret 时走 jscode2session；未配置 mock 模式下 code 即 openid */
    @NotBlank(message = "code 不能为空")
    private String code;

    /** user | boss，默认 user */
    private String client = "user";

    // ---- 以下为可选资料（uni.getUserProfile 授权 / H5 mock 联调），有值才落库，不覆盖已有值为空 ----

    @Size(max = 32, message = "昵称最长 32 字")
    private String nickname;

    @Size(max = 512, message = "头像地址过长")
    private String avatar;

    /** 0未知 1男 2女 */
    private Integer gender;

    @Size(max = 64, message = "城市名过长")
    private String city;

    /** getUserProfile.province */
    @Size(max = 64, message = "省份名过长")
    private String province;

    /** getUserProfile.country */
    @Size(max = 64, message = "国家名过长")
    private String country;

    /** getUserProfile.language，如 zh_CN */
    @Size(max = 16, message = "语言标识过长")
    private String language;

    /** 已废弃：登录接口不落库手机号，避免客户端伪造。请走 bind-phone / bind-phone-wx */
    @Size(max = 20, message = "手机号过长")
    private String phone;
}
