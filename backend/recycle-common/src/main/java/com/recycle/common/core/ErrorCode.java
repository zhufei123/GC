package com.recycle.common.core;

import lombok.Getter;

/**
 * 错误码分段：0 成功；4xxxx 鉴权/参数；5xxxx 服务端；1xxxx 用户；2xxxx 订单；3xxxx 商品价格；6xxxx 门店；7xxxx 文件
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),

    PARAM_ERROR(40000, "请求参数错误"),
    UNAUTHORIZED(40100, "未登录或登录已过期"),
    LOGIN_ELSEWHERE(40101, "账号已在其他设备登录"),
    FORBIDDEN(40300, "无权限"),
    NOT_FOUND(40400, "资源不存在"),
    TOO_MANY_REQUESTS(42900, "请求过于频繁"),
    SYSTEM_ERROR(50000, "系统繁忙"),

    USER_NOT_FOUND(10001, "用户不存在"),
    USER_DISABLED(10002, "用户已被禁用"),
    SMS_CODE_ERROR(10003, "验证码错误"),
    PASSWORD_ERROR(10004, "账号或密码错误"),
    NOT_BOSS(10005, "非老板身份"),

    ORDER_NOT_FOUND(20401, "订单不存在"),
    ORDER_STATUS_ILLEGAL(20402, "订单当前状态不允许该操作"),
    ORDER_TAKEN(20403, "订单已被抢"),

    CATEGORY_HAS_CHILDREN(30001, "分类下存在子分类或SKU"),
    SKU_OFFLINE(30002, "SKU已下架"),

    APPLY_DUPLICATE(60001, "重复入驻申请"),
    STORE_NOT_APPROVED(60002, "门店未审核"),

    FILE_TYPE_NOT_ALLOWED(70001, "文件类型不允许"),
    SIGN_FAILED(70002, "签名失败");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
