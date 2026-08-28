package com.recycle.app.controller.order;

import com.recycle.app.dto.StoreApplyDTO;
import com.recycle.app.service.AppStoreApplyService;
import com.recycle.app.support.CurrentUser;
import com.recycle.common.core.R;
import com.recycle.common.entity.store.StationApply;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "App-回收站入驻")
@RestController
@RequestMapping("/app-api/store/apply")
@RequiredArgsConstructor
public class AppStoreApplyController {

    private final AppStoreApplyService applyService;

    @Operation(summary = "提交入驻申请")
    @PostMapping
    public R<Map<String, Long>> apply(@Valid @RequestBody StoreApplyDTO dto) {
        Long id = applyService.apply(CurrentUser.id(), dto);
        return R.ok(Map.of("id", id));
    }

    @Operation(summary = "我的最新入驻申请")
    @GetMapping("/latest")
    public R<StationApply> latest() {
        return R.ok(applyService.latest(CurrentUser.id()));
    }
}
