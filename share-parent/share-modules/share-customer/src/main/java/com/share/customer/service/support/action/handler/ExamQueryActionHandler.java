package com.share.customer.service.support.action.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.customer.service.support.action.CustomerActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 学情考试与测验考核动作处理器
 */
@Slf4j
@Component
public class ExamQueryActionHandler implements CustomerActionHandler {

    private final ObjectMapper objectMapper;

    public ExamQueryActionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String content, String lower) {
        return lower.contains("考试") || lower.contains("测验") || lower.contains("查分")
                || lower.contains("考试成绩") || lower.contains("考试记录") || lower.contains("错题") || lower.contains("答卷");
    }

    @Override
    public String handle(String content, String lower, Long userId, String userName) {
        try {
            Map<String, Object> card = new HashMap<>();
            card.put("action", "exam_query");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("📝 **学情考试与测验考核中心**\n\n");
            sb.append("• **专属成绩档案**：考试记录与错题本严格绑定当前学员（ID: ").append(userId).append("），真实记录期末测试、随堂小测与技术实训成绩。\n\n");
            sb.append("点击下方卡片即可查看您的考试记录、成绩单与错题解析：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成考试中心卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public int getOrder() {
        return 90;
    }
}
