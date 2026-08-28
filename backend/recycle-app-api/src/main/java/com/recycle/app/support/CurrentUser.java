package com.recycle.app.support;

import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.satoken.StpKit;

/**
 * 当前登录用户（USER 优先，其次 BOSS，同一张 user 表）
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id() {
        if (StpKit.USER.isLogin()) {
            return StpKit.USER.getLoginIdAsLong();
        }
        if (StpKit.BOSS.isLogin()) {
            return StpKit.BOSS.getLoginIdAsLong();
        }
        throw new BizException(ErrorCode.UNAUTHORIZED);
    }

    public static Long bossId() {
        if (StpKit.BOSS.isLogin()) {
            return StpKit.BOSS.getLoginIdAsLong();
        }
        throw new BizException(ErrorCode.UNAUTHORIZED);
    }
}
