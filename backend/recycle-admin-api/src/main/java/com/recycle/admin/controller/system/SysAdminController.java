package com.recycle.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.recycle.admin.dto.AdminSaveDTO;
import com.recycle.admin.dto.PasswordDTO;
import com.recycle.admin.dto.StatusDTO;
import com.recycle.admin.service.SysAdminService;
import com.recycle.admin.vo.AdminInfoVO;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.core.R;
import com.recycle.common.log.OpLog;
import com.recycle.common.satoken.StpKit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "管理端-管理员")
@RestController
@RequestMapping("/admin-api/system/admin")
@RequiredArgsConstructor
public class SysAdminController {

    private final SysAdminService adminService;

    @Operation(summary = "管理员分页")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "system:admin:list")
    @GetMapping("/page")
    public R<PageResult<AdminInfoVO>> page(@RequestParam(required = false) String username,
                                           @RequestParam(required = false) Integer status,
                                           PageQuery query) {
        return R.ok(adminService.page(username, status, query));
    }

    @Operation(summary = "新增管理员")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "system:admin:add")
    @OpLog(module = "system", type = "ADD", value = "新增管理员")
    @PostMapping
    public R<Map<String, Long>> create(@Valid @RequestBody AdminSaveDTO dto) {
        return R.ok(Map.of("id", adminService.create(dto)));
    }

    @Operation(summary = "编辑管理员")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "system:admin:update")
    @OpLog(module = "system", type = "UPDATE", value = "编辑管理员")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody AdminSaveDTO dto) {
        adminService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "启停管理员")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "system:admin:update")
    @OpLog(module = "system", type = "UPDATE", value = "启停管理员")
    @PutMapping("/{id}/status")
    public R<Void> status(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        adminService.updateStatus(id, dto.getStatus());
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "system:admin:resetPwd")
    @OpLog(module = "system", type = "UPDATE", value = "重置管理员密码")
    @PutMapping("/{id}/password")
    public R<Void> password(@PathVariable Long id, @Valid @RequestBody PasswordDTO dto) {
        adminService.resetPassword(id, dto.getPassword());
        return R.ok();
    }

    @Operation(summary = "删除管理员")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "system:admin:delete")
    @OpLog(module = "system", type = "DELETE", value = "删除管理员")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        adminService.delete(id);
        return R.ok();
    }
}
