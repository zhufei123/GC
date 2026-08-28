package com.recycle.app.controller.boss;

import com.recycle.app.dto.BossPriceSaveDTO;
import com.recycle.app.dto.BossStoreUpdateDTO;
import com.recycle.app.dto.CompleteDTO;
import com.recycle.app.dto.WeighDTO;
import com.recycle.app.service.BossService;
import com.recycle.app.support.CurrentUser;
import com.recycle.app.vo.BossPriceVO;
import com.recycle.app.vo.OrderVO;
import com.recycle.app.vo.WorkbenchVO;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.core.R;
import com.recycle.common.entity.store.RecycleStation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "App-老板端")
@RestController
@RequestMapping("/app-api/boss")
@RequiredArgsConstructor
public class BossController {

    private final BossService bossService;

    @Operation(summary = "工作台")
    @GetMapping("/workbench")
    public R<WorkbenchVO> workbench() {
        return R.ok(bossService.workbench(CurrentUser.bossId()));
    }

    @Operation(summary = "接单大厅（PENDING 脱敏摘要）")
    @GetMapping("/order/pool")
    public R<PageResult<OrderVO>> pool(PageQuery query) {
        return R.ok(bossService.pool(CurrentUser.bossId(), query));
    }

    @Operation(summary = "门店订单分页")
    @GetMapping("/order/page")
    public R<PageResult<OrderVO>> orderPage(@RequestParam(required = false) String status, PageQuery query) {
        return R.ok(bossService.orderPage(CurrentUser.bossId(), status, query));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/order/{id}")
    public R<OrderVO> orderDetail(@PathVariable Long id) {
        return R.ok(bossService.orderDetail(CurrentUser.bossId(), id));
    }

    @Operation(summary = "抢单（乐观锁 WHERE status=PENDING，已被抢 20403）")
    @PostMapping("/order/{id}/accept")
    public R<Void> accept(@PathVariable Long id) {
        bossService.accept(CurrentUser.bossId(), id);
        return R.ok();
    }

    @Operation(summary = "开始服务 ACCEPTED→SERVING")
    @PostMapping("/order/{id}/start")
    public R<Void> start(@PathVariable Long id) {
        bossService.start(CurrentUser.bossId(), id);
        return R.ok();
    }

    @Operation(summary = "提交称重 SERVING→WEIGHED（实收明细+价格快照）")
    @PostMapping("/order/{id}/weigh")
    public R<Map<String, BigDecimal>> weigh(@PathVariable Long id, @Valid @RequestBody WeighDTO dto) {
        BigDecimal total = bossService.weigh(CurrentUser.bossId(), id, dto);
        return R.ok(Map.of("actualAmount", total));
    }

    @Operation(summary = "完成订单 WEIGHED→COMPLETED（线下付款，金额需一致）")
    @PostMapping("/order/{id}/complete")
    public R<Void> complete(@PathVariable Long id, @Valid @RequestBody CompleteDTO dto) {
        bossService.complete(CurrentUser.bossId(), id, dto);
        return R.ok();
    }

    @Operation(summary = "报价管理：全部上架 SKU + 指导价 + 本店报价")
    @GetMapping("/prices")
    public R<Map<String, List<BossPriceVO>>> prices() {
        return R.ok(Map.of("list", bossService.prices(CurrentUser.bossId())));
    }

    @Operation(summary = "批量报价 upsert（报价中 price>0，status 0/1）")
    @PutMapping("/prices")
    public R<Void> savePrices(@Valid @RequestBody BossPriceSaveDTO dto) {
        bossService.savePrices(CurrentUser.bossId(), dto);
        return R.ok();
    }

    @Operation(summary = "批量报价（POST 别名，语义同 PUT）")
    @PostMapping("/prices")
    public R<Void> createPrices(@Valid @RequestBody BossPriceSaveDTO dto) {
        bossService.savePrices(CurrentUser.bossId(), dto);
        return R.ok();
    }

    @Operation(summary = "我的门店")
    @GetMapping("/store")
    public R<RecycleStation> getStore() {
        return R.ok(bossService.getStore(CurrentUser.bossId()));
    }

    @Operation(summary = "更新门店（含营业状态）")
    @PutMapping("/store")
    public R<Void> updateStore(@RequestBody BossStoreUpdateDTO dto) {
        bossService.updateStore(CurrentUser.bossId(), dto);
        return R.ok();
    }
}
