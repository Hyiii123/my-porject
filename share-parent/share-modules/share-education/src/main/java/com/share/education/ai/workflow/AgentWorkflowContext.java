package com.share.education.ai.workflow;

import com.share.education.ai.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * 智能体运行态上下文与会话快照容器 (AgentWorkflowContext)。
 * 实现多智能体有状态运行态、阶段 Checkpoint 记录与 Human-in-the-Loop 人机协同微调能力。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentWorkflowContext {

    /** 会话唯一标识 */
    private String sessionId;

    /** 学员 ID */
    private Long userId;

    /** 阶段 1 快照：学员认知画像上下文 */
    private UserProfileContext userProfile;

    /** 阶段 2 快照：算法初排候选集 */
    @Builder.Default
    private List<CandidateCourseDTO> candidates = new ArrayList<>();

    /** 阶段 3 快照：大纲知识解构结果 */
    @Builder.Default
    private List<AnalyzedCourseVO> analyzedCourses = new ArrayList<>();

    /** 阶段 4 快照：路径拓扑规划方案 */
    private LearningPathPlan learningPathPlan;

    /** 阶段 5 快照：审判反思质检报告 */
    private CriticReport criticReport;

    /** 阶段 6 快照：可解释性推荐卡片结果 */
    @Builder.Default
    private List<PersonalizedRecommendVO> recommendations = new ArrayList<>();

    /** 人机协同指令集 (Human-in-the-Loop Overrides: 如排除指定课程、强制指定阶段难度、跳过基础等) */
    @Builder.Default
    private Map<String, Object> customOverrides = new HashMap<>();

    /** 各智能体耗时统计 (毫秒) */
    @Builder.Default
    private Map<String, Long> agentLatencies = new LinkedHashMap<>();

    /** 已触发的反思回溯轮次 (避免死循环，上限 1 次) */
    @Builder.Default
    private int reflectionCount = 0;

    /** 是否通过了审判质检 */
    @Builder.Default
    private boolean passedCritic = false;

    /** 工作流开始时间戳 */
    @Builder.Default
    private long startTime = System.currentTimeMillis();

    /** 记录单个智能体耗时 */
    public void recordLatency(String agentName, long costMs) {
        if (agentLatencies == null) {
            agentLatencies = new LinkedHashMap<>();
        }
        agentLatencies.put(agentName, costMs);
    }
}
