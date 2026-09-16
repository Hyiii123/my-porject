package com.share.customer.service.support.action.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.customer.service.support.action.CustomerActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 课表与继续学习动作处理器
 */
@Slf4j
@Component
public class ContinueLearningActionHandler implements CustomerActionHandler {

    private final ObjectMapper objectMapper;

    public ContinueLearningActionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String content, String lower) {
        return lower.contains("学到哪了") || lower.contains("继续学习") || lower.contains("我的进度")
                || lower.contains("课表") || lower.contains("我的课表") || lower.contains("继续上课");
    }

    @Override
    public String handle(String content, String lower, Long userId, String userName) {
        try {
            Map<String, Object> card = new HashMap<>();
            card.put("action", "continue_learning");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("📚 **学伴学习进度接力**\n\n");
            sb.append("已为您同步个人课表与学习进度。保持持续学习与代码实践是快速蜕变为架构师的核心捷径！\n\n");
            sb.append("点击下方操作卡片即可直达我的课表或探索更多精品好课：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成学习进度卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public int getOrder() {
        return 130;
    }
}
