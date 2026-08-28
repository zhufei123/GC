package com.recycle.admin.controller.dashboard;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.recycle.admin.service.AdminDashboardService;
import com.recycle.admin.vo.DashboardSummaryVO;
import com.recycle.common.core.R;
import com.recycle.common.satoken.StpKit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-数据看板")
@RestController
@RequestMapping("/admin-api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AdminDashboardService dashboardService;

    @Operation(summary = "看板汇总")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "dashboard:view")
    @GetMapping("/summary")
    public R<DashboardSummaryVO> summary() {
        return R.ok(dashboardService.summary());
    }
}
