package com.recycle.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.recycle.admin.dto.RoleMenusDTO;
import com.recycle.admin.dto.RoleSaveDTO;
import com.recycle.admin.service.SysRoleService;
import com.recycle.common.core.R;
import com.recycle.common.entity.system.SysRole;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "管理端-角色")
@RestController
@RequestMapping("/admin-api/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @Operation(summary = "角色列表")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "system:role:list")
    @GetMapping
    public R<List<SysRole>> list() {
        return R.ok(roleService.list());
    }

    @Operation(summary = "新增角色")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "system:role:add")
    @OpLog(module = "system", type = "ADD", value = "新增角色")
    @PostMapping
    public R<Map<String, Long>> create(@Valid @RequestBody RoleSaveDTO dto) {
        return R.ok(Map.of("id", roleService.create(dto)));
    }

    @Operation(summary = "编辑角色")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "system:role:update")
    @OpLog(module = "system", type = "UPDATE", value = "编辑角色")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody RoleSaveDTO dto) {
        roleService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "system:role:delete")
    @OpLog(module = "system", type = "DELETE", value = "删除角色")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    @Operation(summary = "角色菜单 ID 列表")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "system:role:list")
    @GetMapping("/{id}/menus")
    public R<List<Long>> menus(@PathVariable Long id) {
        return R.ok(roleService.menuIds(id));
    }

    @Operation(summary = "分配菜单权限")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "system:role:assign")
    @OpLog(module = "system", type = "UPDATE", value = "分配角色菜单")
    @PutMapping("/{id}/menus")
    public R<Void> assignMenus(@PathVariable Long id, @Valid @RequestBody RoleMenusDTO dto) {
        roleService.assignMenus(id, dto.getMenuIds());
        return R.ok();
    }
}
