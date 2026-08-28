package com.recycle.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.recycle.admin.service.AdminLogService;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.core.R;
import com.recycle.common.entity.system.SysLog;
import com.recycle.common.satoken.StpKit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-操作日志")
@RestController
@RequestMapping("/admin-api/system/oplog")
@RequiredArgsConstructor
public class OplogController {

    private final AdminLogService logService;

    @Operation(summary = "操作日志分页")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "system:oplog:list")
    @GetMapping("/page")
    public R<PageResult<SysLog>> page(@RequestParam(required = false) String module,
                                      @RequestParam(required = false) String type,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String beginDate,
                                      @RequestParam(required = false) String endDate,
                                      PageQuery query) {
        if (keyword != null && (query.getKeyword() == null || query.getKeyword().isBlank())) {
            query.setKeyword(keyword);
        }
        if (beginDate != null && (query.getBeginDate() == null || query.getBeginDate().isBlank())) {
            query.setBeginDate(beginDate);
        }
        if (endDate != null && (query.getEndDate() == null || query.getEndDate().isBlank())) {
            query.setEndDate(endDate);
        }
        return R.ok(logService.page(module, type, query));
    }

    @Operation(summary = "日志详情")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "system:oplog:list")
    @GetMapping("/{id}")
    public R<SysLog> detail(@PathVariable Long id) {
        return R.ok(logService.detail(id));
    }
}
