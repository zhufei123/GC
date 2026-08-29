package com.recycle.app.vo;

import lombok.Data;

@Data
public class UserMeVO {

    private Long userId;
    private String phone;
    private String nickname;
    private String avatar;
    /** 0未知 1男 2女 */
    private Integer gender;
    private String city;
    private String role;
    private String recyclerStatus;
    private Boolean hasPhone;
    /** 是否已绑定微信 openid（原始 openid 不出前端） */
    private Boolean hasWx;
    /** 是否已绑定支付宝 openid */
    private Boolean hasAlipay;
    /** recycler 已有门店时返回 */
    private Long storeId;
}
