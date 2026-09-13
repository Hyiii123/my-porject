package com.share.education.ai.agent;

import com.share.education.ai.client.DashScopeAiClient;
import com.share.education.ai.model.*;
import com.share.education.ai.rag.EducationKnowledgeRAG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 智能体 5：解释生成 Agent (ExplanationGenerationAgent)。
 *
 * <p>【Spring AI 框架驱动】：
 * 1. 结合路径规划成果与 RAG 行业胜任力证据切片，基于 Spring AI {@link PromptTemplate} 与 {@link SystemPromptTemplate} 构建结构化 Prompt；
 * 2. 委托官方 Spring AI 客户端生成“可解释性推荐理由 (Explainable AI)”，告别黑盒；
 * 3. 产出最终给前端渲染展示的个性化推荐卡片与路线全景视图。</p>
 */
@Component
public class ExplanationGenerationAgent {

    private static final Logger log = LoggerFactory.getLogger(ExplanationGenerationAgent.class);

    private final DashScopeAiClient aiClient;
    private final EducationKnowledgeRAG knowledgeRAG;

    public ExplanationGenerationAgent(DashScopeAiClient aiClient, EducationKnowledgeRAG knowledgeRAG) {
        this.aiClient = aiClient;
        this.knowledgeRAG = knowledgeRAG;
    }

    /**
     * 生成最终带有丰富可解释性属性的个性化推荐列表
     */
    public List<PersonalizedRecommendVO> generateExplanations(UserProfileContext profile,
                                                              LearningPathPlan pathPlan,
                                                              int limit) {
        if (pathPlan == null || pathPlan.getStages() == null || pathPlan.getStages().isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 检索 RAG 行业知识基线
        String benchmarkEvidence = knowledgeRAG.retrieveCareerCompetencyBenchmark(
            profile.getIntendedRole(), profile.getTopSkills()
        );

        List<PersonalizedRecommendVO> result = new ArrayList<>();

        for (PathStageVO stage : pathPlan.getStages()) {
            for (AnalyzedCourseVO ac : stage.getCourses()) {
                // 2. 生成可解释性理由 (Spring AI 优先 + 本地图谱规则降级)
                String reason = generateCourseReason(ac, profile, stage, benchmarkEvidence);
                String skillGapFilled = (ac.getCoreKnowledgePoints() != null && !ac.getCoreKnowledgePoints().isEmpty())
                    ? String.join("、", ac.getCoreKnowledgePoints())
                    : "核心技术栈专项提升";

                int matchScore = (int) Math.round(ac.getMatchScore() != null ? ac.getMatchScore() : 92.0);
                String matchTag = StringUtils.hasText(ac.getMatchTag()) ? ac.getMatchTag() : buildMatchTag(stage, matchScore);

                result.add(PersonalizedRecommendVO.builder()
                    .id(ac.getCourseId())
                    .title(ac.getCourseName())
                    .cover(ac.getCoverUrl())
                    .coverUrl(ac.getCoverUrl())
                    .price(ac.getPrice())
                    .originalPrice(ac.getOriginalPrice())
                    .teacherName(ac.getTeacherName())
                    .difficulty(ac.getDifficultyLevel())
                    .learners(ac.getLearnerCount())
                    .matchScore(matchScore)
                    .matchTag(matchTag)
                    .recommendReason(reason)
                    .learningStage(stage.getStageName())
                    .skillGapFilled(skillGapFilled)
                    .prerequisiteSkills(ac.getPrerequisiteSkills())
                    .evidencePaths(ac.getEvidencePaths() != null ? ac.getEvidencePaths() : Collections.emptyList())
                    .build());

                if (result.size() >= limit) {
                    break;
                }
            }
            if (result.size() >= limit) {
                break;
            }
        }

        return result;
    }

    private String generateCourseReason(AnalyzedCourseVO ac,
                                        UserProfileContext profile,
                                        PathStageVO stage,
                                        String benchmarkEvidence) {
        // 若百炼大模型客户端处于就绪可用状态，则尝试让大模型润色微调个性化推荐理由
        if (aiClient.isAvailable()) {
            try {
                SystemPromptTemplate systemTemplate = new SystemPromptTemplate(
                    "你是一名资深 IT 职业教育规划专家。请针对学员目标岗位、当前技能画像、知识图谱推导先修链路和推荐课程，"
                    + "用一句温暖、专业、富有严密逻辑性的话（45字以内）阐述【为什么推荐这门课程以及学完对前沿技术突破的帮助】。直接输出一句话，不带格式。"
                );

                String pathEvidence = (ac.getEvidencePaths() != null && !ac.getEvidencePaths().isEmpty())
                    ? "；知识图谱推导先修链路：" + String.join("，", ac.getEvidencePaths()) : "";

                PromptTemplate userTemplate = new PromptTemplate(
                    "学员目标岗位：{intendedRole}；已有技能：{skills}；当前阶段：{stageName}；推荐课程：《{courseName}》；核心知识点：{knowledgePoints}{pathEvidence}；行业背景参考：{benchmark}"
                );

                Map<String, Object> userModel = new HashMap<>();
                userModel.put("intendedRole", StringUtils.hasText(profile.getIntendedRole()) ? profile.getIntendedRole() : "软件工程师");
                userModel.put("skills", profile.getTopSkills() != null && !profile.getTopSkills().isEmpty() ? String.join(",", profile.getTopSkills()) : "计算机基础");
                userModel.put("stageName", stage.getStageName());
                userModel.put("courseName", ac.getCourseName());
                userModel.put("knowledgePoints", ac.getCoreKnowledgePoints() != null ? String.join(",", ac.getCoreKnowledgePoints()) : "核心技术");
                userModel.put("pathEvidence", pathEvidence);
                userModel.put("benchmark", StringUtils.hasText(benchmarkEvidence) ? benchmarkEvidence : "行业通用标准");

                String systemPrompt = systemTemplate.render();
                String userPrompt = userTemplate.render(userModel);

                String aiReason = aiClient.generate(systemPrompt, userPrompt);
                if (StringUtils.hasText(aiReason)) {
                    return aiReason.replaceAll("[“”\"]", "").trim();
                }
            } catch (Exception ex) {
                log.warn("[Spring AI] 大模型生成推荐理由异常，平滑降级为规则引擎: {}", ex.getMessage());
            }
        }

        // 本地轻量可解释性推理引擎 (Dual-Mode Fallback)
        return buildRuleBasedReason(ac, profile, stage);
    }

    private String buildRuleBasedReason(AnalyzedCourseVO ac, UserProfileContext profile, PathStageVO stage) {
        String courseName = ac.getCourseName();
        String role = StringUtils.hasText(profile.getIntendedRole()) ? profile.getIntendedRole() : "技术工程师";
        List<String> topSkills = profile.getTopSkills();
        String mainSkill = (!topSkills.isEmpty()) ? topSkills.get(0) : "现有技术";

        // 优先采纳 DRAG-KP4SR 算法推演出的显式先修知识路径作为解释锚点
        if (ac.getEvidencePaths() != null && !ac.getEvidencePaths().isEmpty()) {
            String firstPath = ac.getEvidencePaths().get(0);
            return String.format("前沿知识攻坚：基于先修拓扑链路（%s），助力平滑跃升攻克 %s 核心难点。", firstPath, courseName);
        }

        int stageIndex = stage.getStageIndex() != null ? stage.getStageIndex() : 1;
        if (stageIndex == 1) {
            return String.format("筑基先修保障：巩固《%s》核心概念，为深入掌握 %s 筑牢底层代码设计与架构底座。", courseName, role);
        } else if (stageIndex == 2) {
            return String.format("实战进阶攻坚：补齐基于 %s 的工业级设计与企业高可用最佳实践，实现技能跃升。", mainSkill);
        } else {
            return String.format("行业前沿破局：冲刺 %s 关键高薪架构与底层优化，对标行业一流胜任力标准。", role);
        }
    }

    private String buildMatchTag(PathStageVO stage, int matchScore) {
        if (matchScore >= 95) return "极度契合";
        int stageIndex = stage.getStageIndex() != null ? stage.getStageIndex() : 1;
        if (stageIndex == 1) return "筑基必备";
        if (stageIndex == 2) return "核心进阶";
        return "前沿突破";
    }
}
