package com.recycle.admin.controller.content;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.recycle.admin.service.AdminContentService;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.core.R;
import com.recycle.common.entity.content.Notice;
import com.recycle.common.log.OpLog;
import com.recycle.common.satoken.StpKit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "管理端-公告")
@RestController
@RequestMapping("/admin-api/content/notice")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final AdminContentService contentService;

    @Operation(summary = "公告分页")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "content:notice:list")
    @GetMapping("/page")
    public R<PageResult<Notice>> page(@RequestParam(required = false) String publishStatus,
                                      @RequestParam(required = false) String title,
                                      PageQuery query) {
        return R.ok(contentService.noticePage(publishStatus, title, query));
    }

    @Operation(summary = "新增公告")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "content:notice:add")
    @OpLog(module = "content", type = "ADD", value = "新增公告")
    @PostMapping
    public R<Map<String, Long>> create(@RequestBody Notice notice) {
        return R.ok(Map.of("id", contentService.createNotice(notice)));
    }

    @Operation(summary = "编辑公告")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "content:notice:update")
    @OpLog(module = "content", type = "UPDATE", value = "编辑公告")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Notice notice) {
        contentService.updateNotice(id, notice);
        return R.ok();
    }

    @Operation(summary = "删除公告")
    @SaCheckPermission(type = StpKit.ADMIN_TYPE, value = "content:notice:delete")
    @OpLog(module = "content", type = "DELETE", value = "删除公告")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        contentService.deleteNotice(id);
        return R.ok();
    }
}
