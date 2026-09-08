package com.share.education.ai.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 结构化个性化成长学习路径规划方案 (PathPlanningAgent 产出)。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathPlan {
    /** 目标岗位 */
    private String intendedRole;

    /** 路线整体战略定位 */
    private String overallGoal;

    /** 规划总课程数 */
    private Integer totalCourses;

    /** 预估总学时 (小时) */
    private Integer totalEstimatedHours;

    /** 阶段化路径列表 (拓扑时序递进) */
    @Builder.Default
    private List<PathStageVO> stages = Collections.emptyList();

    /** 权威参考依据 (RAG 检索知识出处，如 "参考阿里巴巴技术专家(P7)技能胜任力矩阵") */
    private String referenceStandard;
}
