package com.recycle.common.core;

import lombok.Data;

/**
 * 统一响应：{code, msg, data, ts}，code=0 成功
 */
@Data
public class R<T> {

    private int code;
    private String msg;
    private T data;
    private long ts;

    public R() {
        this.ts = System.currentTimeMillis();
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(0);
        r.setMsg("ok");
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMsg());
    }
}
