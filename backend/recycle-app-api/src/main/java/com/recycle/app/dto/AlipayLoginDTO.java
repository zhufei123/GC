package com.recycle.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AlipayLoginDTO {

    /** 已配置 oauth 时走 alipay.system.oauth.token；未配置 mock 模式下 authCode 即 openidAlipay */
    @NotBlank(message = "authCode 不能为空")
    private String authCode;

    /** user | boss，默认 user */
    private String client = "user";

    // ---- 以下为可选资料（my.getOpenUserInfo 授权 / H5 mock 联调），有值才落库，不覆盖已有值为空 ----

    @Size(max = 32, message = "昵称最长 32 字")
    private String nickname;

    @Size(max = 512, message = "头像地址过长")
    private String avatar;

    /** 0未知 1男 2女 */
    private Integer gender;

    @Size(max = 64, message = "城市名过长")
    private String city;

    /** 已废弃：登录接口不落库手机号，避免客户端伪造。请走 bind-phone / bind-phone-alipay */
    @Size(max = 20, message = "手机号过长")
    private String phone;
}
