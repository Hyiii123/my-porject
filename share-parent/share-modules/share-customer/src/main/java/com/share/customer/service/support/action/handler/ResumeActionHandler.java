package com.share.customer.service.support.action.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.customer.domain.interview.vo.ResumeAnalysisVO;
import com.share.customer.service.IUserResumeService;
import com.share.customer.service.support.action.CustomerActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 简历深度诊断与能力雷达动作处理器
 */
@Slf4j
@Component
public class ResumeActionHandler implements CustomerActionHandler {

    private final IUserResumeService userResumeService;
    private final ObjectMapper objectMapper;

    public ResumeActionHandler(@Lazy IUserResumeService userResumeService, ObjectMapper objectMapper) {
        this.userResumeService = userResumeService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String content, String lower) {
        return lower.contains("诊断简历") || lower.contains("我的简历") || lower.contains("分析简历")
                || lower.contains("看看我的简历") || lower.contains("简历诊断") || lower.contains("简历报告");
    }

    @Override
    public String handle(String content, String lower, Long userId, String userName) {
        try {
            ResumeAnalysisVO resume = userResumeService.getMyResume();
            Map<String, Object> card = new HashMap<>();
            StringBuilder sb = new StringBuilder();

            if (resume != null && resume.getId() != null) {
                card.put("action", "resume_diagnose");
                card.put("resumeId", String.valueOf(resume.getId()));
                card.put("fileName", resume.getFileName());
                card.put("matchScore", resume.getMatchScore() != null ? resume.getMatchScore() : 80);
                card.put("targetJob", resume.getTargetJob() != null ? resume.getTargetJob() : "技术开发工程师");
                String cardJson = objectMapper.writeValueAsString(card);

                sb.append("📄 **AI 简历深度诊断与能力雷达**\n\n");
                sb.append("已为您调取当前个人中心关联的简历档案《").append(resume.getFileName() != null ? resume.getFileName() : "我的简历").append("》：\n");
                sb.append("• **对标岗位**：").append(resume.getTargetJob() != null ? resume.getTargetJob() : "技术开发工程师").append("\n");
                sb.append("• **综合契合度评分**：").append(resume.getMatchScore() != null ? resume.getMatchScore() : 80).append(" 分\n");
                if (resume.getProjectHighlights() != null && !resume.getProjectHighlights().isEmpty()) {
                    sb.append("• **核心高光亮点**：").append(String.join("、", resume.getProjectHighlights())).append("\n");
                }
                sb.append("\n点击下方卡片即可查看完整能力六维雷达诊断，或直接针对薄弱项发起模拟面试连环追问：\n\n");
                sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            } else {
                card.put("action", "resume_upload");
                String cardJson = objectMapper.writeValueAsString(card);

                sb.append("📄 **AI 简历深度诊断与职涯对标**\n\n");
                sb.append("检测到您当前尚未在系统中上传求职简历。上传简历后，AI 将基于真实项目与技术标签进行深度特征抽取、契合度量化与高并发/分布式实战考题连环追问。\n\n");
                sb.append("点击下方卡片即可前往个人中心一键上传简历：\n\n");
                sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            }
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成简历诊断卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
