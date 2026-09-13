package com.share.education.ai.agent;

import com.share.education.ai.client.DashScopeAiClient;
import com.share.education.ai.model.*;
import com.share.education.ai.rag.EducationKnowledgeRAG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 智能体 5：解释生成 Agent (ExplanationGenerationAgent)。
 *
 * <p>【核心职责】：
 * 1. 结合路径规划成果与 RAG 行业胜任力证据切片，调用大模型（或本地轻量规则引擎）；
 * 2. 产出“可解释性推荐理由 (Explainable AI)”——告别黑盒，让学员清晰理解推荐原因与学后收益；
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
                // 2. 生成可解释性理由 (AI 优先 + 本地规则降级)
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
                String systemPrompt = "你是一名资深 IT 职业教育规划专家。请针对学员目标岗位、当前技能画像、知识图谱推导先修链路和推荐课程，"
                    + "用一句温暖、专业、富有严密逻辑性的话（45字以内）阐述【为什么推荐这门课程以及学完对前沿技术突破的帮助】。直接输出一句话，不带格式。";
                
                String pathEvidence = (ac.getEvidencePaths() != null && !ac.getEvidencePaths().isEmpty())
                    ? "；知识图谱推导先修链路：" + String.join("，", ac.getEvidencePaths()) : "";

                String userPrompt = String.format(
                    "学员目标岗位：%s；已有技能：%s；当前阶段：%s；推荐课程：《%s》；核心知识点：%s%s；行业背景参考：%s",
                    profile.getIntendedRole(),
                    String.join(",", profile.getTopSkills()),
                    stage.getStageName(),
                    ac.getCourseName(),
                    String.join(",", ac.getCoreKnowledgePoints()),
                    pathEvidence,
                    benchmarkEvidence
                );

                String aiReason = aiClient.generate(systemPrompt, userPrompt);
                if (StringUtils.hasText(aiReason)) {
                    return aiReason.replaceAll("[“”\"]", "").trim();
                }
            } catch (Exception ex) {
                log.warn("大模型生成推荐理由异常，平滑降级为规则引擎: {}", ex.getMessage());
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

        if (stage.getStageIndex() == 1) {
            return String.format("筑基必备：稳固 %s 核心语法与工程规范，为进阶 %s 构筑不可或缺的底层基石。", mainSkill, role);
        } else if (stage.getStageIndex() == 2) {
            return String.format("核心进阶：针对 %s 方向核心诉求，突破单体瓶颈，实战攻坚 %s 核心技术栈。", role, courseName);
        } else if (stage.getStageIndex() == 3) {
            return String.format("架构实战：对标大厂高并发架构标准，补齐分布式全景短板，助力迈向 %s 骨干梯队。", role);
        } else {
            return String.format("技术破局：洞悉前沿技术内核与系统调优，突破职业天花板，冲刺 %s 架构专家。", role);
        }
    }

    private String buildMatchTag(PathStageVO stage, int matchScore) {
        if (matchScore >= 98) {
            return "🔥 目标岗位强契合";
        } else if (stage.getStageIndex() == 1) {
            return "📌 先修必修基石";
        } else if (stage.getStageIndex() >= 3) {
            return "🚀 架构突破攻坚";
        } else {
            return "💡 关键技能补齐";
        }
    }
}
