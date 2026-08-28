package com.recycle.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.recycle.admin.dto.MenuSaveDTO;
import com.recycle.admin.service.SysMenuService;
import com.recycle.admin.vo.MenuNodeVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "管理端-菜单")
@RestController
@RequestMapping("/admin-api/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    @Operation(summary = "全量菜单树（含按钮）")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "system:menu:list")
    @GetMapping("/tree")
    public R<List<MenuNodeVO>> tree() {
        return R.ok(menuService.tree());
    }

    @Operation(summary = "新增菜单")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "system:menu:add")
    @OpLog(module = "system", type = "ADD", value = "新增菜单")
    @PostMapping
    public R<Map<String, Long>> create(@Valid @RequestBody MenuSaveDTO dto) {
        return R.ok(Map.of("id", menuService.create(dto)));
    }

    @Operation(summary = "编辑菜单")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "system:menu:update")
    @OpLog(module = "system", type = "UPDATE", value = "编辑菜单")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody MenuSaveDTO dto) {
        menuService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除菜单")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "system:menu:delete")
    @OpLog(module = "system", type = "DELETE", value = "删除菜单")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return R.ok();
    }
}
