package com.recycle.admin.controller.store;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.recycle.admin.dto.AuditDTO;
import com.recycle.admin.dto.StatusDTO;
import com.recycle.admin.service.AdminStoreService;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.core.R;
import com.recycle.common.entity.store.RecycleStation;
import com.recycle.common.entity.store.StationApply;
import com.recycle.common.log.OpLog;
import com.recycle.common.satoken.StpKit;
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

@Tag(name = "管理端-门店与入驻审核")
@RestController
@RequestMapping("/admin-api/store")
@RequiredArgsConstructor
public class AdminStoreController {

    private final AdminStoreService storeService;

    @Operation(summary = "门店分页")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "store:store:list")
    @GetMapping("/page")
    public R<PageResult<RecycleStation>> page(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(required = false) Integer status,
                                              PageQuery query) {
        if (keyword != null && (query.getKeyword() == null || query.getKeyword().isBlank())) {
            query.setKeyword(keyword);
        }
        return R.ok(storeService.storePage(name, status, query));
    }

    @Operation(summary = "入驻申请分页")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "store:apply:list")
    @GetMapping("/apply/page")
    public R<PageResult<StationApply>> applyPage(@RequestParam(required = false) String auditStatus,
                                                 @RequestParam(required = false) String status,
                                                 PageQuery query) {
        return R.ok(storeService.applyPage(auditStatus != null ? auditStatus : status, query));
    }

    @Operation(summary = "入驻申请详情")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "store:apply:list")
    @GetMapping("/apply/{id}")
    public R<StationApply> applyDetail(@PathVariable Long id) {
        return R.ok(storeService.applyDetail(id));
    }

    @Operation(summary = "入驻审核（通过 → 建门店 + 用户升级 recycler）")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "store:apply:audit")
    @OpLog(module = "store", type = "AUDIT", value = "入驻审核")
    @PostMapping("/apply/{id}/audit")
    public R<Void> audit(@PathVariable Long id, @Valid @RequestBody AuditDTO dto) {
        storeService.audit(id, dto, StpKit.ADMIN.getLoginIdAsLong());
        return R.ok();
    }

    @Operation(summary = "编辑门店")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "store:store:update")
    @OpLog(module = "store", type = "UPDATE", value = "编辑门店")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody RecycleStation store) {
        storeService.updateStore(id, store);
        return R.ok();
    }

    @Operation(summary = "启停门店")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "store:store:update")
    @OpLog(module = "store", type = "UPDATE", value = "启停门店")
    @PutMapping("/{id}/status")
    public R<Void> status(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        storeService.updateStoreStatus(id, dto.getStatus());
        return R.ok();
    }
}
