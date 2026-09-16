package com.share.customer.service.support.action.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.customer.service.support.action.CustomerActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 赛季学霸天梯榜与积分资产动作处理器
 */
@Slf4j
@Component
public class PointsRankingActionHandler implements CustomerActionHandler {

    private final ObjectMapper objectMapper;

    public PointsRankingActionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String content, String lower) {
        if (lower.contains("微积分") || lower.contains("定积分") || lower.contains("不定积分")
                || lower.contains("重积分") || lower.contains("积分方程") || lower.contains("蒙特卡洛")) {
            return false;
        }
        return lower.contains("学霸") || lower.contains("天梯") || lower.contains("排行榜")
                || lower.contains("学霸榜") || lower.contains("天梯榜") || lower.contains("积分榜")
                || lower.contains("积分排行") || lower.contains("积分明细") || lower.contains("我的积分")
                || lower.contains("多少积分") || lower.contains("积分中心") || lower.contains("查积分")
                || lower.contains("学分") || lower.contains("领积分");
    }

    @Override
    public String handle(String content, String lower, Long userId, String userName) {
        try {
            Map<String, Object> card = new HashMap<>();
            card.put("action", "points_ranking");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("🏆 **赛季学霸天梯榜与积分资产**\n\n");
            sb.append("• **用户资产核验**：当前登录用户（ID: ").append(userId).append("）的学分资产已就绪。\n");
            sb.append("• **排行榜规则**：每赛季根据日常答题、签到与考场表现更新学霸天梯段位。\n\n");
            sb.append("点击下方卡片即可查看赛季天梯排名或我的积分明细：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成积分榜卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public int getOrder() {
        return 80;
    }
}
