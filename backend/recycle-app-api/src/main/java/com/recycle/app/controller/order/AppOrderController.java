package com.recycle.app.controller.order;

import com.recycle.app.dto.OrderCancelDTO;
import com.recycle.app.dto.OrderCreateDTO;
import com.recycle.app.dto.OrderReviewDTO;
import com.recycle.app.service.AppOrderService;
import com.recycle.app.support.CurrentUser;
import com.recycle.app.vo.OrderReviewVO;
import com.recycle.app.vo.OrderVO;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.core.R;
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

import java.util.Map;

@Tag(name = "App-客户订单")
@RestController
@RequestMapping("/app-api/order")
@RequiredArgsConstructor
public class AppOrderController {

    private final AppOrderService orderService;

    @Operation(summary = "上门/到店下单（ESTIMATE 明细+价格快照，状态 PENDING）")
    @PostMapping
    public R<Map<String, Long>> create(@Valid @RequestBody OrderCreateDTO dto) {
        Long id = orderService.create(CurrentUser.id(), dto);
        return R.ok(Map.of("id", id));
    }

    @Operation(summary = "我的订单分页")
    @GetMapping("/page")
    public R<PageResult<OrderVO>> page(@RequestParam(required = false) String status, PageQuery query) {
        return R.ok(orderService.page(CurrentUser.id(), status, query));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public R<OrderVO> detail(@PathVariable Long id) {
        return R.ok(orderService.detail(CurrentUser.id(), id));
    }

    @Operation(summary = "取消订单（仅 PENDING/ACCEPTED）")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id, @Valid @RequestBody OrderCancelDTO dto) {
        orderService.cancel(CurrentUser.id(), id, dto);
        return R.ok();
    }

    @Operation(summary = "评价订单（仅 COMPLETED，一单一评）")
    @PostMapping("/{id}/review")
    public R<Void> review(@PathVariable Long id, @Valid @RequestBody OrderReviewDTO dto) {
        orderService.review(CurrentUser.id(), id, dto);
        return R.ok();
    }

    @Operation(summary = "查看我的订单评价（未评价返回 null）")
    @GetMapping("/{id}/review")
    public R<OrderReviewVO> getReview(@PathVariable Long id) {
        return R.ok(orderService.getReview(CurrentUser.id(), id));
    }
}
