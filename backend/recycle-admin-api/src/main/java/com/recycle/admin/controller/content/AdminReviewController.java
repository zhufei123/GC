package com.recycle.admin.controller.content;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.recycle.admin.dto.ReviewAuditDTO;
import com.recycle.admin.service.AdminReviewService;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.core.R;
import com.recycle.common.entity.trade.OrderReview;
import com.recycle.common.log.OpLog;
import com.recycle.common.satoken.StpKit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-评价审核")
@RestController
@RequestMapping("/admin-api/content/review")
@RequiredArgsConstructor
public class AdminReviewController {

    private final AdminReviewService reviewService;

    @Operation(summary = "评价分页（含待审/已通过/已拒绝）")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "content:review:list")
    @GetMapping("/page")
    public R<PageResult<OrderReview>> page(@RequestParam(required = false) String auditStatus,
                                           @RequestParam(required = false) Long stationId,
                                           PageQuery query) {
        return R.ok(reviewService.page(auditStatus, stationId, query));
    }

    @Operation(summary = "审核评价")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "content:review:audit")
    @OpLog(module = "content", type = "AUDIT", value = "审核评价")
    @PostMapping("/{id}/audit")
    public R<Void> audit(@PathVariable Long id, @Valid @RequestBody ReviewAuditDTO dto) {
        reviewService.audit(id, dto);
        return R.ok();
    }
}
