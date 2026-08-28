package com.recycle.admin.controller.auth;

import com.recycle.admin.dto.AdminLoginDTO;
import com.recycle.admin.service.AdminAuthService;
import com.recycle.admin.vo.AdminLoginVO;
import com.recycle.admin.vo.MenuNodeVO;
import com.recycle.common.core.R;
import com.recycle.common.log.OpLog;
import com.recycle.common.satoken.StpKit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理端-认证与菜单")
@RestController
@RequestMapping("/admin-api/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService authService;

    @Operation(summary = "账密登录")
    @OpLog(module = "auth", type = "LOGIN", value = "管理员登录")
    @PostMapping("/login")
    public R<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    @Operation(summary = "当前管理员 + perms")
    @GetMapping("/me")
    public R<AdminLoginVO> me() {
        return R.ok(authService.me(StpKit.ADMIN.getLoginIdAsLong()));
    }

    @Operation(summary = "动态路由菜单树")
    @GetMapping("/menus")
    public R<List<MenuNodeVO>> menus() {
        return R.ok(authService.menus(StpKit.ADMIN.getLoginIdAsLong()));
    }
}
