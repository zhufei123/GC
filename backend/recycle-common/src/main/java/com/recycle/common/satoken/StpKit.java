package com.recycle.common.satoken;

import cn.dev33.satoken.stp.StpLogic;

/**
 * Sa-Token 多账号体系：ADMIN（后台）/ USER（C端）/ BOSS（B端，同 user 表登录端隔离）
 */
public final class StpKit {

    public static final String ADMIN_TYPE = "admin";
    public static final String USER_TYPE = "user";
    public static final String BOSS_TYPE = "boss";

    public static final StpLogic ADMIN = new StpLogic(ADMIN_TYPE);
    public static final StpLogic USER = new StpLogic(USER_TYPE);
    public static final StpLogic BOSS = new StpLogic(BOSS_TYPE);

    private StpKit() {
    }
}
