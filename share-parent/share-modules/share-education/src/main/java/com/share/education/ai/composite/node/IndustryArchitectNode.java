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
 * 行为：第一轮提出极具竞争力的初版架构进阶草案；第二轮深度解析审判法官下发的修正指令集，
 * 触发 PathPlanningAgent 执行自适应拓扑重构与阶段坡度软化，完成真反思与真折中。
 */
@Component
public class IndustryArchitectNode {

    private static final Logger log = LoggerFactory.getLogger(IndustryArchitectNode.class);

    private final RecommendationAgent recommendationAgent;
    private final CourseAnalysisAgent courseAnalysisAgent;
    private final PathPlanningAgent pathPlanningAgent;
    private final com.share.education.ai.client.ThirdPartyAiClient aiClient;
    private final com.share.education.ai.tools.market.JobMarketRadarTool marketRadarTool;

    public IndustryArchitectNode(RecommendationAgent recommendationAgent,
                                 CourseAnalysisAgent courseAnalysisAgent,
                                 PathPlanningAgent pathPlanningAgent,
                                 @org.springframework.beans.factory.annotation.Autowired(required = false) com.share.education.ai.client.ThirdPartyAiClient aiClient,
                                 @org.springframework.beans.factory.annotation.Autowired(required = false) com.share.education.ai.tools.market.JobMarketRadarTool marketRadarTool) {
        this.recommendationAgent = recommendationAgent;
        this.courseAnalysisAgent = courseAnalysisAgent;
        this.pathPlanningAgent = pathPlanningAgent;
        this.aiClient = aiClient;
        this.marketRadarTool = marketRadarTool;
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

        // 调取产业前沿招聘行情雷达 (Live Market Tool)
        com.share.education.ai.tools.market.JobMarketTrackInfo marketInfo = null;
        if (marketRadarTool != null) {
            marketInfo = marketRadarTool.apply(new com.share.education.ai.tools.market.JobMarketQueryRequest(role));
        }

        // 4. 发起首轮发言 (支持大模型动态立论)
        String dynamicArg = null;
        if (aiClient != null && aiClient.isAvailable()) {
            String sys = "你是一线大厂资深架构师和技术委员会专家。你的职责是把关企业级硬核标准与技术胜任力。请针对目标岗位提出首版4阶段进阶方案的专业立论理由（不超过100字）。";
            String marketText = marketInfo != null ? String.format("【前沿行情】市场热度【%d分】，平均薪资【%s】，高频技术栈【%s】。",
                marketInfo.getDemandIndex(), marketInfo.getAvgSalaryRange(), String.join("/", marketInfo.getHotKeywords().subList(0, Math.min(4, marketInfo.getHotKeywords().size())))) : "";
            String usr = String.format("目标岗位：%s，%s已规划课程：%d门，预估学时：%dh。请说明你的设计初衷与大厂刚需。",
                role, marketText, initialPlan.getTotalCourses(), initialPlan.getTotalEstimatedHours());
            dynamicArg = aiClient.generate(sys, usr);
        }

        String marketNote = marketInfo != null ? String.format("【招聘趋势：%s | 平均薪资：%s】", marketInfo.getDemandTrend(), marketInfo.getAvgSalaryRange()) : "";
        String arg = (dynamicArg != null && !dynamicArg.isBlank()) ? dynamicArg.trim() : String.format(
            "针对【%s】岗位标准 %s，我基于一线大厂胜任力图谱规划了 4 阶段进阶方案，共 %d 门专业课。核心聚焦企业级实战标准，必须满足招聘硬指标！",
            role, marketNote, initialPlan.getTotalCourses());

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
     * 第二轮：针对学情导师质疑与法官修正指令执行自省反思与深度折中 (Compromise & Re-planning)
     */
    public void reflectAndCompromise(DebateBlackboardState state) {
        long tStart = System.currentTimeMillis();
        String role = state.getIntendedRole() != null ? state.getIntendedRole() : "技术工程师";

        // 1. 深度读取审判法官下发的真实修正指令集
        Map<String, Object> directives = state.getRefinementDirectives();
        Map<String, Object> effectiveDirectives = new HashMap<>();
        if (state.getCustomOverrides() != null) {
            effectiveDirectives.putAll(state.getCustomOverrides());
        }
        if (directives != null) {
            effectiveDirectives.putAll(directives);
        }
        // 显式激活核心修复指令：先修拓扑防倒置与难度平滑过渡
        effectiveDirectives.put("fixPrerequisiteInversion", true);
        effectiveDirectives.put("smoothDifficultyTransition", true);

        // 2. 调用 PathPlanningAgent 依据修正指令执行全局 DAG 自适应重排
        List<AnalyzedCourseVO> analyzed = state.getAnalyzedCourses();
        LearningPathPlan rePlanned = pathPlanningAgent.planPath(state.getUserProfile(), analyzed, effectiveDirectives);

        // 3. 挑选过渡实战课程组装补丁
        AnalyzedCourseVO transitionalCourse = null;
        if (analyzed != null && !analyzed.isEmpty()) {
            Set<Long> alreadyPlannedIds = (rePlanned != null && rePlanned.getStages() != null)
                ? rePlanned.getStages().stream()
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

        // 4. 更新当前草案方案至黑板
        if (rePlanned != null && rePlanned.getStages() != null && !rePlanned.getStages().isEmpty()) {
            state.setCurrentDraftPlan(rePlanned);
        }

        PlanPatch patch = PlanPatch.builder()
            .targetStageIndex(2)
            .adjustedDifficulty(2)
            .hoursAdjustment(-5)
            .insertCourses(transitionalCourse != null ? List.of(transitionalCourse) : Collections.emptyList())
            .rationale("全面采纳质检法官指令与学情导师意见：执行 Kahn DAG 拓扑重排，软化阶段 2 理论坡度，插入渐进式过渡模块")
            .build();

        long latency = System.currentTimeMillis() - tStart;
        state.recordLatency("IndustryArchitectNode_Compromise", latency);

        // 5. 动态生成自我反思与折中申明
        String directiveSummary = directives != null && !directives.isEmpty()
            ? String.join("、", directives.keySet()) : "平滑阶段坡度与先修重排";

        String dynamicArg = null;
        if (aiClient != null && aiClient.isAvailable()) {
            String sys = "你是一线大厂资深架构师和技术委员会专家。面对学情导师关于认知负荷的质疑和质检法官的量化修正指令，请陈述你的折中反思和技术妥协举措（控制在90字内）。";
            String usr = String.format("目标岗位：%s，已响应质检指令【%s】执行了全局先修拓扑重排，并在阶段2插入实战过渡。请说明妥协理由。",
                role, directiveSummary);
            dynamicArg = aiClient.generate(sys, usr);
        }

        String arg = (dynamicArg != null && !dynamicArg.isBlank()) ? dynamicArg.trim() : String.format(
            "我充分理解学情导师与质检法官的量化反馈。针对【%s】指令，我已响应下调阶段 2 理论深度，通过 Kahn DAG 拓扑修正消除先修断层，并插入过渡实战模块，确保既防劝退又保就业硬核标准！",
            directiveSummary);

        AgentDebateTurn turn = AgentDebateTurn.builder()
            .round(state.getCurrentRound())
            .speaker("IndustryArchitect")
            .speakerName("大厂技术总监")
            .stance("COMPROMISE")
            .targetObject("Stage 2 & Global Topology")
            .argument(arg)
            .proposedPatch(patch)
            .metricSummary("已响应质检指令执行拓扑自适应重排：消除先修倒置，软化认知坡度")
            .latencyMs(latency)
            .timestamp(System.currentTimeMillis())
            .build();

        state.recordTurn(turn);
        log.info("[IndustryArchitectNode] 响应修正指令已完成自适应重排并应用补丁至黑板: 指令集={}", directiveSummary);
    }
}
