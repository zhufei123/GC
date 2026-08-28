package com.recycle.admin.controller.finance;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.core.R;
import com.recycle.common.entity.trade.PayoutOrder;
import com.recycle.common.mapper.PayoutOrderMapper;
import com.recycle.common.satoken.StpKit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-财务")
@RestController
@RequestMapping("/admin-api/finance")
@RequiredArgsConstructor
public class AdminFinanceController {

    private final PayoutOrderMapper payoutOrderMapper;

    @Operation(summary = "打款单分页（复用订单查看权限）")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "trade:order:list")
    @GetMapping("/payout/page")
    public R<PageResult<PayoutOrder>> page(@RequestParam(required = false) String status,
                                           @RequestParam(required = false) String channel,
                                           @RequestParam(required = false) Long orderId,
                                           @RequestParam(required = false) Long userId,
                                           PageQuery query) {
        Page<PayoutOrder> page = payoutOrderMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<PayoutOrder>()
                        .eq(StringUtils.hasText(status), PayoutOrder::getStatus, status)
                        .eq(StringUtils.hasText(channel), PayoutOrder::getChannel, channel)
                        .eq(orderId != null, PayoutOrder::getOrderId, orderId)
                        .eq(userId != null, PayoutOrder::getUserId, userId)
                        .orderByDesc(PayoutOrder::getId));
        return R.ok(PageResult.of(page));
    }
}
