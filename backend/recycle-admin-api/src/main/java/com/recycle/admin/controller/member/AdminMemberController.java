package com.recycle.admin.controller.member;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.recycle.admin.dto.StatusDTO;
import com.recycle.admin.service.AdminMemberService;
import com.recycle.admin.vo.UserPageVO;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.core.R;
import com.recycle.common.log.OpLog;
import com.recycle.common.satoken.StpKit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-用户")
@RestController
@RequestMapping("/admin-api/member/user")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService memberService;

    @Operation(summary = "用户分页（手机号脱敏）")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "member:user:list")
    @GetMapping("/page")
    public R<PageResult<UserPageVO>> page(@RequestParam(required = false) String phone,
                                          @RequestParam(required = false) String role,
                                          @RequestParam(required = false) Integer status,
                                          PageQuery query) {
        return R.ok(memberService.page(phone, role, status, query));
    }

    @Operation(summary = "用户详情")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "member:user:list")
    @GetMapping("/{id}")
    public R<UserPageVO> detail(@PathVariable Long id) {
        return R.ok(memberService.detail(id));
    }

    @Operation(summary = "启停用户")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "member:user:update")
    @OpLog(module = "member", type = "UPDATE", value = "启停用户")
    @PutMapping("/{id}/status")
    public R<Void> status(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        memberService.updateStatus(id, dto.getStatus());
        return R.ok();
    }
}
