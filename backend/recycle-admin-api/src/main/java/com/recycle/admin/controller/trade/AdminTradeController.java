package com.recycle.admin.controller.trade;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.recycle.admin.dto.AdminCancelDTO;
import com.recycle.admin.service.AdminTradeService;
import com.recycle.admin.vo.AdminOrderVO;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.core.R;
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

@Tag(name = "管理端-订单")
@RestController
@RequestMapping("/admin-api/trade/order")
@RequiredArgsConstructor
public class AdminTradeController {

    private final AdminTradeService tradeService;

    @Operation(summary = "订单分页（手机号脱敏）")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "trade:order:list")
    @GetMapping("/page")
    public R<PageResult<AdminOrderVO>> page(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) String orderNo,
                                            @RequestParam(required = false) Long userId,
                                            @RequestParam(required = false) String beginDate,
                                            @RequestParam(required = false) String endDate,
                                            PageQuery query) {
        if (beginDate != null && (query.getBeginDate() == null || query.getBeginDate().isBlank())) {
            query.setBeginDate(beginDate);
        }
        if (endDate != null && (query.getEndDate() == null || query.getEndDate().isBlank())) {
            query.setEndDate(endDate);
        }
        return R.ok(tradeService.page(status, orderNo, userId, query));
    }

    @Operation(summary = "订单详情（预估/实收明细、照片、地址快照、时间线）")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "trade:order:list")
    @GetMapping("/{id}")
    public R<AdminOrderVO> detail(@PathVariable Long id) {
        return R.ok(tradeService.detail(id));
    }

    @Operation(summary = "后台取消（非终态）")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "trade:order:cancel")
    @OpLog(module = "trade", type = "UPDATE", value = "后台取消订单")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id, @Valid @RequestBody AdminCancelDTO dto) {
        tradeService.cancel(id, dto.getReason());
        return R.ok();
    }
}
