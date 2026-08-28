package com.recycle.common.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解，配合 OpLogAspect 异步写 sys_log
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpLog {

    /** 模块名 */
    String module();

    /** 操作类型：ADD/UPDATE/DELETE/AUDIT/LOGIN/OTHER */
    String type() default "OTHER";

    /** 描述 */
    String value() default "";
}
