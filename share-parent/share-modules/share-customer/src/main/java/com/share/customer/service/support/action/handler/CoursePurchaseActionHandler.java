package com.share.customer.service.support.action.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.common.core.web.domain.AjaxResult;
import com.share.customer.service.support.action.CustomerActionHandler;
import com.share.education.api.RemoteEducationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程购买与加购动作处理器
 */
@Slf4j
@Component
public class CoursePurchaseActionHandler implements CustomerActionHandler {

    private final RemoteEducationService remoteEducationService;
    private final ObjectMapper objectMapper;

    public CoursePurchaseActionHandler(RemoteEducationService remoteEducationService, ObjectMapper objectMapper) {
        this.remoteEducationService = remoteEducationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String content, String lower) {
        if (lower.contains("退款") || lower.contains("退课") || lower.contains("发票") || lower.contains("开票") || lower.contains("密码")) {
            return false;
        }
        // 如果仅为课程推荐/咨询求助，严禁误判为购课卡片动作，应交由导学多智能体推演或大模型处理
        if (lower.contains("推荐") && !(lower.contains("买") || lower.contains("购") || lower.contains("下单") || lower.contains("加购") || lower.contains("加购物车") || lower.contains("结算"))) {
            return false;
        }
        return lower.contains("买课") || lower.contains("买课程") || lower.contains("购课")
                || lower.contains("我要买") || lower.contains("想买") || lower.contains("购买")
                || lower.contains("加购") || lower.contains("加购物车") || lower.contains("加入购物车")
                || lower.contains("报名") || lower.contains("选购课程")
                || (lower.contains("课程") && (lower.contains("买") || lower.contains("购") || lower.contains("加") || lower.contains("下单")));
    }

    private String extractCourseKeyword(String content) {
        if (!StringUtils.hasText(content)) return "";
        String lower = content.toLowerCase();
        if (lower.contains("微服务") || lower.contains("springcloud") || lower.contains("cloud")) return "微服务";
        if (lower.contains("springboot") || lower.contains("boot")) return "SpringBoot";
        if (lower.contains("node") || lower.contains("nodejs")) return "Node";
        if (lower.contains("vue") || lower.contains("前端") || lower.contains("react")) return "前端";
        if (lower.contains("python") || lower.contains("爬虫") || lower.contains("机器学习")) return "Python";
        if (lower.contains("go") || lower.contains("golang")) return "Go";
        if (lower.contains("docker") || lower.contains("k8s") || lower.contains("云原生")) return "云原生";
        if (lower.contains("mysql") || lower.contains("数据库") || lower.contains("sql")) return "MySQL";
        if (lower.contains("redis") || lower.contains("缓存")) return "Redis";
        if (lower.contains("大模型") || lower.contains("ai") || lower.contains("llm")) return "大模型";
        if (lower.contains("算法") || lower.contains("数据结构")) return "算法";
        if (lower.contains("java")) return "Java";
        return "";
    }

    @Override
    @SuppressWarnings("unchecked")
    public String handle(String content, String lower, Long userId, String userName) {
        if (!supports(content, lower)) {
            return null;
        }
        String keyword = extractCourseKeyword(content);
        try {
            AjaxResult res = remoteEducationService.searchCourses(keyword, 5);
            if (res != null && res.isSuccess() && res.get("data") != null) {
                Map<String, Object> dataMap = (Map<String, Object>) res.get("data");
                Object listObj = dataMap.get("list");
                if (listObj instanceof List) {
                    List<?> list = (List<?>) listObj;
                    if (!list.isEmpty() && list.get(0) instanceof Map) {
                        Map<String, Object> course = (Map<String, Object>) list.get(0);
                        String courseId = String.valueOf(course.get("id"));
                        String title = course.get("title") != null ? course.get("title").toString() : String.valueOf(course.get("courseName"));
                        String cover = course.get("cover") != null ? course.get("cover").toString() : "/src/assets/images/courses/default-cover.svg";
                        Number priceNum = course.get("price") instanceof Number ? (Number) course.get("price") : 0;
                        Number originalPriceNum = course.get("originalPrice") instanceof Number ? (Number) course.get("originalPrice") : priceNum;
                        String teacherName = course.get("teacherName") != null ? course.get("teacherName").toString() : "资深讲师团队";
                        Object lessons = course.getOrDefault("lessons", 0);
                        Object isFree = course.getOrDefault("isFree", 0);
                        String desc = course.get("shortDescription") != null ? course.get("shortDescription").toString() : "";

                        Map<String, Object> card = new HashMap<>();
                        card.put("action", "course_purchase");
                        card.put("courseId", courseId);
                        card.put("title", title);
                        card.put("cover", cover);
                        card.put("price", priceNum);
                        card.put("originalPrice", originalPriceNum);
                        card.put("teacherName", teacherName);
                        card.put("lessons", lessons);
                        card.put("isFree", isFree);
                        String cardJson = objectMapper.writeValueAsString(card);

                        BigDecimal priceYuan = new BigDecimal(priceNum.toString()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                        StringBuilder sb = new StringBuilder();
                        sb.append("🛒 **已为您精准匹配目标课程！**\n\n");
                        sb.append("已为您调取最受学员好评的精品好课《").append(title).append("》：\n");
                        if (StringUtils.hasText(desc)) {
                            sb.append("• **课程介绍**：").append(desc).append("\n");
                        }
                        sb.append("• **主讲名师**：").append(teacherName).append(" ｜ **总课时**：").append(lessons).append(" 讲\n");
                        sb.append("• **课程价格**：");
                        if ("1".equals(String.valueOf(isFree)) || priceYuan.compareTo(BigDecimal.ZERO) == 0) {
                            sb.append("【限时免费】\n");
                        } else {
                            sb.append("¥").append(priceYuan).append(" 元\n");
                        }
                        sb.append("\n我已为您生成专属购课操作卡片，您可以直接点击【加入购物车】或【立即结算】一键发起购买：\n\n");
                        sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
                        return sb.toString();
                    }
                }
            }
        } catch (Exception ex) {
            log.error("智能体搜索并生成购课卡片异常: {}", ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return 60;
    }
}
