package com.recycle.common.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.satoken.StpKit;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 拦截规则：
 * - /admin-api/** 除 login 外要求 ADMIN 登录
 * - /app-api/boss/** 要求 BOSS 登录
 * - 其余 /app-api/** 除公开接口（auth/home/recycle/store/nearby/notices）外要求 USER 或 BOSS 已登录
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
                    SaRouter.match("/admin-api/**")
                            .notMatch("/admin-api/auth/login")
                            .check(r -> StpKit.ADMIN.checkLogin());

                    SaRouter.match("/app-api/boss/**")
                            .check(r -> StpKit.BOSS.checkLogin());

                    SaRouter.match("/app-api/**")
                            .notMatch("/app-api/auth/**",
                                    "/app-api/home",
                                    "/app-api/recycle/**",
                                    "/app-api/store/nearby",
                                    "/app-api/notices/**",
                                    "/app-api/boss/**")
                            .check(r -> {
                                if (!StpKit.USER.isLogin() && !StpKit.BOSS.isLogin()) {
                                    throw new BizException(ErrorCode.UNAUTHORIZED);
                                }
                            });
                }))
                .addPathPatterns("/**")
                .excludePathPatterns("/error", "/favicon.ico", "/actuator/**",
                        "/doc.html", "/webjars/**", "/v3/api-docs/**",
                        "/swagger-ui/**", "/swagger-resources/**");
    }
}
