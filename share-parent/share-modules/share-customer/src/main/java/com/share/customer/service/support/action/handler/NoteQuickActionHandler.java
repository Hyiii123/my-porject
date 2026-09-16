package com.share.customer.service.support.action.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.customer.service.support.action.CustomerActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 随堂速记与个人笔记动作处理器
 */
@Slf4j
@Component
public class NoteQuickActionHandler implements CustomerActionHandler {

    private final ObjectMapper objectMapper;

    public NoteQuickActionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String content, String lower) {
        return lower.contains("记笔记") || lower.contains("做笔记") || lower.contains("我的笔记")
                || lower.contains("记一条笔记") || lower.contains("帮我记笔记") || lower.contains("写笔记") || lower.contains("查看笔记");
    }

    @Override
    public String handle(String content, String lower, Long userId, String userName) {
        try {
            String noteText = "";
            if (content.contains("：")) {
                noteText = content.substring(content.indexOf("：") + 1).trim();
            } else if (content.contains(":")) {
                noteText = content.substring(content.indexOf(":") + 1).trim();
            } else {
                for (String prefix : new String[]{"记一条笔记", "帮我记笔记", "做笔记", "记笔记", "写笔记"}) {
                    if (content.contains(prefix)) {
                        noteText = content.substring(content.indexOf(prefix) + prefix.length()).trim();
                        break;
                    }
                }
            }

            Map<String, Object> card = new HashMap<>();
            card.put("action", "note_quick");
            card.put("noteContent", noteText);
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("📒 **随堂速记与个人技术知识库**\n\n");
            sb.append("• **私密知识空间**：笔记内容已开启个人强隔离（归属用户 ID: ").append(userId).append("），沉淀专属技术心得。\n");
            if (StringUtils.hasText(noteText)) {
                sb.append("• **识别笔记内容**：").append(noteText).append("\n");
            }
            sb.append("\n点击下方卡片即可快速将要点存入您的笔记库，或查看历史笔记：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成笔记卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public int getOrder() {
        return 110;
    }
}
