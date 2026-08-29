package com.recycle.app.vo;

import lombok.Data;

@Data
public class AppLoginVO {

    private String token;
    private Long userId;
    /** customer/recycler */
    private String role;
    private Boolean hasPhone;
    /** 脱敏手机号（138****0001），未绑定为 null */
    private String phoneMasked;
    private String nickname;
    private String avatar;
    /** 是否已绑定微信 openid（原始 openid 不出前端） */
    private Boolean hasWx;
    /** 是否已绑定支付宝 openid */
    private Boolean hasAlipay;
    /** none/pending/approved/rejected */
    private String recyclerStatus;
    /** B 端登录时返回 */
    private Long storeId;
    /** 三方登录返回 */
    private Boolean isNewUser;
}
