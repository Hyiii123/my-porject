package com.share.education.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 审判反思智能体质检报告 (PathCriticAgent 产出)。
 * 量化评估学习成长路径的拓扑合法性、认知平滑度与阶段均衡性。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriticReport {

    /** 是否通过质检审查 (综合评分 >= 80) */
    private Boolean passed;

    /** 综合审判质检得分 (0 ~ 100) */
    private Integer overallScore;

    /** 先修拓扑合规度得分 (0 ~ 100，排查反向先修与环路) */
    private Integer prerequisiteScore;

    /** 认知进阶平滑度得分 (0 ~ 100，排查难度断层) */
    private Integer smoothnessScore;

    /** 阶段结构均衡度得分 (0 ~ 100，排查阶段过载或过疏) */
    private Integer balanceScore;

    /** 综合评级等级 (如 "卓越 (A+)", "优良 (A)", "合格 (B)", "待优化 (C)") */
    private String verdictLevel;

    /** 质检审计细则说明清单 */
    @Builder.Default
    private List<String> critiqueNotes = Collections.emptyList();

    /** 探测出的潜在异常风险清单 */
    @Builder.Default
    private List<String> detectedAnomalies = Collections.emptyList();

    /** 触发反思回溯时的针对性修正指令集 (供 RecommendationAgent 与 PathPlanningAgent 局部修正) */
    @Builder.Default
    private Map<String, Object> refinementDirectives = Collections.emptyMap();
}
