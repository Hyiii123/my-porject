package com.share.education.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.education.domain.EduDashboardDaily;
import com.share.education.service.EducationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理工作台兼容接口。
 *
 * <p>旧管理端通过 /ds/data/* 读取看板数据，Gateway 去掉 /ds 前缀后转发到这里。
 * 趋势和今日摘要统一读取教育域的 MySQL 日统计表，保持原图表需要的
 * series/xaxis/yaxis 数据结构。</p>
 */
@RestController
@RequestMapping("/data")
public class EducationDashboardController extends BaseController {
    private final EducationService educationService;

    public EducationDashboardController(EducationService educationService) {
        this.educationService = educationService;
    }

    /** 原首页图表接口，支持 types=1,2,3,4,5,6,7 和 days=1..31。 */
    @GetMapping("/board")
    public AjaxResult board(@RequestParam Map<String, Object> params) {
        Map<String, Object> statistics = educationService.statistics();
        int days = number(params == null ? null : params.get("days"), 7);
        List<EduDashboardDaily> records = educationService.dashboardDaily(days);
        List<String> dates = records.stream().map(item -> item.getStatDate() == null
                ? "" : item.getStatDate().toString().substring(5)).toList();
        List<Map<String, Object>> series = new ArrayList<>();
        List<Map<String, Object>> xaxis = new ArrayList<>();
        List<Map<String, Object>> yaxis = new ArrayList<>();
        String types = params == null ? null : String.valueOf(params.get("types"));
        List<String> requested = types == null || "null".equalsIgnoreCase(types) || types.isBlank()
                ? List.of("6", "4", "7")
                : Arrays.stream(types.split(",")).map(String::trim).filter(item -> !item.isBlank()).toList();
        for (String type : requested) {
            Metric metric = metric(type, records);
            List<Integer> values = metric.values();
            Map<String, Object> seriesItem = new LinkedHashMap<>();
            seriesItem.put("name", metric.name());
            seriesItem.put("type", metric.chartType());
            seriesItem.put("data", values);
            seriesItem.put("max", values.stream().mapToInt(Integer::intValue).max().orElse(0));
            seriesItem.put("min", values.stream().mapToInt(Integer::intValue).min().orElse(0));
            series.add(seriesItem);

            Map<String, Object> xaxisItem = new LinkedHashMap<>();
            xaxisItem.put("data", dates);
            xaxis.add(xaxisItem);

            int max = Math.max(10, values.stream().mapToInt(Integer::intValue).max().orElse(0));
            Map<String, Object> yaxisItem = new LinkedHashMap<>();
            yaxisItem.put("type", "value");
            yaxisItem.put("min", 0);
            yaxisItem.put("max", max);
            yaxisItem.put("average", values.stream().mapToInt(Integer::intValue).average().orElse(0D));
            yaxisItem.put("interval", Math.max(1, max / 5));
            yaxis.add(yaxisItem);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("series", series);
        result.put("xaxis", xaxis);
        result.put("yaxis", yaxis);
        result.put("statistics", statistics);
        return success(result);
    }

    /** 今日摘要接口，提供旧页面可能使用的平铺字段和 today 对象。 */
    @GetMapping("/today")
    public AjaxResult today() {
        Map<String, Object> statistics = educationService.statistics();
        EduDashboardDaily current = educationService.dashboardToday();
        if (current == null) {
            current = new EduDashboardDaily();
            current.setStatDate(LocalDate.now());
            current.setVisits(0L);
            current.setOrderCount(0L);
            current.setOrderRevenue(BigDecimal.ZERO);
            current.setNewStudents(0L);
            current.setActiveUsers(0L);
            current.setTotalStudents(numberLong(statistics.get("totalStudents")));
        }
        EduDashboardDaily previous = educationService.dashboardPrevious(current);
        Map<String, Object> today = new LinkedHashMap<>();
        today.put("date", LocalDate.now().toString());
        today.put("statDate", current.getStatDate());
        today.put("todayVisits", numberLong(current.getVisits()));
        today.put("todayOrders", numberLong(current.getOrderCount()));
        today.put("todayRevenue", defaultValue(current.getOrderRevenue(), BigDecimal.ZERO));
        today.put("newStudents", numberLong(current.getNewStudents()));
        today.put("activeUsers", numberLong(current.getActiveUsers()));
        today.put("totalStudents", current.getTotalStudents() == null
                ? statistics.getOrDefault("totalStudents", 0L) : current.getTotalStudents());
        today.put("publishedCourses", statistics.getOrDefault("publishedCourses", 0L));
        Map<String, String> changes = new LinkedHashMap<>();
        changes.put("visits", change(current.getVisits(), previous == null ? null : previous.getVisits()));
        changes.put("orders", change(current.getOrderCount(), previous == null ? null : previous.getOrderCount()));
        changes.put("revenue", change(current.getOrderRevenue(), previous == null ? null : previous.getOrderRevenue()));
        changes.put("newStudents", change(current.getNewStudents(), previous == null ? null : previous.getNewStudents()));
        today.put("changes", changes);
        Map<String, Object> result = new LinkedHashMap<>(today);
        result.put("today", today);
        result.put("statistics", statistics);
        return success(result);
    }

    /** 热门课程排行接口，按课程学习人数从高到低返回前十条。 */
    @GetMapping("/top10")
    @SuppressWarnings("unchecked")
    public AjaxResult top10() {
        Map<String, Object> page = educationService.portalCourses(Map.of(
                "pageNo", 1L, "pageSize", 10L, "sortBy", "learners"));
        List<Map<String, Object>> list = page.get("list") instanceof List<?> values
                ? (List<Map<String, Object>>) values : List.of();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("rows", list);
        result.put("courses", list);
        result.put("total", page.getOrDefault("total", list.size()));
        return success(result);
    }

    private Metric metric(String type, List<EduDashboardDaily> records) {
        return switch (type) {
            case "1" -> new Metric("访问量", "bar", records.stream().map(item -> number(item.getVisits())).toList());
            case "2" -> new Metric("订单金额", "bar", records.stream().map(item -> amount(item.getOrderRevenue())).toList());
            case "3" -> new Metric("订单笔数", "line", records.stream().map(item -> number(item.getOrderCount())).toList());
            case "4" -> new Metric("新增学员", "line", records.stream().map(item -> number(item.getNewStudents())).toList());
            case "5" -> new Metric("客单价", "line", records.stream().map(this::averageOrderAmount).toList());
            case "7" -> new Metric("日活跃用户数", "line", records.stream().map(item -> number(item.getActiveUsers())).toList());
            case "6" -> new Metric("学员总数", "line", records.stream().map(item -> number(item.getTotalStudents())).toList());
            default -> new Metric("学员总数", "line", records.stream().map(item -> number(item.getTotalStudents())).toList());
        };
    }

    private int averageOrderAmount(EduDashboardDaily item) {
        long count = numberLong(item.getOrderCount());
        if (count <= 0) return 0;
        return amount(defaultValue(item.getOrderRevenue(), BigDecimal.ZERO)
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
    }

    private int amount(BigDecimal value) {
        if (value == null) return 0;
        try {
            return Math.max(0, Math.min(value.setScale(0, RoundingMode.HALF_UP).intValueExact(), Integer.MAX_VALUE));
        } catch (ArithmeticException ignored) {
            return value.signum() < 0 ? 0 : Integer.MAX_VALUE;
        }
    }

    private int number(Object value) {
        return number(value, 0);
    }

    private int number(Object value, int fallback) {
        if (value instanceof Number number) {
            return Math.max(0, Math.min(number.intValue(), Integer.MAX_VALUE));
        }
        try {
            return value == null ? fallback : Math.max(0, Integer.parseInt(String.valueOf(value)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private long numberLong(Object value) {
        if (value instanceof Number number) return Math.max(0L, number.longValue());
        try {
            return value == null ? 0L : Math.max(0L, Long.parseLong(String.valueOf(value)));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private String change(Number current, Number previous) {
        if (current == null || previous == null) return "--";
        return change(BigDecimal.valueOf(current.doubleValue()), BigDecimal.valueOf(previous.doubleValue()));
    }

    private String change(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null) return "--";
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0 ? "0.0%" : "100.0%";
        }
        return current.subtract(previous).divide(previous, 4, RoundingMode.HALF_UP)
                .movePointRight(2).setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private <T> T defaultValue(T value, T fallback) {
        return value == null ? fallback : value;
    }

    private record Metric(String name, String chartType, List<Integer> values) {
    }
}
