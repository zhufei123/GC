package com.recycle.app.controller.store;

import com.recycle.app.service.AppStoreService;
import com.recycle.app.vo.StoreDetailVO;
import com.recycle.app.vo.StoreNearbyVO;
import com.recycle.app.vo.StorePriceVO;
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

@Tag(name = "App-门店（附近/详情/报价单）")
@RestController
@RequestMapping("/app-api/store")
@RequiredArgsConstructor
public class AppStoreController {

    private final AppStoreService storeService;

    @Operation(summary = "附近门店（含亮点价/报价数/TOP3报价）")
    @GetMapping("/nearby")
    public R<List<StoreNearbyVO>> nearby(@RequestParam(required = false) BigDecimal longitude,
                                         @RequestParam(required = false) BigDecimal latitude) {
        return R.ok(storeService.nearby(longitude, latitude));
    }

    @Operation(summary = "门店详情（带经纬度返回距离）")
    @GetMapping("/{id}")
    public R<StoreDetailVO> detail(@PathVariable Long id,
                                   @RequestParam(required = false) BigDecimal longitude,
                                   @RequestParam(required = false) BigDecimal latitude) {
        return R.ok(storeService.detail(id, longitude, latitude));
    }

    @Operation(summary = "门店报价单（对比平台指导价）")
    @GetMapping("/{id}/prices")
    public R<List<StorePriceVO>> prices(@PathVariable Long id) {
        return R.ok(storeService.prices(id));
    }
}
