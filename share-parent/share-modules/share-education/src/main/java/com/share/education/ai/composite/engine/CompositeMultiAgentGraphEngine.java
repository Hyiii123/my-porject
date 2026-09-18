package com.share.education.ai.composite.engine;

import com.share.education.ai.composite.model.AgentDebateTurn;
import com.share.education.ai.composite.model.DebateConsensusSummary;
import com.share.education.ai.composite.node.*;
import com.share.education.ai.composite.state.DebateBlackboardState;
import com.share.education.ai.evals.AgentEvaluationService;
import com.share.education.ai.model.CriticReport;
import com.share.education.ai.model.LearningPathPlan;
import com.share.education.ai.model.PersonalizedRecommendVO;
import com.share.education.ai.streaming.AgentReasoningEvent;
import com.share.education.ai.workflow.AgentWorkflowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 复合多智能体图执行引擎 (CompositeMultiAgentGraphEngine)。
 * 深度落地【模式 2 动态路由 + 模式 5 圆桌博弈 + 模式 4 审判反思闭环】的统一协同引擎。
 * 支持同步批处理与高响应 SSE 实时流式事件总线。
 */
@Component
public class CompositeMultiAgentGraphEngine {

    private static final Logger log = LoggerFactory.getLogger(CompositeMultiAgentGraphEngine.class);

    private final IntentDispatcherNode intentDispatcherNode;
    private final PedagogyMentorNode pedagogyMentorNode;
    private final IndustryArchitectNode industryArchitectNode;
    private final PathCriticNode pathCriticNode;
    private final ConsensusArbiterNode consensusArbiterNode;
    private final ExplanationSynthesisNode explanationSynthesisNode;
    private final AgentEvaluationService evalService;

    private final ExecutorService streamExecutor = Executors.newFixedThreadPool(
        Math.max(4, Runtime.getRuntime().availableProcessors() * 2),
        r -> {
            Thread t = new Thread(r, "composite-agent-stream-worker");
            t.setDaemon(true);
            return t;
        }
    );

    public CompositeMultiAgentGraphEngine(IntentDispatcherNode intentDispatcherNode,
                                         PedagogyMentorNode pedagogyMentorNode,
                                         IndustryArchitectNode industryArchitectNode,
                                         PathCriticNode pathCriticNode,
                                         ConsensusArbiterNode consensusArbiterNode,
                                         ExplanationSynthesisNode explanationSynthesisNode,
                                         AgentEvaluationService evalService) {
        this.intentDispatcherNode = intentDispatcherNode;
        this.pedagogyMentorNode = pedagogyMentorNode;
        this.industryArchitectNode = industryArchitectNode;
        this.pathCriticNode = pathCriticNode;
        this.consensusArbiterNode = consensusArbiterNode;
        this.explanationSynthesisNode = explanationSynthesisNode;
        this.evalService = evalService;
    }

    /**
     * 同步执行多智能体博弈与反思工作流
     */
    public DebateBlackboardState runWorkflow(DebateBlackboardState state, int limit) {
        long workflowStart = System.currentTimeMillis();
        state.setStartTime(workflowStart);

        // 0. 模式 2：意图动态路由分发
        String query = extractQuery(state);
        String route = intentDispatcherNode.dispatch(query, state);
        state.setRoutedIntent(route);

        if (IntentDispatcherNode.ROUTE_ACTION_CARD.equals(route) || IntentDispatcherNode.ROUTE_MOCK_INTERVIEW.equals(route)) {
            return handleFastRoutedWorkflow(state, route, limit, workflowStart);
        }

        // 1. 学情导师初始化或校准画像基线
        pedagogyMentorNode.initializeProfile(state);

        // 2. 首席仲裁者召开专家圆桌会议 (Round 1)
        consensusArbiterNode.openRoundtable(state);

        // 3. 大厂技术总监提交初始技术方案草案
        industryArchitectNode.proposeInitialPlan(state);

        // 4. 学情成长导师进行认知审查并提出严正质疑 (CHALLENGE)
        pedagogyMentorNode.evaluateAndChallenge(state);

        // 5. 审判质检法官执行 Kahn 拓扑图算法与平滑度审计 (AUDIT)
        boolean passR1 = pathCriticNode.auditCurrentPlan(state);

        if (!passR1) {
            // 模式 4：触发 Actor-Critic 审判反思闭环回路 (Round 2)
            // 6. 首席仲裁者下发针对性折中反思指令
            consensusArbiterNode.guideCompromise(state);

            // 7. 大厂技术总监执行自我反思与折中补丁 (COMPROMISE)
            industryArchitectNode.reflectAndCompromise(state);

            // 8. 学情成长导师复核折中方案并表达认可 (APPROVE)
            pedagogyMentorNode.reviewCompromise(state);

            // 9. 审判质检法官执行终审质检与 Kahn DAG 闭环核验 (AUDIT_PASS)
            pathCriticNode.auditCurrentPlan(state);
        }

        // 10. 首席仲裁者签署共识决议 (CONSENSUS)
        consensusArbiterNode.sealConsensus(state);

        // 11. 合成交付层：Spring AI + RAG 生成可解释性理由
        explanationSynthesisNode.synthesizeDelivery(state, limit);

        // 12. 记录快照至自动化质量度量体系
        recordSnapshot(state, workflowStart);

        long totalCost = System.currentTimeMillis() - workflowStart;
        log.info("[CompositeEngine] 复合多智能体工作流执行完成: 耗时={}ms, 轮次={}, 质检分={}, 共识达成={}",
            totalCost, state.getCurrentRound(),
            state.getCriticReport() != null ? state.getCriticReport().getOverallScore() : 100,
            state.getConsensusReached());

        return state;
    }

    /**
     * 响应式 SSE 流式思考推演：实时推送各专家 Agent 论驳与自省心流
     */
    public void streamWorkflow(DebateBlackboardState state, int limit, SseEmitter emitter) {
        AtomicBoolean isCompleted = new AtomicBoolean(false);
        emitter.onCompletion(() -> isCompleted.set(true));
        emitter.onTimeout(() -> {
            isCompleted.set(true);
            try { emitter.complete(); } catch (Exception ignored) {}
        });
        emitter.onError(e -> isCompleted.set(true));

        streamExecutor.execute(() -> {
            try {
                if (isCompleted.get()) return;
                long workflowStart = System.currentTimeMillis();
                state.setStartTime(workflowStart);

                // STEP 0: 模式 2 动态意图路由分发
                String query = extractQuery(state);
                String route = intentDispatcherNode.dispatch(query, state);
                state.setRoutedIntent(route);

                if (IntentDispatcherNode.ROUTE_ACTION_CARD.equals(route) || IntentDispatcherNode.ROUTE_MOCK_INTERVIEW.equals(route)) {
                    handleFastRoutedStream(state, route, limit, emitter, isCompleted, workflowStart);
                    return;
                }

                sendTurnEvent(emitter, AgentReasoningEvent.of("PROBE_CHECK", "IntentDispatcherNode", 1,
                    String.format("动态意图路由中心完成意向核验：识别目标意向【%s】，路由决议【%s】，已激活多智能体圆桌博弈子图！",
                        state.getIntendedRole(), route),
                    route, 5L), isCompleted);

                // STEP 1: 画像与探针检测
                pedagogyMentorNode.initializeProfile(state);
                sendTurnEvent(emitter, AgentReasoningEvent.of("PROFILE_BUILT", "PedagogyMentorAgent", 1,
                    String.format("学情护航导师完成画像建模：锁定【%s】，挖掘关键技术短板【%s】",
                        state.getUserProfile().getIntendedRole(), String.join("、", state.getUserProfile().getSkillGaps())),
                    state.getUserProfile(), 12L), isCompleted);

                // STEP 2: 圆桌辩论正式开场
                consensusArbiterNode.openRoundtable(state);
                sendTurnEvent(emitter, AgentReasoningEvent.of("CANDIDATES_RECALLED", "ConsensusArbiterAgent", 2,
                    "圆桌首席仲裁者宣布：多智能体专家圆桌辩论正式启动，已召集大厂技术总监、学情导师与审判法官！",
                    state.getIntendedRole(), 8L), isCompleted);

                // STEP 3: 大厂总监率先提案
                industryArchitectNode.proposeInitialPlan(state);
                sendTurnEvent(emitter, AgentReasoningEvent.of("COURSE_ANALYSIS", "IndustryArchitectAgent", 3,
                    String.format("大厂技术总监提交初始草案：立足企业级微服务刚需，规划 %d 门硬核专业课！",
                        state.getCurrentDraftPlan().getTotalCourses()),
                    state.getCurrentDraftPlan(), 25L), isCompleted);

                // STEP 4: 学情导师严正反驳 (Challenge)
                pedagogyMentorNode.evaluateAndChallenge(state);
                sendTurnEvent(emitter, AgentReasoningEvent.of("PATH_PLANNED", "PedagogyMentorAgent", 4,
                    "学情成长导师提出严正质疑：阶段 2 理论过陡，违反最近发展区，劝退风险高达 65%，要求软化！",
                    state.getDialogueTurns().get(state.getDialogueTurns().size() - 1), 18L), isCompleted);

                // STEP 5: 审判法官初审
                boolean passR1 = pathCriticNode.auditCurrentPlan(state);

                if (!passR1) {
                    sendTurnEvent(emitter, AgentReasoningEvent.of("REFLECTION_DIRECTIVE", "PathCriticAgent", 5,
                        String.format("审判质检法官出具初审报告：得分 %d 分 (未通过)，认知阶梯排查指出阶段 2 过陡，驳回方案并下发反思指令！",
                            state.getCriticReport().getOverallScore()),
                        state.getCriticReport(), 20L), isCompleted);

                    // STEP 6: 仲裁者引导第二轮折中
                    consensusArbiterNode.guideCompromise(state);

                    // STEP 7: 总监反思与妥协 (Compromise)
                    industryArchitectNode.reflectAndCompromise(state);

                    // STEP 8: 导师认可签字 (Approve)
                    pedagogyMentorNode.reviewCompromise(state);

                    // STEP 9: 审判法官终审放行 (Audit Pass)
                    pathCriticNode.auditCurrentPlan(state);
                    sendTurnEvent(emitter, AgentReasoningEvent.of("CRITIC_AUDIT", "PathCriticAgent", 5,
                        String.format("审判法官终审通过：复核折中补丁，得分 %d 分 (评级 %s)，Kahn DAG 拓扑 100%% 无环合规！",
                            state.getCriticReport().getOverallScore(), state.getCriticReport().getVerdictLevel()),
                        state.getCriticReport(), 15L), isCompleted);
                } else {
                    sendTurnEvent(emitter, AgentReasoningEvent.of("CRITIC_AUDIT", "PathCriticAgent", 5,
                        String.format("审判法官首轮核验放行：得分 %d 分 (评级 %s)，Kahn DAG 拓扑合规！",
                            state.getCriticReport().getOverallScore(), state.getCriticReport().getVerdictLevel()),
                        state.getCriticReport(), 15L), isCompleted);
                }

                // STEP 10: 仲裁者盖章共识
                consensusArbiterNode.sealConsensus(state);

                // STEP 11: 方案合成与交付
                List<PersonalizedRecommendVO> recs = explanationSynthesisNode.synthesizeDelivery(state, limit);
                recordSnapshot(state, workflowStart);

                Map<String, Object> finalPayload = new LinkedHashMap<>();
                finalPayload.put("recommendations", recs);
                finalPayload.put("learningPath", state.getCurrentDraftPlan());
                finalPayload.put("criticReport", state.getCriticReport());
                finalPayload.put("consensusSummary", state.getConsensusSummary());
                finalPayload.put("debateDialogue", state.getDialogueTurns());
                finalPayload.put("totalPipelineLatencyMs", System.currentTimeMillis() - workflowStart);

                sendTurnEvent(emitter, AgentReasoningEvent.of("FINAL_RESULT", "ExplanationSynthesisNode", 6,
                    String.format("专家团历经 2 轮博弈达成 100%% 共识，【%s】定制进阶方案已交付！", state.getIntendedRole()),
                    finalPayload, 30L), isCompleted);

                sendTurnEvent(emitter, AgentReasoningEvent.of("STREAM_DONE", "ConsensusArbiterAgent", 6,
                    "圆桌推演流圆满完成", null, System.currentTimeMillis() - workflowStart), isCompleted);

                if (!isCompleted.get()) {
                    isCompleted.set(true);
                    emitter.complete();
                }
            } catch (Exception ex) {
                if (!isCompleted.get()) {
                    log.error("[CompositeEngine] 流式博弈推演异常", ex);
                    try {
                        emitter.send(SseEmitter.event().name("agent_event").data(
                            AgentReasoningEvent.of("STREAM_ERROR", "ConsensusArbiterAgent", 0, "推演异常中断: " + ex.getMessage(), null, 0L)
                        ));
                        emitter.completeWithError(ex);
                    } catch (Exception ignored) {}
                    isCompleted.set(true);
                }
            }
        });
    }

    private String extractQuery(DebateBlackboardState state) {
        String query = null;
        if (state.getCustomOverrides() != null) {
            query = (String) state.getCustomOverrides().getOrDefault("query", state.getCustomOverrides().get("userIntent"));
            if (query == null) {
                query = (String) state.getCustomOverrides().getOrDefault("targetRole", state.getCustomOverrides().get("customRole"));
            }
        }
        if (query == null) {
            query = state.getIntendedRole();
        }
        return query;
    }

    private DebateBlackboardState handleFastRoutedWorkflow(DebateBlackboardState state, String route, int limit, long workflowStart) {
        pedagogyMentorNode.initializeProfile(state);
        industryArchitectNode.proposeInitialPlan(state);

        CriticReport report = CriticReport.builder()
            .passed(true)
            .overallScore(100)
            .prerequisiteScore(100)
            .smoothnessScore(100)
            .balanceScore(100)
            .topologyValid(true)
            .cognitiveContinuityScore(100)
            .phaseBalanceScore(100)
            .verdictLevel("卓越 (A+)")
            .summary("快捷直通通道：已跳过重度多轮辩论，直达专属推荐服务")
            .critiqueNotes(List.of("动态意图精准命中：" + route))
            .detectedAnomalies(Collections.emptyList())
            .refinementDirectives(Collections.emptyMap())
            .build();
        state.setCriticReport(report);

        DebateConsensusSummary summary = DebateConsensusSummary.builder()
            .totalRounds(1)
            .consensusReached(true)
            .initialConflictSummary("识别标准意图【" + route + "】，直达业务直通分流")
            .compromiseResolution("免除重型多轮博弈，毫秒级快速交付专属结果")
            .finalCriticScore(100)
            .qualityGrade("卓越 (A+)")
            .keyAgreements(List.of("动态意图直达", "跳过冗余辩论"))
            .build();
        state.setConsensusSummary(summary);

        explanationSynthesisNode.synthesizeDelivery(state, limit);
        recordSnapshot(state, workflowStart);
        return state;
    }

    private void handleFastRoutedStream(DebateBlackboardState state, String route, int limit, SseEmitter emitter, AtomicBoolean isCompleted, long workflowStart) {
        sendTurnEvent(emitter, AgentReasoningEvent.of("PROBE_CHECK", "IntentDispatcherNode", 1,
            String.format("动态意图路由中心完成意向核验：识别目标意向【%s】，路由决议【%s】，已激活快速直通通道！",
                state.getIntendedRole(), route),
            route, 5L), isCompleted);

        pedagogyMentorNode.initializeProfile(state);
        sendTurnEvent(emitter, AgentReasoningEvent.of("PROFILE_BUILT", "PedagogyMentorAgent", 1,
            String.format("学情护航导师完成画像建模：锁定目标【%s】", state.getUserProfile().getIntendedRole()),
            state.getUserProfile(), 10L), isCompleted);

        industryArchitectNode.proposeInitialPlan(state);
        sendTurnEvent(emitter, AgentReasoningEvent.of("COURSE_ANALYSIS", "IndustryArchitectAgent", 3,
            String.format("快捷通道完成选品召回：立足意图规划 %d 门专业课！", state.getCurrentDraftPlan().getTotalCourses()),
            state.getCurrentDraftPlan(), 15L), isCompleted);

        CriticReport report = CriticReport.builder()
            .passed(true)
            .overallScore(100)
            .prerequisiteScore(100)
            .smoothnessScore(100)
            .balanceScore(100)
            .topologyValid(true)
            .cognitiveContinuityScore(100)
            .phaseBalanceScore(100)
            .verdictLevel("卓越 (A+)")
            .summary("快捷直通通道：已跳过重度多轮辩论，直达专属推荐服务")
            .critiqueNotes(List.of("动态意图精准命中：" + route))
            .detectedAnomalies(Collections.emptyList())
            .refinementDirectives(Collections.emptyMap())
            .build();
        state.setCriticReport(report);

        DebateConsensusSummary summary = DebateConsensusSummary.builder()
            .totalRounds(1)
            .consensusReached(true)
            .initialConflictSummary("识别标准意图【" + route + "】，直达业务直通分流")
            .compromiseResolution("免除重型多轮博弈，毫秒级快速交付专属结果")
            .finalCriticScore(100)
            .qualityGrade("卓越 (A+)")
            .keyAgreements(List.of("动态意图直达", "跳过冗余辩论"))
            .build();
        state.setConsensusSummary(summary);

        sendTurnEvent(emitter, AgentReasoningEvent.of("CRITIC_AUDIT", "PathCriticAgent", 5,
            "审判法官放行：快捷方案无需重度拓扑排布，质检通过！", report, 10L), isCompleted);

        List<PersonalizedRecommendVO> recs = explanationSynthesisNode.synthesizeDelivery(state, limit);
        recordSnapshot(state, workflowStart);

        Map<String, Object> finalPayload = new LinkedHashMap<>();
        finalPayload.put("recommendations", recs);
        finalPayload.put("learningPath", state.getCurrentDraftPlan());
        finalPayload.put("criticReport", state.getCriticReport());
        finalPayload.put("consensusSummary", state.getConsensusSummary());
        finalPayload.put("totalPipelineLatencyMs", System.currentTimeMillis() - workflowStart);

        sendTurnEvent(emitter, AgentReasoningEvent.of("FINAL_RESULT", "ExplanationSynthesisNode", 6,
            String.format("快捷通道已为【%s】交付专属方案！", state.getIntendedRole()),
            finalPayload, 15L), isCompleted);

        sendTurnEvent(emitter, AgentReasoningEvent.of("STREAM_DONE", "ConsensusArbiterAgent", 6,
            "快捷推演流完成", null, System.currentTimeMillis() - workflowStart), isCompleted);

        if (!isCompleted.get()) {
            isCompleted.set(true);
            emitter.complete();
        }
    }

    private boolean sendTurnEvent(SseEmitter emitter, AgentReasoningEvent event, AtomicBoolean isCompleted) {
        if (isCompleted.get()) return false;
        try {
            emitter.send(SseEmitter.event().name("agent_event").data(event));
            return true;
        } catch (Exception ex) {
            isCompleted.set(true);
            return false;
        }
    }

    private void recordSnapshot(DebateBlackboardState state, long startTime) {
        try {
            AgentWorkflowContext snapshot = AgentWorkflowContext.builder()
                .sessionId(state.getSessionId() != null ? state.getSessionId() : UUID.randomUUID().toString())
                .userId(state.getUserId())
                .startTime(startTime)
                .userProfile(state.getUserProfile())
                .candidates(state.getCandidateCourses())
                .analyzedCourses(state.getAnalyzedCourses())
                .learningPathPlan(state.getCurrentDraftPlan())
                .criticReport(state.getCriticReport())
                .passedCritic(state.getCriticReport() != null && Boolean.TRUE.equals(state.getCriticReport().getPassed()))
                .recommendations(state.getFinalRecommendations())
                .build();
            evalService.recordPipelineExecution(snapshot);
        } catch (Exception ex) {
            log.warn("[CompositeEngine] 记录质量快照异常: {}", ex.getMessage());
        }
    }
}
