package com.recycle.common.util;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

/**
 * Jackson 3 静态工具（DB JSON 列 ↔ Java 对象）
 */
public final class JsonUtils {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private JsonUtils() {
    }

    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    public static List<String> toStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    public static Map<String, Object> toMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    public static List<Long> toLongList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<Long>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }
}
