package com.recycle.app.dto;

import lombok.Data;

@Data
public class BindPhoneAlipayDTO {

    /** 支付宝小程序 getPhoneNumber 返回的加密数据（response） */
    private String encryptedData;
}
