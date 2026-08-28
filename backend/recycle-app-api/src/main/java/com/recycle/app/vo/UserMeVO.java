package com.recycle.app.vo;

import lombok.Data;

@Data
public class UserMeVO {

    private Long userId;
    private String phone;
    private String nickname;
    private String avatar;
    private String role;
    private String recyclerStatus;
    private Boolean hasPhone;
    /** recycler 已有门店时返回 */
    private Long storeId;
}
