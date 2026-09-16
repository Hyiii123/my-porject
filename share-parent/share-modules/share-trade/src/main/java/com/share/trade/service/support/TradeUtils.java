package com.share.trade.service.support;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.share.common.core.exception.ServiceException;
import com.share.common.security.utils.SecurityUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 交易服务通用辅助工具类
 */
public final class TradeUtils {
    private TradeUtils() {}

    public static BigDecimal cents(BigDecimal value) {
        return defaultValue(value, BigDecimal.ZERO).movePointRight(2).setScale(0, RoundingMode.HALF_UP);
    }

    public static Map<String, Object> pageView(long total, List<?> list) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("list", list);
        return result;
    }

    public static long safePage(long value) {
        return value < 1 ? 1 : Math.min(value, 100000);
    }

    public static long safeSize(long value) {
        return value < 1 ? 10 : Math.min(value, 200);
    }

    public static <T> T defaultValue(T value, T fallback) {
        return value == null ? fallback : value;
    }

    public static Long longValue(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) return null;
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static List<Long> longValues(Object value) {
        if (value == null) return List.of();
        if (value instanceof List<?> list) {
            return list.stream().map(TradeUtils::longValue).filter(java.util.Objects::nonNull).toList();
        }
        return Arrays.stream(String.valueOf(value).split(","))
                .map(TradeUtils::longValue)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public static Integer intValue(Object value) {
        Long valueLong = longValue(value);
        return valueLong == null ? null : valueLong.intValue();
    }

    public static int intValue(Object value, int fallback) {
        Long parsed = longValue(value);
        return parsed == null ? fallback : parsed.intValue();
    }

    public static long number(Map<String, ?> map, String key, long fallback) {
        Object value = map == null ? null : map.get(key);
        if (value == null) return fallback;
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static boolean bool(Object value) {
        return value instanceof Boolean b ? b : "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value));
    }

    public static String defaultText(Object value, String fallback) {
        return value == null || !StringUtils.hasText(String.valueOf(value)) ? fallback : String.valueOf(value);
    }

    public static void require(boolean condition, String message) {
        if (!condition) throw new ServiceException(message);
    }

    public static Long currentUserId() {
        Long value = SecurityUtils.getUserId();
        return value == null || value < 1 ? 1L : value;
    }

    public static long newId() {
        return IdWorker.getId();
    }
}
