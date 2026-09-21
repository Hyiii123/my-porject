package com.share.education.ai.composite.node;

import com.share.education.ai.composite.state.DebateBlackboardState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

/**
 * 复合架构模式 2：动态路由分发节点 (IntentDispatcherNode)。
 * 负责分析用户输入意向与上下文特征，完成首道意图精准分流，并支持自然语言岗位意向实体提取。
 */
@Component
public class IntentDispatcherNode {

    private static final Logger log = LoggerFactory.getLogger(IntentDispatcherNode.class);

    public static final String ROUTE_ACTION_CARD = "ROUTE_ACTION_CARD";
    public static final String ROUTE_MOCK_INTERVIEW = "ROUTE_MOCK_INTERVIEW";
    public static final String ROUTE_MULTI_AGENT_DEBATE = "ROUTE_MULTI_AGENT_DEBATE";

    private static final List<String> INTERVIEW_KEYWORDS = List.of(
        "模拟面试", "面试考场", "全真面试", "技术面", "hr面", "八股文", "面试题", "代码考核", "mock interview"
    );

    private static final List<String> ACTION_KEYWORDS = List.of(
        "退款", "退课", "开票", "发票", "我的订单", "购物车", "打卡", "签到", "积分天梯", "优惠券", "领券", "查学分", "排行榜"
    );

    private static final Map<String, String> ROLE_INTENT_MAPPINGS = Map.ofEntries(
        entry("大模型", "大语言模型应用工程师"),
        entry("llm", "大语言模型应用工程师"),
        entry("ai", "大语言模型应用工程师"),
        entry("大数据", "大数据开发工程师"),
        entry("flink", "大数据开发工程师"),
        entry("spark", "大数据开发工程师"),
        entry("go", "Go云原生架构师"),
        entry("golang", "Go云原生架构师"),
        entry("k8s", "Go云原生架构师"),
        entry("前端", "前端技术专家"),
        entry("vue", "前端技术专家"),
        entry("react", "前端技术专家"),
        entry("鸿蒙", "移动端跨平台工程师"),
        entry("flutter", "移动端跨平台工程师")
    );

    /**
     * 判定当前请求流向并提取意向实体
     */
    public String dispatch(String content, DebateBlackboardState state) {
        if (!StringUtils.hasText(content)) {
            return ROUTE_MULTI_AGENT_DEBATE;
        }
        String lower = content.toLowerCase().trim();

        // 1. 模拟面试意图识别
        for (String kw : INTERVIEW_KEYWORDS) {
            if (lower.contains(kw)) {
                log.info("[IntentDispatcherNode] 命中模拟面试意图: kw={}, 路由至全真考场智能体", kw);
                return ROUTE_MOCK_INTERVIEW;
            }
        }

        // 2. 事务性卡片意图识别 (订单、退款、加购、打卡、优惠券等)
        for (String kw : ACTION_KEYWORDS) {
            if (lower.contains(kw)) {
                log.info("[IntentDispatcherNode] 命中标准事务卡片意图: kw={}, 路由至客服动作工厂", kw);
                return ROUTE_ACTION_CARD;
            }
        }

        // 3. 岗位意向实体动态提取 (若当前角色为默认或空，则自适应提取并设置)
        if (state.getIntendedRole() == null || "全栈开发工程师".equals(state.getIntendedRole()) || "Java全栈架构师".equals(state.getIntendedRole())) {
            for (Map.Entry<String, String> entry : ROLE_INTENT_MAPPINGS.entrySet()) {
                if (lower.contains(entry.getKey())) {
                    state.setIntendedRole(entry.getValue());
                    log.info("[IntentDispatcherNode] 从输入意图自动提取目标岗位: kw={}, 岗位锁定={}", entry.getKey(), entry.getValue());
                    break;
                }
            }
        }

        // 3.5 难度意向实体动态提取
        if (state.getCustomOverrides() != null && !state.getCustomOverrides().containsKey("preferredDifficulty")) {
            if (lower.contains("零基础") || lower.contains("小白") || lower.contains("初学") || lower.contains("入门")) {
                state.getCustomOverrides().put("preferredDifficulty", 1);
                state.getCustomOverrides().put("difficulty", 1);
                log.info("[IntentDispatcherNode] 从输入意图识别初级难度偏好 (Level 1)");
            } else if (lower.contains("高并发") || lower.contains("架构师") || lower.contains("专家") || lower.contains("底层源码")) {
                state.getCustomOverrides().put("preferredDifficulty", 3);
                state.getCustomOverrides().put("difficulty", 3);
                log.info("[IntentDispatcherNode] 从输入意图识别高阶架构难度偏好 (Level 3)");
            }
        }

        // 4. 成长路线与导学规划意图 (激活圆桌博弈子图)
        log.info("[IntentDispatcherNode] 命中导学规划意图，激活多智能体圆桌博弈子图: {}", state.getIntendedRole());
        return ROUTE_MULTI_AGENT_DEBATE;
    }
}

