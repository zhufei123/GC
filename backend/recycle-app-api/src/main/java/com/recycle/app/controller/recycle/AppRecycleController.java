package com.recycle.app.controller.recycle;

import com.recycle.app.service.AppRecycleService;
import com.recycle.app.service.AppStoreService;
import com.recycle.app.vo.CategoryNodeVO;
import com.recycle.app.vo.SkuQuoteVO;
import com.recycle.app.vo.SkuVO;
import com.recycle.common.core.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "App-查价")
@RestController
@RequestMapping("/app-api/recycle")
@RequiredArgsConstructor
public class AppRecycleController {

    private final AppRecycleService recycleService;
    private final AppStoreService storeService;

    @Operation(summary = "分类树（仅上架）")
    @GetMapping("/category/tree")
    public R<List<CategoryNodeVO>> categoryTree() {
        return R.ok(recycleService.categoryTree());
    }

    @Operation(summary = "SKU 列表（带今日价，无价为 null）")
    @GetMapping("/sku/list")
    public R<List<SkuVO>> skuList(@RequestParam(required = false) Long categoryId) {
        return R.ok(recycleService.skuList(categoryId, null));
    }

    @Operation(summary = "SKU 搜索")
    @GetMapping("/sku/search")
    public R<List<SkuVO>> skuSearch(@RequestParam String keyword) {
        return R.ok(recycleService.skuList(null, keyword));
    }

    @Operation(summary = "某 SKU 的附近门店报价（距离优先，其次价高）")
    @GetMapping("/sku/{skuId}/quotes")
    public R<List<SkuQuoteVO>> skuQuotes(@PathVariable Long skuId,
                                         @RequestParam(required = false) BigDecimal longitude,
                                         @RequestParam(required = false) BigDecimal latitude) {
        return R.ok(storeService.skuQuotes(skuId, longitude, latitude));
    }
}
