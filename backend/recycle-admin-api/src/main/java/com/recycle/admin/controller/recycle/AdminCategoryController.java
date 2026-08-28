package com.recycle.admin.controller.recycle;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.recycle.admin.dto.CategorySaveDTO;
import com.recycle.admin.dto.StatusDTO;
import com.recycle.admin.service.AdminCategoryService;
import com.recycle.admin.vo.CategoryTreeVO;
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

@Tag(name = "管理端-分类")
@RestController
@RequestMapping("/admin-api/recycle/category")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService categoryService;

    @Operation(summary = "分类树（全量）")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "recycle:category:list")
    @GetMapping("/tree")
    public R<List<CategoryTreeVO>> tree() {
        return R.ok(categoryService.tree());
    }

    @Operation(summary = "新增分类")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "recycle:category:add")
    @OpLog(module = "recycle", type = "ADD", value = "新增分类")
    @PostMapping
    public R<Map<String, Long>> create(@Valid @RequestBody CategorySaveDTO dto) {
        return R.ok(Map.of("id", categoryService.create(dto)));
    }

    @Operation(summary = "编辑分类")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "recycle:category:update")
    @OpLog(module = "recycle", type = "UPDATE", value = "编辑分类")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CategorySaveDTO dto) {
        categoryService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除分类（有子级或 SKU 返回 30001）")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "recycle:category:delete")
    @OpLog(module = "recycle", type = "DELETE", value = "删除分类")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }

    @Operation(summary = "上下架分类")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "recycle:category:update")
    @OpLog(module = "recycle", type = "UPDATE", value = "上下架分类")
    @PutMapping("/{id}/status")
    public R<Void> status(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        categoryService.updateStatus(id, dto.getStatus());
        return R.ok();
    }
}
