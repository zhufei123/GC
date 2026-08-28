package com.recycle.admin.controller.content;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.recycle.admin.dto.StatusDTO;
import com.recycle.admin.service.AdminContentService;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.core.R;
import com.recycle.common.entity.content.Banner;
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

@Tag(name = "管理端-Banner")
@RestController
@RequestMapping("/admin-api/content/banner")
@RequiredArgsConstructor
public class AdminBannerController {

    private final AdminContentService contentService;

    @Operation(summary = "Banner 分页")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "content:banner:list")
    @GetMapping("/page")
    public R<PageResult<Banner>> page(@RequestParam(required = false) Integer status, PageQuery query) {
        return R.ok(contentService.bannerPage(status, query));
    }

    @Operation(summary = "新增 Banner")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "content:banner:add")
    @OpLog(module = "content", type = "ADD", value = "新增Banner")
    @PostMapping
    public R<Map<String, Long>> create(@RequestBody Banner banner) {
        return R.ok(Map.of("id", contentService.createBanner(banner)));
    }

    @Operation(summary = "编辑 Banner")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "content:banner:update")
    @OpLog(module = "content", type = "UPDATE", value = "编辑Banner")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Banner banner) {
        contentService.updateBanner(id, banner);
        return R.ok();
    }

    @Operation(summary = "删除 Banner")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "content:banner:delete")
    @OpLog(module = "content", type = "DELETE", value = "删除Banner")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        contentService.deleteBanner(id);
        return R.ok();
    }

    @Operation(summary = "上下架 Banner")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "content:banner:update")
    @OpLog(module = "content", type = "UPDATE", value = "上下架Banner")
    @PutMapping("/{id}/status")
    public R<Void> status(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        contentService.updateBannerStatus(id, dto.getStatus());
        return R.ok();
    }
}
