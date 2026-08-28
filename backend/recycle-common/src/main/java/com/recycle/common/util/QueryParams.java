package com.recycle.common.util;

import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理端列表查询：keyword 别名、日期区间解析。
 */
public final class QueryParams {

    private QueryParams() {
    }

    public static String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    public static String lower(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : null;
    }

    /** 订单状态筛选：空 / ALL 表示不过滤 */
    public static String orderStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String trimmed = status.trim();
        if ("ALL".equalsIgnoreCase(trimmed) || "*".equals(trimmed)) {
            return null;
        }
        return trimmed;
    }

    public static LocalDateTime startOfDay(String date) {
        LocalDate parsed = parseDate(date);
        return parsed == null ? null : parsed.atStartOfDay();
    }

    public static LocalDateTime startOfNextDay(String date) {
        LocalDate parsed = parseDate(date);
        return parsed == null ? null : parsed.plusDays(1).atStartOfDay();
    }

    private static LocalDate parseDate(String date) {
        if (!StringUtils.hasText(date)) {
            return null;
        }
        String raw = date.trim();
        if (raw.length() >= 10) {
            raw = raw.substring(0, 10);
        }
        try {
            return LocalDate.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }
}
