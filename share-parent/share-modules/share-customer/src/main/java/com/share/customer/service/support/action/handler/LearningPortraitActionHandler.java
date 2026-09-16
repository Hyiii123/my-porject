package com.share.customer.service.support.action.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.customer.service.support.action.CustomerActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 学员动态学习画像与技能雷达动作处理器
 */
@Slf4j
@Component
public class LearningPortraitActionHandler implements CustomerActionHandler {

    private final ObjectMapper objectMapper;

    public LearningPortraitActionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String content, String lower) {
        return lower.contains("学习画像") || lower.contains("技能图谱") || lower.contains("我的画像")
                || lower.contains("能力雷达") || lower.contains("技能雷达") || lower.contains("能力画像");
    }

    @Override
    public String handle(String content, String lower, Long userId, String userName) {
        try {
            Map<String, Object> card = new HashMap<>();
            card.put("action", "learning_portrait");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("📊 **学员多维动态学习画像与能力雷达**\n\n");
            sb.append("• **动态构建**：基于您当前账户（ID: ").append(userId).append("）在平台完成的课程进度、模拟面试打分与随堂测试表现实时推演。\n\n");
            sb.append("点击下方卡片即可直达个人中心查看学员能力六维雷达与技能图谱：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成画像卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public int getOrder() {
        return 120;
    }
}
