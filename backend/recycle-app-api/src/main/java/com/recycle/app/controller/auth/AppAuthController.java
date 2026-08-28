package com.recycle.app.controller.auth;

import com.recycle.app.dto.PhoneLoginDTO;
import com.recycle.app.dto.SmsCodeDTO;
import com.recycle.app.dto.WxLoginDTO;
import com.recycle.app.service.AppAuthService;
import com.recycle.app.vo.AppLoginVO;
import com.recycle.common.core.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "App-认证")
@RestController
@RequestMapping("/app-api/auth")
@RequiredArgsConstructor
public class AppAuthController {

    private final AppAuthService authService;

    @Operation(summary = "发送短信验证码（mock 固定 123456，60s 限频）")
    @PostMapping("/sms-code")
    public R<Void> smsCode(@Valid @RequestBody SmsCodeDTO dto) {
        authService.sendSmsCode(dto);
        return R.ok();
    }

    @Operation(summary = "手机号+验证码登录（client=user|boss）")
    @PostMapping("/phone-login")
    public R<AppLoginVO> phoneLogin(@Valid @RequestBody PhoneLoginDTO dto) {
        return R.ok(authService.phoneLogin(dto));
    }

    @Operation(summary = "微信登录（mock：code 当 openid）")
    @PostMapping("/wx-login")
    public R<AppLoginVO> wxLogin(@Valid @RequestBody WxLoginDTO dto) {
        return R.ok(authService.wxLogin(dto));
    }

    @Operation(summary = "登出当前端")
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }
}
