package com.recycle.app.vo;

import lombok.Data;

@Data
public class AppLoginVO {

    private String token;
    private Long userId;
    /** customer/recycler */
    private String role;
    private Boolean hasPhone;
    private String nickname;
    /** none/pending/approved/rejected */
    private String recyclerStatus;
    /** B 端登录时返回 */
    private Long storeId;
    /** 三方登录返回 */
    private Boolean isNewUser;
}
