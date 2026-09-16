package com.share.customer.service.support.action.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.customer.service.support.action.CustomerActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 每日打卡签到动作处理器
 */
@Slf4j
@Component
public class SignInActionHandler implements CustomerActionHandler {

    private final ObjectMapper objectMapper;

    public SignInActionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String content, String lower) {
        return lower.contains("签到") || lower.contains("打卡") || lower.contains("今日打卡")
                || lower.contains("领积分") || lower.contains("每日签到");
    }

    @Override
    public String handle(String content, String lower, Long userId, String userName) {
        try {
            Map<String, Object> card = new HashMap<>();
            card.put("action", "sign_in");
            card.put("dailyPoints", 10);
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("✨ **每日学情打卡与积分中心**\n\n");
            sb.append("坚持每日签到打卡，每天可固定获得 **+10 学分** 奖励，连续打卡更有惊喜加成！积分可在购买课程时直接抵扣现金或兑换专属大额优惠券。\n\n");
            sb.append("点击下方卡片中的【一键打卡签到】即可立即完成打卡：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成签到卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public int getOrder() {
        return 70;
    }
}
