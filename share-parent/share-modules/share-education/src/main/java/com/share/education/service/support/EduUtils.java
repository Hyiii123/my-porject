package com.share.education.service.support;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.common.core.exception.ServiceException;
import com.share.common.security.utils.SecurityUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教育服务通用工具与类型转换助手
 */
public final class EduUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private EduUtils() {}

    public static String cleanHtmlTags(String text) {
        if (!StringUtils.hasText(text)) return "";
        return text.replaceAll("<[^>]*>", "").replaceAll("&[a-zA-Z]{1,10};", " ").trim();
    }

    public static void require(boolean condition, String message) {
        if (!condition) throw new ServiceException(message);
    }

    public static long newId() {
        return IdWorker.getId();
    }

    public static Long currentUserId() {
        Long value = SecurityUtils.getUserId();
        return value == null || value < 1 ? null : value;
    }

    public static Long requireCurrentUserId() {
        Long value = SecurityUtils.getUserId();
        if (value == null || value < 1) {
            throw new ServiceException("当前操作需要登录，请先登录");
        }
        return value;
    }

    public static String currentUserName() {
        return StringUtils.hasText(SecurityUtils.getUsername()) ? SecurityUtils.getUsername() : "学习者";
    }

    public static BigDecimal moneyCents(BigDecimal value) {
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

    public static long number(Map<String, ?> params, String key, long fallback) {
        Object value = params == null ? null : params.get(key);
        if (value == null) return fallback;
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static Long longValue(Object value) {
        if (value == null || "".equals(String.valueOf(value))) return null;
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static Long firstLong(Object value) {
        if (value instanceof List<?> list) return list.isEmpty() ? null : firstLong(list.get(list.size() - 1));
        if (value != null && value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            return array.length == 0 ? null : firstLong(array[array.length - 1]);
        }
        return longValue(value);
    }

    public static int intValue(Object value, int fallback) {
        Long parsed = longValue(value);
        return parsed == null ? fallback : parsed.intValue();
    }

    public static BigDecimal decimalValue(Object value, BigDecimal fallback) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) return fallback;
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static BigDecimal moneyYuan(Object value, BigDecimal fallback) {
        BigDecimal cents = decimalValue(value, null);
        return cents == null ? fallback : cents.movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
    }

    public static int durationSeconds(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number number) return number.intValue();
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return fallback;
        if (text.contains(":")) {
            String[] parts = text.split(":");
            try {
                int seconds = Integer.parseInt(parts[parts.length - 1]);
                int minutes = parts.length > 1 ? Integer.parseInt(parts[parts.length - 2]) : 0;
                int hours = parts.length > 2 ? Integer.parseInt(parts[parts.length - 3]) : 0;
                return hours * 3600 + minutes * 60 + seconds;
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static int durationSeconds(LocalDateTime startedAt, LocalDateTime submittedAt) {
        if (startedAt == null || submittedAt == null || submittedAt.isBefore(startedAt)) return 0;
        long seconds = java.time.Duration.between(startedAt, submittedAt).getSeconds();
        return seconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seconds;
    }

    public static int catalogType(Map<String, ?> source, int fallback) {
        int value = intValue(source.get("catalogType"), intValue(source.get("type"), fallback));
        return value < 1 ? fallback : value;
    }

    public static Object unwrapData(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            if (map.containsKey("datas")) return map.get("datas");
            if (map.containsKey("data")) return map.get("data");
            if (map.containsKey("list")) return map.get("list");
        }
        return payload;
    }

    public static List<Map<String, ?>> mapList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        List<Map<String, ?>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) result.add(castMap(map));
        }
        return result;
    }

    public static List<?> listValues(Object value) {
        if (value instanceof List<?> list) return list;
        if (value == null) return List.of();
        String text = String.valueOf(value);
        if (!StringUtils.hasText(text)) return List.of();
        if (text.startsWith("[") && text.endsWith("]")) {
            try {
                return OBJECT_MAPPER.readValue(text, List.class);
            } catch (Exception ignored) {}
        }
        return Arrays.stream(text.split(",|，|、")).map(String::trim).filter(StringUtils::hasText).toList();
    }

    public static boolean isQuestionBankQuery(Map<String, ?> params) {
        if (params == null) return false;
        return params.containsKey("type") || params.containsKey("questionType") || params.containsKey("categoryId")
                || bool(params.get("isBank"));
    }

    public static String normalizeQuestionType(String value) {
        if (!StringUtils.hasText(value)) return value;
        return switch (value) {
            case "single", "single_choice", "1" -> "single";
            case "multiple", "multiple_choice", "2" -> "multiple";
            case "judge", "判断", "3" -> "judge";
            case "fill", "blank", "4" -> "blank";
            default -> value;
        };
    }

    public static int legacyQuestionType(String value) {
        if (!StringUtils.hasText(value)) return 1;
        String normalized = normalizeQuestionType(value);
        return switch (normalized) {
            case "single" -> 1;
            case "multiple" -> 2;
            case "judge" -> 4;
            case "blank", "fill", "essay" -> 5;
            default -> intValue(value, 5);
        };
    }

    public static boolean sameAnswer(String expected, String actual, String questionType) {
        if (!StringUtils.hasText(expected) || !StringUtils.hasText(actual)) return false;
        return normalizeAnswer(expected, questionType).equalsIgnoreCase(normalizeAnswer(actual, questionType));
    }

    public static String normalizeAnswer(String value, String questionType) {
        if (!StringUtils.hasText(value)) return "";
        String type = normalizeQuestionType(questionType);
        String raw = value.trim();
        if (raw.startsWith("[") && raw.endsWith("]")) {
            try {
                List<?> values = OBJECT_MAPPER.readValue(raw, List.class);
                raw = values.stream().map(String::valueOf).collect(Collectors.joining(","));
            } catch (Exception ignored) {}
        }
        if ("judge".equals(type)) {
            if ("true".equalsIgnoreCase(raw) || "正确".equals(raw) || "1".equals(raw) || "A".equalsIgnoreCase(raw)) return "A";
            if ("false".equalsIgnoreCase(raw) || "错误".equals(raw) || "0".equals(raw) || "2".equals(raw) || "B".equalsIgnoreCase(raw)) return "B";
            return raw.replaceAll("[\\s,，、]", "").toUpperCase();
        }
        if (!"single".equals(type) && !"multiple".equals(type)) return raw;
        String[] tokens = raw.split("[,，、;；\\s]+");
        if (tokens.length == 1 && "multiple".equals(type) && tokens[0].length() > 1
                && tokens[0].matches("[A-Za-z0-9]+")) {
            tokens = tokens[0].split("");
        }
        List<String> normalized = new ArrayList<>();
        for (String token : tokens) {
            if (!StringUtils.hasText(token)) continue;
            String item = token.trim();
            if (item.matches("\\d+")) {
                int index = intValue(item, -1);
                if (index >= 1 && index <= 26) item = String.valueOf((char) ('A' + index - 1));
            } else {
                item = item.toUpperCase();
            }
            normalized.add(item);
            if ("single".equals(type)) break;
        }
        if ("multiple".equals(type)) {
            return normalized.stream().distinct().sorted().collect(Collectors.joining());
        }
        return normalized.isEmpty() ? "" : normalized.get(0);
    }

    public static boolean bool(Object value) {
        return value instanceof Boolean b ? b : "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value));
    }

    public static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public static String defaultText(Object value, String fallback) {
        return value == null || !StringUtils.hasText(String.valueOf(value)) ? fallback : String.valueOf(value);
    }

    public static String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    public static <T> T defaultValue(T value, T fallback) {
        return value == null ? fallback : value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> castMap(Map<?, ?> value) {
        Map<String, Object> result = new LinkedHashMap<>();
        value.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }
}

