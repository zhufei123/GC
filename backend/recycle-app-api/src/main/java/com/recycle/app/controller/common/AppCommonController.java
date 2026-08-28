package com.recycle.app.controller.common;

import com.recycle.app.dto.CosSignDTO;
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

@Tag(name = "App-公共")
@RestController
@RequestMapping("/app-api/common")
@RequiredArgsConstructor
public class AppCommonController {

    private final CosSignHelper cosSignHelper;

    @Operation(summary = "COS 上传签名（mock 假 credentials，契约不变）")
    @PostMapping("/cos/upload-sign")
    public R<Map<String, Object>> uploadSign(@Valid @RequestBody CosSignDTO dto) {
        return R.ok(cosSignHelper.mockSign(dto.getScene(), dto.getExt(), dto.getCount()));
    }
}
