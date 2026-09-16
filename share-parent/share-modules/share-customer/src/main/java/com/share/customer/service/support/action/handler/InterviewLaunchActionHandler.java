package com.share.customer.service.support.action.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.customer.domain.interview.InterviewSession;
import com.share.customer.domain.interview.dto.StartInterviewRequest;
import com.share.customer.service.IInterviewService;
import com.share.customer.service.support.action.CustomerActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 模拟面试开考与考场创建动作处理器
 */
@Slf4j
@Component
public class InterviewLaunchActionHandler implements CustomerActionHandler {

    private final IInterviewService interviewService;
    private final ObjectMapper objectMapper;

    public InterviewLaunchActionHandler(@Lazy IInterviewService interviewService, ObjectMapper objectMapper) {
        this.interviewService = interviewService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String content, String lower) {
        return lower.contains("模拟面试") || lower.contains("我要面试") || lower.contains("开一场面试")
                || lower.contains("帮我开一场面试") || lower.contains("面试考场") || lower.contains("开始面试")
                || lower.contains("想面试") || lower.contains("阿里面试") || lower.contains("java面试")
                || lower.contains("高并发面试") || (lower.contains("面试") && (lower.contains("开") || lower.contains("来一场") || lower.contains("开始")));
    }

    @Override
    public String handle(String content, String lower, Long userId, String userName) {
        String targetJob = "Java高级开发工程师";
        if (lower.contains("大模型") || lower.contains("llm") || lower.contains("ai")) {
            targetJob = "大语言模型应用工程师";
        } else if (lower.contains("go") || lower.contains("golang") || lower.contains("云原生")) {
            targetJob = "Go云原生架构师";
        } else if (lower.contains("前端") || lower.contains("vue") || lower.contains("react")) {
            targetJob = "Web前端架构专家";
        } else if (lower.contains("大数据") || lower.contains("spark") || lower.contains("flink")) {
            targetJob = "大数据流批一体工程师";
        }

        String company = "大厂通用";
        if (lower.contains("阿里")) company = "阿里巴巴";
        else if (lower.contains("字节")) company = "字节跳动";
        else if (lower.contains("腾讯")) company = "腾讯科技";
        else if (lower.contains("美团")) company = "美团";
        else if (lower.contains("百度")) company = "百度";

        try {
            StartInterviewRequest req = new StartInterviewRequest();
            req.setTargetJob(targetJob);
            req.setCompanyTarget(company);
            req.setInterviewerStyle("p7_architect");
            req.setTotalTurns(20);
            InterviewSession newSession = interviewService.startSession(req);
            Long sId = newSession.getId();

            Map<String, Object> card = new HashMap<>();
            card.put("action", "interview_launch");
            card.put("sessionId", String.valueOf(sId));
            card.put("targetJob", targetJob);
            card.put("company", company);
            card.put("interviewerStyle", "p7_architect");
            card.put("roundInfo", "全真三环节 20 题 60 分钟限时架构");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("🎯 **全真 AI 模拟面试考场已为您极速就绪！**\n\n");
            sb.append("已为您成功开辟面向【").append(company).append("】的【").append(targetJob).append("】全真考核专场：\n");
            sb.append("• **考核架构**：全真三环节 20 题 60 分钟限时考核（环节一：破题自我介绍 ➔ 环节二：小林coding 10大独立模块八股文 ➔ 环节三：真实项目上下文深度深挖连环追问）；\n");
            sb.append("• **考官引擎**：微软晓晓 (Xiaoxiao Neural TTS) 真实级语音朗读 + 数字人口型毫秒级音画协同；\n");
            sb.append("• **考场场次**：#").append(sId).append("，第一题考题已生成完毕。\n\n");
            sb.append("点击下方考场卡片，即可立即入场进入沉浸式考场开考：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("智能体自动开辟面试考场失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
