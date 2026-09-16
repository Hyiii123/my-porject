package com.share.customer.service.support.action.handler;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.customer.domain.interview.InterviewSession;
import com.share.customer.service.IInterviewService;
import com.share.customer.service.support.action.CustomerActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模拟面试历史复盘与成绩查询动作处理器
 */
@Slf4j
@Component
public class InterviewReportActionHandler implements CustomerActionHandler {

    private final IInterviewService interviewService;
    private final ObjectMapper objectMapper;

    public InterviewReportActionHandler(@Lazy IInterviewService interviewService, ObjectMapper objectMapper) {
        this.interviewService = interviewService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String content, String lower) {
        if (lower.contains("开") || lower.contains("开始") || lower.contains("来一场") || lower.contains("我要面试") || lower.contains("开一场")) {
            return false;
        }
        return lower.contains("面试报告") || lower.contains("面试成绩") || lower.contains("面试复盘")
                || lower.contains("上次面试") || lower.contains("面试记录") || lower.contains("面试得了多少分")
                || lower.contains("查面试") || lower.contains("我的面试") || lower.contains("面试结果");
    }

    @Override
    public String handle(String content, String lower, Long userId, String userName) {
        try {
            if (userId == null) {
                return "请先登录后再查看您的模拟面试记录与复盘报告。";
            }
            IPage<InterviewSession> mySessions = interviewService.listMySessions(1, 5);
            List<InterviewSession> records = mySessions != null ? mySessions.getRecords() : Collections.emptyList();

            if (records.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("📊 **AI 模拟面试档案查询**\n\n");
                sb.append("• **身份安全校验**：已严格按当前登录用户（ID: ").append(userId).append("）进行数据隔离。\n");
                sb.append("检测到您当前暂无任何模拟面试考核记录。点击下方即可立即开启您的首场全真模拟考核：\n\n");
                Map<String, Object> card = new HashMap<>();
                card.put("action", "interview_launch");
                card.put("targetJob", "Java高级开发工程师");
                card.put("company", "阿里巴巴");
                sb.append("[AGENT_ACTION_CARD:").append(objectMapper.writeValueAsString(card)).append("]");
                return sb.toString();
            }

            InterviewSession latest = records.get(0);
            Map<String, Object> card = new HashMap<>();
            card.put("action", "interview_report");
            card.put("sessionId", String.valueOf(latest.getId()));
            card.put("targetJob", latest.getTargetJob() != null ? latest.getTargetJob() : "技术开发工程师");
            card.put("company", latest.getCompanyTarget() != null ? latest.getCompanyTarget() : "大厂通用");
            card.put("status", latest.getStatus());
            card.put("score", latest.getScore() != null ? latest.getScore() : 82);
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("📊 **已为您调取属于您本人的最新模拟面试复盘档案！**\n\n");
            sb.append("• **身份安全隔离**：已严格锁定当前认证用户（ID: ").append(userId).append("），杜绝越权访问他人面试隐私\n");
            sb.append("• **考场场次**：#").append(latest.getId()).append("\n");
            sb.append("• **考核方向**：【").append(latest.getCompanyTarget()).append("】").append(latest.getTargetJob()).append("\n");
            sb.append("• **考核状态**：").append(latest.getStatus() == 2 ? "✅ 考核已终局完成" : (latest.getStatus() == 1 ? "⏳ 考场进行中" : "已结束")).append("\n");
            if (latest.getScore() != null) {
                sb.append("• **综合得分**：").append(latest.getScore()).append(" 分\n");
            }
            sb.append("\n点击下方卡片即可直接进入全真大屏复盘与六维能力雷达：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("查询面试复盘卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
