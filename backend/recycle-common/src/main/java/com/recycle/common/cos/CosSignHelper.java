package com.recycle.common.cos;

import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * COS 上传签名（骨架 mock：返回假 credentials，契约不变）
 */
@Component
public class CosSignHelper {

    private static final Set<String> SCENES =
            Set.of("avatar", "order", "weigh", "apply", "banner", "sku", "icon", "notice");
    private static final Set<String> EXTS = Set.of("jpg", "jpeg", "png", "webp");

    @Value("${app.cos.bucket:recycle-mock-1250000000}")
    private String bucket;

    @Value("${app.cos.region:ap-guangzhou}")
    private String region;

    @Value("${app.cos.cdn-host:https://cdn.example.com}")
    private String cdnHost;

    public Map<String, Object> mockSign(String scene, String ext, Integer count) {
        if (scene == null || !SCENES.contains(scene)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "scene 不在白名单");
        }
        String realExt = ext == null ? "" : ext.toLowerCase();
        if (!EXTS.contains(realExt)) {
            throw new BizException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        int n = count == null || count < 1 ? 1 : count;
        if (n > 9) {
            throw new BizException(ErrorCode.PARAM_ERROR, "count 不能超过 9");
        }
        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            keys.add(scene + "/" + day + "/" + UUID.randomUUID().toString().replace("-", "") + "." + realExt);
        }
        long now = System.currentTimeMillis() / 1000;
        Map<String, Object> credentials = new LinkedHashMap<>();
        credentials.put("tmpSecretId", "MOCK_SECRET_ID_" + UUID.randomUUID().toString().replace("-", ""));
        credentials.put("tmpSecretKey", "MOCK_SECRET_KEY_" + UUID.randomUUID().toString().replace("-", ""));
        credentials.put("sessionToken", "MOCK_SESSION_TOKEN_" + UUID.randomUUID().toString().replace("-", ""));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bucket", bucket);
        result.put("region", region);
        result.put("keys", keys);
        result.put("credentials", credentials);
        result.put("startTime", now);
        result.put("expiredTime", now + 1800);
        result.put("cdnHost", cdnHost);
        result.put("mock", true);
        return result;
    }
}
