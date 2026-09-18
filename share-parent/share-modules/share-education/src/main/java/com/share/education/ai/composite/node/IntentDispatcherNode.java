package com.share.education.ai.composite.node;

import com.share.education.ai.composite.state.DebateBlackboardState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 复合架构模式 2：动态路由分发节点 (IntentDispatcherNode)。
 * 负责分析用户输入意向与上下文特征，完成首道意图精准分流，决定是否激活圆桌博弈子图。
 */
@Component
public class IntentDispatcherNode {

    private static final Logger log = LoggerFactory.getLogger(IntentDispatcherNode.class);

    public static final String ROUTE_ACTION_CARD = "ROUTE_ACTION_CARD";
    public static final String ROUTE_MOCK_INTERVIEW = "ROUTE_MOCK_INTERVIEW";
    public static final String ROUTE_MULTI_AGENT_DEBATE = "ROUTE_MULTI_AGENT_DEBATE";

    /**
     * 判定当前请求流向
     */
    public String dispatch(String content, DebateBlackboardState state) {
        if (!StringUtils.hasText(content)) {
            return ROUTE_MULTI_AGENT_DEBATE;
        }
        String lower = content.toLowerCase().trim();

        // 1. 模拟面试意图
        if (lower.contains("模拟面试") || lower.contains("面试考场") || lower.contains("全真面试")) {
            log.info("[IntentDispatcherNode] 命中模拟面试意图，路由至全真考场智能体");
            return ROUTE_MOCK_INTERVIEW;
        }

        // 2. 事务性卡片意图 (订单、退款、加购、打卡、优惠券等)
        if (lower.contains("退款") || lower.contains("退课") || lower.contains("开票")
            || lower.contains("发票") || lower.contains("我的订单") || lower.contains("购物车")
            || lower.contains("打卡") || lower.contains("签到") || lower.contains("积分天梯")) {
            log.info("[IntentDispatcherNode] 命中标准事务卡片意图，路由至客服动作工厂");
            return ROUTE_ACTION_CARD;
        }

        // 3. 成长路线与导学规划意图 (激活圆桌博弈子图)
        log.info("[IntentDispatcherNode] 命中导学规划意图，激活多智能体圆桌博弈子图: {}", state.getIntendedRole());
        return ROUTE_MULTI_AGENT_DEBATE;
    }
}
