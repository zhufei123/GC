package com.recycle.admin.controller.recycle;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.recycle.admin.dto.SkuPriceDTO;
import com.recycle.admin.dto.SkuSaveDTO;
import com.recycle.admin.dto.StatusDTO;
import com.recycle.admin.service.AdminSkuService;
import com.recycle.admin.vo.SkuPageVO;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.core.R;
import com.recycle.common.entity.recycle.SkuPriceLog;
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

import java.util.List;
import java.util.Map;

@Tag(name = "管理端-SKU与价格")
@RestController
@RequestMapping("/admin-api/recycle/sku")
@RequiredArgsConstructor
public class AdminSkuController {

    private final AdminSkuService skuService;

    @Operation(summary = "SKU 分页（含当前价）")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "recycle:sku:list")
    @GetMapping("/page")
    public R<PageResult<SkuPageVO>> page(@RequestParam(required = false) Long categoryId,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String name,
                                         @RequestParam(required = false) Integer status,
                                         PageQuery query) {
        if (keyword != null && (query.getKeyword() == null || query.getKeyword().isBlank())) {
            query.setKeyword(keyword);
        }
        return R.ok(skuService.page(categoryId, name, status, query));
    }

    @Operation(summary = "SKU 详情")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "recycle:sku:list")
    @GetMapping("/{id}")
    public R<SkuPageVO> detail(@PathVariable Long id) {
        return R.ok(skuService.detail(id));
    }

    @Operation(summary = "新增 SKU（可带初始价）")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "recycle:sku:add")
    @OpLog(module = "recycle", type = "ADD", value = "新增SKU")
    @PostMapping
    public R<Map<String, Long>> create(@Valid @RequestBody SkuSaveDTO dto) {
        return R.ok(Map.of("id", skuService.create(dto)));
    }

    @Operation(summary = "编辑 SKU")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "recycle:sku:update")
    @OpLog(module = "recycle", type = "UPDATE", value = "编辑SKU")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SkuSaveDTO dto) {
        skuService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除 SKU")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "recycle:sku:delete")
    @OpLog(module = "recycle", type = "DELETE", value = "删除SKU")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        skuService.delete(id);
        return R.ok();
    }

    @Operation(summary = "上下架 SKU")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "recycle:sku:update")
    @OpLog(module = "recycle", type = "UPDATE", value = "上下架SKU")
    @PutMapping("/{id}/status")
    public R<Void> status(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        skuService.updateStatus(id, dto.getStatus());
        return R.ok();
    }

    @Operation(summary = "改价（写 sku_price + sku_price_log）")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "recycle:sku:price")
    @OpLog(module = "recycle", type = "UPDATE", value = "SKU改价")
    @PutMapping("/{id}/price")
    public R<Void> price(@PathVariable Long id, @Valid @RequestBody SkuPriceDTO dto) {
        skuService.changePrice(id, dto, StpKit.ADMIN.getLoginIdAsLong());
        return R.ok();
    }

    @Operation(summary = "调价记录")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "recycle:sku:list")
    @GetMapping("/{id}/price-log")
    public R<List<SkuPriceLog>> priceLog(@PathVariable Long id) {
        return R.ok(skuService.priceLog(id));
    }
}
