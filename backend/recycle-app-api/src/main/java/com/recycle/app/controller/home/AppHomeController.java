package com.recycle.app.controller.home;

import com.recycle.app.service.AppHomeService;
import com.recycle.app.vo.HomeVO;
import com.recycle.app.vo.StoreNearbyVO;
import com.recycle.app.vo.TimeslotVO;
import com.recycle.common.core.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "App-首页/时段/附近门店")
@RestController
@RequestMapping("/app-api")
@RequiredArgsConstructor
public class AppHomeController {

    private final AppHomeService homeService;

    @Operation(summary = "首页聚合：banners + hotCategories + notices")
    @GetMapping("/home")
    public R<HomeVO> home() {
        return R.ok(homeService.home());
    }

    @Operation(summary = "可预约时段枚举")
    @GetMapping("/timeslots")
    public R<List<TimeslotVO>> timeslots() {
        return R.ok(homeService.timeslots());
    }

    @Operation(summary = "附近门店")
    @GetMapping("/store/nearby")
    public R<List<StoreNearbyVO>> nearby(@RequestParam(required = false) BigDecimal longitude,
                                         @RequestParam(required = false) BigDecimal latitude) {
        return R.ok(homeService.nearby(longitude, latitude));
    }
}
