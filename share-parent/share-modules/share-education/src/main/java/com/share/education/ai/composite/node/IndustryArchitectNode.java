package com.share.education.ai.composite.node;

import com.share.education.ai.agent.CourseAnalysisAgent;
import com.share.education.ai.agent.PathPlanningAgent;
import com.share.education.ai.agent.RecommendationAgent;
import com.share.education.ai.composite.model.AgentDebateTurn;
import com.share.education.ai.composite.model.PlanPatch;
import com.share.education.ai.composite.state.DebateBlackboardState;
import com.share.education.ai.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 复合架构模式 5：大厂技术总监节点 (IndustryArchitectNode)。
 * 角色设定：大厂资深架构师 / 技术委员会专家。
 * 立场：坚守大厂岗位刚需与硬核实战标准，主张高并发、微服务、分布式全链路攻坚；
 * 行为：第一轮提出极具竞争力的初版架构进阶草案；第二轮面对学情导师质疑时，执行自我反思与折中（以战带练），产出修改补丁。
 */
@Component
public class IndustryArchitectNode {

    private static final Logger log = LoggerFactory.getLogger(IndustryArchitectNode.class);

    private final RecommendationAgent recommendationAgent;
    private final CourseAnalysisAgent courseAnalysisAgent;
    private final PathPlanningAgent pathPlanningAgent;

    public IndustryArchitectNode(RecommendationAgent recommendationAgent,
                                 CourseAnalysisAgent courseAnalysisAgent,
                                 PathPlanningAgent pathPlanningAgent) {
        this.recommendationAgent = recommendationAgent;
        this.courseAnalysisAgent = courseAnalysisAgent;
        this.pathPlanningAgent = pathPlanningAgent;
    }

    /**
     * 第一轮：提出初始技术架构进阶路线草案 (Initial Proposal)
     */
    public void proposeInitialPlan(DebateBlackboardState state) {
        long tStart = System.currentTimeMillis();
        UserProfileContext profile = state.getUserProfile();
        String role = state.getIntendedRole() != null ? state.getIntendedRole() : "全栈架构师";

        // 1. 算法多路初筛召回 (含 Python 深度模型预测)
        List<CandidateCourseDTO> candidates = recommendationAgent.recallCandidates(profile, 14);
        state.setCandidateCourses(candidates);

        // 2. 布鲁姆认知解构
        List<AnalyzedCourseVO> analyzed = courseAnalysisAgent.analyzeCourses(candidates, profile);
        state.setAnalyzedCourses(analyzed);

        // 3. 初版 DAG 进阶路线编排
        LearningPathPlan initialPlan = pathPlanningAgent.planPath(profile, analyzed, state.getCustomOverrides());
        state.setCurrentDraftPlan(initialPlan);

        long latency = System.currentTimeMillis() - tStart;
        state.recordLatency("IndustryArchitectNode", latency);

        // 4. 发起首轮发言
        String arg = String.format("针对【%s】岗位标准，我基于一线大厂胜任力图谱规划了 4 阶段进阶方案，共 %d 门专业课。核心聚焦企业级微服务治理与高并发架构实战，必须满足招聘硬指标！",
            role, initialPlan.getTotalCourses());

        AgentDebateTurn turn = AgentDebateTurn.builder()
            .round(state.getCurrentRound())
            .speaker("IndustryArchitect")
            .speakerName("大厂技术总监")
            .stance("PROPOSE")
            .targetObject("GlobalCurriculum")
            .argument(arg)
            .metricSummary(String.format("总课程: %d 门, 预估总学时: %dh", initialPlan.getTotalCourses(), initialPlan.getTotalEstimatedHours()))
            .latencyMs(latency)
            .timestamp(System.currentTimeMillis())
            .build();

        state.recordTurn(turn);
        log.info("[IndustryArchitectNode] 初始方案已发布至黑板: 课程数={}, 学时={}h",
            initialPlan.getTotalCourses(), initialPlan.getTotalEstimatedHours());
    }

    /**
     * 第二轮：针对学情导师质疑与法官修正指令执行自省反思与折中 (Compromise)
     */
    public void reflectAndCompromise(DebateBlackboardState state) {
        long tStart = System.currentTimeMillis();

        // 挑选一门温和的实战过渡课程作为补丁 (严格去重：不得与当前方案中已规划的课程重复)
        List<AnalyzedCourseVO> analyzed = state.getAnalyzedCourses();
        AnalyzedCourseVO transitionalCourse = null;
        if (analyzed != null && !analyzed.isEmpty()) {
            Set<Long> alreadyPlannedIds = (state.getCurrentDraftPlan() != null && state.getCurrentDraftPlan().getStages() != null)
                ? state.getCurrentDraftPlan().getStages().stream()
                    .filter(s -> s.getCourses() != null)
                    .flatMap(s -> s.getCourses().stream())
                    .map(AnalyzedCourseVO::getCourseId)
                    .collect(Collectors.toSet())
                : Collections.emptySet();

            transitionalCourse = analyzed.stream()
                .filter(c -> !alreadyPlannedIds.contains(c.getCourseId()))
                .filter(c -> c.getDifficultyLevel() != null && c.getDifficultyLevel() <= 2)
                .findFirst()
                .orElse(null);
        }

        // 组装补丁：在第 2 阶段软化理论深度，插入过渡实战课
        PlanPatch patch = PlanPatch.builder()
            .targetStageIndex(2)
            .adjustedDifficulty(2)
            .hoursAdjustment(-5)
            .insertCourses(transitionalCourse != null ? List.of(transitionalCourse) : Collections.emptyList())
            .rationale("采纳学情导师防劝退意见：软化第 2 阶段纯理论深度，将难点分流，并插入过渡实战模块")
            .build();

        long latency = System.currentTimeMillis() - tStart;
        state.recordLatency("IndustryArchitectNode_Compromise", latency);

        AgentDebateTurn turn = AgentDebateTurn.builder()
            .round(state.getCurrentRound())
            .speaker("IndustryArchitect")
            .speakerName("大厂技术总监")
            .stance("COMPROMISE")
            .targetObject("Stage 2: Core Acceleration")
            .argument("我充分理解学情导师关于认知负荷的担忧。为兼顾架构师胜任力底线，我做出折中妥协：调整第 2 阶段课程坡度，将晦涩理论改为实战带练，并把前置依赖做细颗粒度拆解。")
            .proposedPatch(patch)
            .metricSummary("已应用方案补丁：降低阶段 2 难度方差，平滑先修坡度")
            .latencyMs(latency)
            .timestamp(System.currentTimeMillis())
            .build();

        state.recordTurn(turn);
        log.info("[IndustryArchitectNode] 折中方案补丁已更新至黑板");
    }
}
