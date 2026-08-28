package com.recycle.admin.controller.common;

import com.recycle.admin.dto.AdminCosSignDTO;
import com.recycle.common.core.R;
import com.recycle.common.cos.CosSignHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "管理端-公共")
@RestController
@RequestMapping("/admin-api/common")
@RequiredArgsConstructor
public class AdminCommonController {

    private final CosSignHelper cosSignHelper;

    @Operation(summary = "COS 上传签名（mock 假 credentials）")
    @PostMapping("/cos/upload-sign")
    public R<Map<String, Object>> uploadSign(@Valid @RequestBody AdminCosSignDTO dto) {
        return R.ok(cosSignHelper.mockSign(dto.getScene(), dto.getExt(), dto.getCount()));
    }
}
