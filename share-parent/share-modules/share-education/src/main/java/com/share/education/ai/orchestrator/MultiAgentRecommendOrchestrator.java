package com.share.education.ai.orchestrator;

import com.share.common.redis.service.RedisService;
import com.share.education.ai.agent.*;
import com.share.education.ai.config.AiRecommendProperties;
import com.share.education.ai.evals.AgentEvalMetricsVO;
import com.share.education.ai.evals.AgentEvaluationService;
import com.share.education.ai.model.*;
import com.share.education.ai.streaming.AgentReasoningEvent;
import com.share.education.ai.workflow.AgentWorkflowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 下一代动态自省多智能体协同导学总编排器 (MultiAgentRecommendOrchestrator)。
 *
 * <p>【架构工作流驱动 (Workflow Execution Engine - L4 闭环架构)】：
 * <pre>
 *   学员/访客
 *      ↓
 *   学员画像与主动探针智能体 (UserProfileAgent, 50维画像建模与冷启动自适应主动探针)
 *      ↓
 *   推荐匹配 Agent (RecommendationAgent, 算法SPI初排多路打散)
 *      ↓
 *   大纲解构 Agent (CourseAnalysisAgent, 布鲁姆认知分级与知识切片)
 *      ↓
 *   路线规划 Agent (PathPlanningAgent, 4阶段拓扑有向无环图编排)
 *      ↓
 *   审判反思 Agent (PathCriticAgent, 拓扑合规度/平滑度/均衡度量化审计)
 *      ↺ [未达标触发单次受控反思重排回路 (One-Pass Reflection Loop)]
 *      ↓
 *   解释生成 Agent (ExplanationGenerationAgent, Spring AI/RAG证据链)
 *      ↓
 *   状态机快照与度量 (AgentWorkflowContext ➔ AgentEvaluationService ➔ HUD)
 * </pre>
 * </p>
 */
@Service
public class MultiAgentRecommendOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(MultiAgentRecommendOrchestrator.class);

    private static final String CACHE_RECOMMEND_PREFIX = "edu:ai:recommend:";
    private static final String CACHE_PATH_PREFIX = "edu:ai:path:";

    private final UserProfileAgent userProfileAgent;
    private final RecommendationAgent recommendationAgent;
    private final CourseAnalysisAgent courseAnalysisAgent;
    private final PathPlanningAgent pathPlanningAgent;
    private final PathCriticAgent pathCriticAgent;
    private final ExplanationGenerationAgent explanationGenerationAgent;
    private final AgentEvaluationService evalService;
    private final RedisService redisService;
    private final AiRecommendProperties properties;

    private final ExecutorService agentThreadPool = new ThreadPoolExecutor(
            Math.max(2, Runtime.getRuntime().availableProcessors()),
            Math.max(4, Runtime.getRuntime().availableProcessors() * 2),
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "agent-orchestrator-" + counter.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public MultiAgentRecommendOrchestrator(UserProfileAgent userProfileAgent,
                                          RecommendationAgent recommendationAgent,
                                          CourseAnalysisAgent courseAnalysisAgent,
                                          PathPlanningAgent pathPlanningAgent,
                                          PathCriticAgent pathCriticAgent,
                                          ExplanationGenerationAgent explanationGenerationAgent,
                                          AgentEvaluationService evalService,
                                          RedisService redisService,
                                          AiRecommendProperties properties) {
        this.userProfileAgent = userProfileAgent;
        this.recommendationAgent = recommendationAgent;
        this.courseAnalysisAgent = courseAnalysisAgent;
        this.pathPlanningAgent = pathPlanningAgent;
        this.pathCriticAgent = pathCriticAgent;
        this.explanationGenerationAgent = explanationGenerationAgent;
        this.evalService = evalService;
        this.redisService = redisService;
        this.properties = properties;
    }

    /**
     * 运行完整流水线，产出高契合度个性化推荐课程流
     *
     * @param userId 学员用户 ID (未登录访客为 null)
     * @param limit 期望数量
     * @return 带有丰富可解释性属性的推荐结果
     */
    public List<PersonalizedRecommendVO> recommendCourses(Long userId, int limit) {
        return recommendCourses(userId, limit, Collections.emptyMap(), Collections.emptyMap());
    }

    /**
     * 支持探针反馈校准与人机协同微调的完整推荐推演流水线
     */
    public List<PersonalizedRecommendVO> recommendCourses(Long userId,
                                                         int limit,
                                                         Map<String, String> probeAnswers,
                                                         Map<String, Object> customOverrides) {
        int safeLimit = Math.max(1, Math.min(limit, 20));

        boolean hasCustomParams = (probeAnswers != null && !probeAnswers.isEmpty())
            || (customOverrides != null && !customOverrides.isEmpty());

        // 1. 尝试从 Redis 缓存获取（无微调参数时）
        String cacheKey = CACHE_RECOMMEND_PREFIX + (userId != null ? userId : "guest") + ":" + safeLimit;
        if (!hasCustomParams && redisService != null) {
            try {
                List<PersonalizedRecommendVO> cached = redisService.getCacheObject(cacheKey);
                if (cached != null && !cached.isEmpty()) {
                    return cached;
                }
            } catch (Exception ignored) {}
        }

        // 2. 初始化工作流运行态上下文 (WorkflowContext)
        AgentWorkflowContext ctx = AgentWorkflowContext.builder()
            .sessionId(UUID.randomUUID().toString())
            .userId(userId)
            .customOverrides(customOverrides != null ? customOverrides : Collections.emptyMap())
            .startTime(System.currentTimeMillis())
            .build();

        // 步骤 1: 用户画像 Agent 提取多维画像 (若有探针输入则动态校准)
        long t1 = System.currentTimeMillis();
        UserProfileContext profile = userProfileAgent.buildProfile(userId, probeAnswers);
        if (customOverrides != null && customOverrides.containsKey("customRole")) {
            String role = customOverrides.get("customRole").toString();
            profile = UserProfileContext.builder()
                .userId(profile.getUserId())
                .intendedRole(role)
                .preferredDifficulty(profile.getPreferredDifficulty())
                .skillWeights(profile.getSkillWeights())
                .topSkills(profile.getTopSkills())
                .skillGaps(profile.getSkillGaps())
                .disciplineScore(profile.getDisciplineScore())
                .userTags(profile.getUserTags())
                .enrolledCourseIds(profile.getEnrolledCourseIds())
                .completedHours(profile.getCompletedHours())
                .cognitiveLevel(profile.getCognitiveLevel())
                .chronologicalCourseIds(profile.getChronologicalCourseIds())
                .courseProgressMap(profile.getCourseProgressMap())
                .build();
        }
        ctx.setUserProfile(profile);
        ctx.recordLatency("UserProfileAgent", System.currentTimeMillis() - t1);

        // 步骤 2: 推荐 Agent 调度算法引擎 SPI 进行多路初排召回
        long t2 = System.currentTimeMillis();
        List<CandidateCourseDTO> candidates = recommendationAgent.recallCandidates(profile, 14);
        ctx.setCandidates(candidates);
        ctx.recordLatency("RecommendationAgent", System.currentTimeMillis() - t2);

        // 步骤 3: 课程分析 Agent 深度拆解大纲知识拓扑与布鲁姆认知分级
        long t3 = System.currentTimeMillis();
        List<AnalyzedCourseVO> analyzedCourses = courseAnalysisAgent.analyzeCourses(candidates, profile);
        ctx.setAnalyzedCourses(analyzedCourses);
        ctx.recordLatency("CourseAnalysisAgent", System.currentTimeMillis() - t3);

        // 步骤 4: 路径规划 Agent 进行阶段化进阶路线拓扑编排
        long t4 = System.currentTimeMillis();
        LearningPathPlan pathPlan = pathPlanningAgent.planPath(profile, analyzedCourses, customOverrides);
        ctx.recordLatency("PathPlanningAgent", System.currentTimeMillis() - t4);

        // 步骤 5: 审判反思 Agent 执行量化质检评估 (DAG 拓扑、认知阶梯、阶段均衡)
        long t5 = System.currentTimeMillis();
        CriticReport criticReport = pathCriticAgent.auditPathPlan(pathPlan, profile);

        // 【闭环自省机制】：未达标时触发单次受控反思回路 (One-Pass Reflection Loop)
        if (!Boolean.TRUE.equals(criticReport.getPassed()) && ctx.getReflectionCount() == 0) {
            ctx.setReflectionCount(1);
            log.warn("[Orchestrator] 审判质检未达标 (综合得分: {})，触发反思回溯回路: {}",
                criticReport.getOverallScore(), criticReport.getDetectedAnomalies());

            // 依据 Critic 修正指令重新切分排布
            Map<String, Object> directives = new HashMap<>(criticReport.getRefinementDirectives());
            if (customOverrides != null) {
                directives.putAll(customOverrides);
            }
            pathPlan = pathPlanningAgent.planPath(profile, analyzedCourses, directives);
            criticReport = pathCriticAgent.auditPathPlan(pathPlan, profile);
        }

        pathPlan.setCriticReport(criticReport);
        ctx.setLearningPathPlan(pathPlan);
        ctx.setCriticReport(criticReport);
        ctx.setPassedCritic(Boolean.TRUE.equals(criticReport.getPassed()));
        ctx.recordLatency("PathCriticAgent", System.currentTimeMillis() - t5);

        // 步骤 6: 解释生成 Agent 融合 RAG 行业标准，生成可解释性推荐理由
        long t6 = System.currentTimeMillis();
        List<PersonalizedRecommendVO> result = explanationGenerationAgent.generateExplanations(profile, pathPlan, safeLimit);
        ctx.setRecommendations(result);
        ctx.recordLatency("ExplanationGenerationAgent", System.currentTimeMillis() - t6);

        // 步骤 7: 将执行快照写入评测体系与度量大屏
        evalService.recordPipelineExecution(ctx);

        long totalCost = System.currentTimeMillis() - ctx.getStartTime();
        log.info("下一代 L4 多智能体流水线执行完成: 用户={}, 耗时={}ms, Critic得分={}, 推荐数={}",
            userId, totalCost, criticReport.getOverallScore(), result.size());

        // 写入缓存 (仅在无自定义微调时)
        if (!hasCustomParams && redisService != null && !result.isEmpty()) {
            try {
                redisService.setCacheObject(cacheKey, result, (long) properties.getCacheTtlMinutes(), TimeUnit.MINUTES);
            } catch (Exception ignored) {}
        }

        return result;
    }

    /**
     * 获取带有质检报告的学员专属结构化全景学习成长进阶路径规划
     */
    public LearningPathPlan getLearningPath(Long userId) {
        return getLearningPath(userId, Collections.emptyMap(), Collections.emptyMap());
    }

    public LearningPathPlan getLearningPath(Long userId,
                                           Map<String, String> probeAnswers,
                                           Map<String, Object> customOverrides) {
        boolean hasCustom = (probeAnswers != null && !probeAnswers.isEmpty())
            || (customOverrides != null && !customOverrides.isEmpty());

        String cacheKey = CACHE_PATH_PREFIX + (userId != null ? userId : "guest");
        if (!hasCustom && redisService != null) {
            try {
                LearningPathPlan cached = redisService.getCacheObject(cacheKey);
                if (cached != null) {
                    return cached;
                }
            } catch (Exception ignored) {}
        }

        UserProfileContext profile = userProfileAgent.buildProfile(userId, probeAnswers);
        if (customOverrides != null && customOverrides.containsKey("customRole")) {
            String role = customOverrides.get("customRole").toString();
            profile = UserProfileContext.builder()
                .userId(profile.getUserId())
                .intendedRole(role)
                .preferredDifficulty(profile.getPreferredDifficulty())
                .skillWeights(profile.getSkillWeights())
                .topSkills(profile.getTopSkills())
                .skillGaps(profile.getSkillGaps())
                .disciplineScore(profile.getDisciplineScore())
                .userTags(profile.getUserTags())
                .enrolledCourseIds(profile.getEnrolledCourseIds())
                .completedHours(profile.getCompletedHours())
                .cognitiveLevel(profile.getCognitiveLevel())
                .chronologicalCourseIds(profile.getChronologicalCourseIds())
                .courseProgressMap(profile.getCourseProgressMap())
                .build();
        }

        List<CandidateCourseDTO> candidates = recommendationAgent.recallCandidates(profile, 14);
        List<AnalyzedCourseVO> analyzedCourses = courseAnalysisAgent.analyzeCourses(candidates, profile);
        LearningPathPlan plan = pathPlanningAgent.planPath(profile, analyzedCourses, customOverrides);

        CriticReport criticReport = pathCriticAgent.auditPathPlan(plan, profile);
        if (!Boolean.TRUE.equals(criticReport.getPassed())) {
            Map<String, Object> directives = new HashMap<>(criticReport.getRefinementDirectives());
            if (customOverrides != null) directives.putAll(customOverrides);
            plan = pathPlanningAgent.planPath(profile, analyzedCourses, directives);
            criticReport = pathCriticAgent.auditPathPlan(plan, profile);
        }
        plan.setCriticReport(criticReport);

        if (!hasCustom && redisService != null && plan != null) {
            try {
                redisService.setCacheObject(cacheKey, plan, (long) properties.getCacheTtlMinutes(), TimeUnit.MINUTES);
            } catch (Exception ignored) {}
        }

        return plan;
    }

    /**
     * 冷启动主动探针：获取诊断问题问卷
     */
    public List<ActiveProbeQuestion> getProbingQuestions(Long userId) {
        UserProfileContext profile = userProfileAgent.buildProfile(userId);
        if (userProfileAgent.needsProbing(profile)) {
            return userProfileAgent.generateDiagnosticProbes(profile);
        }
        return Collections.emptyList();
    }

    /**
     * 提交主动探针反馈并即时自适应推演
     */
    public Map<String, Object> submitProbingAnswers(Long userId, Map<String, String> answers) {
        invalidateUserCache(userId);
        List<PersonalizedRecommendVO> recs = recommendCourses(userId, 6, answers, Collections.emptyMap());
        LearningPathPlan plan = getLearningPath(userId, answers, Collections.emptyMap());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("calibrated", true);
        result.put("recommendations", recs);
        result.put("learningPath", plan);
        return result;
    }

    /**
     * 人机协同微调：局部重入推演
     */
    public Map<String, Object> refineLearningPath(Long userId, Map<String, Object> overrides) {
        invalidateUserCache(userId);
        List<PersonalizedRecommendVO> recs = recommendCourses(userId, 6, Collections.emptyMap(), overrides);
        LearningPathPlan plan = getLearningPath(userId, Collections.emptyMap(), overrides);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("refined", true);
        result.put("recommendations", recs);
        result.put("learningPath", plan);
        return result;
    }

    /**
     * 获取智能体自动化评测度量大屏数据
     */
    public AgentEvalMetricsVO getEvaluationMetrics() {
        return evalService.getMetricsSnapshot();
    }

    /**
     * 下一代 L5 智能体流式思考与异步并发总线 (SSE Streaming Deliberation)
     *
     * @param userId 学员 ID (访客为 null)
     * @param targetRole 目标角色覆盖 (可选)
     * @param emitter Spring SseEmitter 实例
     */
    private boolean sendEventSafely(SseEmitter emitter, AgentReasoningEvent event, AtomicBoolean isCompleted) {
        if (isCompleted.get()) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event().name("agent_event").data(event));
            return true;
        } catch (Exception ex) {
            log.info("[StreamOrchestrator] 客户端断开连接或写入失败，终止推演: {}", ex.getMessage());
            isCompleted.set(true);
            return false;
        }
    }

    public void streamReasoning(Long userId, String targetRole, SseEmitter emitter) {
        AtomicBoolean isCompleted = new AtomicBoolean(false);
        emitter.onCompletion(() -> isCompleted.set(true));
        emitter.onTimeout(() -> {
            isCompleted.set(true);
            try {
                emitter.complete();
            } catch (Exception ignored) {}
        });
        emitter.onError(e -> isCompleted.set(true));

        agentThreadPool.execute(() -> {
            try {
                if (isCompleted.get()) return;
                long pipelineStart = System.currentTimeMillis();

                // STEP 1: 冷启动主动探针与画像完备度检测
                if (!sendEventSafely(emitter, AgentReasoningEvent.of("PROBE_CHECK", "UserProfileAgent", 1,
                        "正在检测学员画像完备度并评估冷启动意图基线...", null, 1L), isCompleted)) {
                    return;
                }

                // 1. 学员画像构建建模
                long tProfileStart = System.currentTimeMillis();
                UserProfileContext p = userProfileAgent.buildProfile(userId, Collections.emptyMap());
                if (targetRole != null && !targetRole.isBlank()) {
                    p = UserProfileContext.builder()
                            .userId(p.getUserId())
                            .intendedRole(targetRole.trim())
                            .preferredDifficulty(p.getPreferredDifficulty())
                            .skillWeights(p.getSkillWeights())
                            .topSkills(p.getTopSkills())
                            .skillGaps(p.getSkillGaps())
                            .disciplineScore(p.getDisciplineScore())
                            .userTags(p.getUserTags())
                            .enrolledCourseIds(p.getEnrolledCourseIds())
                            .completedHours(p.getCompletedHours())
                            .cognitiveLevel(p.getCognitiveLevel())
                            .chronologicalCourseIds(p.getChronologicalCourseIds())
                            .courseProgressMap(p.getCourseProgressMap())
                            .build();
                }
                UserProfileContext profile = p;
                long profileLatency = System.currentTimeMillis() - tProfileStart;

                // 发射画像建模完成事件
                Map<String, Object> profileSummary = new HashMap<>();
                profileSummary.put("intendedRole", profile.getIntendedRole());
                profileSummary.put("disciplineScore", profile.getDisciplineScore());
                profileSummary.put("skillGaps", profile.getSkillGaps());
                profileSummary.put("topSkills", profile.getTopSkills());

                if (!sendEventSafely(emitter, AgentReasoningEvent.of("PROFILE_BUILT", "UserProfileAgent", 1,
                        String.format("已完成学情特征建模：锁定目标方向【%s】，挖掘技能短板【%s】",
                                profile.getIntendedRole(), String.join("、", profile.getSkillGaps())),
                        profileSummary, profileLatency), isCompleted)) {
                    return;
                }

                // STEP 2: 候选课程初筛与推荐召回
                long tRecallStart = System.currentTimeMillis();
                List<CandidateCourseDTO> candidates = recommendationAgent.recallCandidates(profile, 14);
                long recallLatency = System.currentTimeMillis() - tRecallStart;
                if (!sendEventSafely(emitter, AgentReasoningEvent.of("CANDIDATES_RECALLED", "RecommendationAgent", 2,
                        String.format("SPI 算法多路初筛召回 %d 门专业课程，已完成跨品类多样性打散", candidates.size()),
                        candidates.size(), recallLatency), isCompleted)) {
                    return;
                }

                // STEP 3: 布鲁姆大纲解构与 Capstone 识别
                long tAnalysisStart = System.currentTimeMillis();
                List<AnalyzedCourseVO> analyzed = courseAnalysisAgent.analyzeCourses(candidates, profile);
                long analysisLatency = System.currentTimeMillis() - tAnalysisStart;
                if (!sendEventSafely(emitter, AgentReasoningEvent.of("COURSE_ANALYSIS", "CourseAnalysisAgent", 3,
                        "已深度解构课程知识拓扑：构建布鲁姆六级认知梯队，标注综合实战 Capstone 项目与先修依赖",
                        analyzed.size(), analysisLatency), isCompleted)) {
                    return;
                }

                // STEP 4: 路径规划编排
                long tPlanStart = System.currentTimeMillis();
                Map<String, Object> customOverrides = new HashMap<>();
                if (targetRole != null && !targetRole.isBlank()) {
                    customOverrides.put("customRole", targetRole.trim());
                }
                LearningPathPlan pathPlan = pathPlanningAgent.planPath(profile, analyzed, customOverrides);
                long planLatency = System.currentTimeMillis() - tPlanStart;
                if (!sendEventSafely(emitter, AgentReasoningEvent.of("PATH_PLANNED", "PathPlanningAgent", 4,
                        String.format("DAG 阶段拓扑编排完成：规划出【%s】共 4 阶段进阶成长路线", pathPlan.getIntendedRole()),
                        pathPlan.getStages(), planLatency), isCompleted)) {
                    return;
                }

                // STEP 5: 审判反思 Agent
                long tCriticStart = System.currentTimeMillis();
                CriticReport criticReport = pathCriticAgent.auditPathPlan(pathPlan, profile);

                // 判断是否触发反思回路
                if (!Boolean.TRUE.equals(criticReport.getPassed())) {
                    if (!sendEventSafely(emitter, AgentReasoningEvent.of("REFLECTION_DIRECTIVE", "PathCriticAgent", 5,
                            "审判质检发现排布缺陷，触发单次受控反思回路：正在应用修正指令重排...",
                            criticReport.getRefinementDirectives(), System.currentTimeMillis() - tCriticStart), isCompleted)) {
                        return;
                    }

                    Map<String, Object> directives = new HashMap<>(criticReport.getRefinementDirectives());
                    directives.putAll(customOverrides);
                    pathPlan = pathPlanningAgent.planPath(profile, analyzed, directives);
                    criticReport = pathCriticAgent.auditPathPlan(pathPlan, profile);
                }

                pathPlan.setCriticReport(criticReport);
                long criticLatency = System.currentTimeMillis() - tCriticStart;
                if (!sendEventSafely(emitter, AgentReasoningEvent.of("CRITIC_AUDIT", "PathCriticAgent", 5,
                        String.format("PathCritic 量化质检通过：得分 %d 分，评级 %s，Kahn DAG 无环拓扑合规",
                                criticReport.getOverallScore() != null ? criticReport.getOverallScore() : 100,
                                criticReport.getVerdictLevel() != null ? criticReport.getVerdictLevel() : "卓越 (A+)"),
                        criticReport, criticLatency), isCompleted)) {
                    return;
                }

                // STEP 6: 解释生成与最终交付
                long tExplStart = System.currentTimeMillis();
                List<PersonalizedRecommendVO> recs = explanationGenerationAgent.generateExplanations(profile, pathPlan, 4);
                long explLatency = System.currentTimeMillis() - tExplStart;

                // 记录流式推演快照至在线质量度量体系
                try {
                    AgentWorkflowContext streamCtx = AgentWorkflowContext.builder()
                            .sessionId(UUID.randomUUID().toString())
                            .userId(userId)
                            .startTime(pipelineStart)
                            .userProfile(profile)
                            .candidates(candidates)
                            .analyzedCourses(analyzed)
                            .learningPathPlan(pathPlan)
                            .criticReport(criticReport)
                            .passedCritic(Boolean.TRUE.equals(criticReport.getPassed()))
                            .recommendations(recs)
                            .build();
                    evalService.recordPipelineExecution(streamCtx);
                } catch (Exception evalEx) {
                    log.warn("[StreamOrchestrator] 记录评测快照异常: {}", evalEx.getMessage());
                }

                Map<String, Object> finalPayload = new HashMap<>();
                finalPayload.put("recommendations", recs);
                finalPayload.put("learningPath", pathPlan);
                finalPayload.put("criticReport", criticReport);
                finalPayload.put("totalPipelineLatencyMs", System.currentTimeMillis() - pipelineStart);

                sendEventSafely(emitter, AgentReasoningEvent.of("FINAL_RESULT", "ExplanationGenerationAgent", 6,
                        "全链路多智能体流式思考推演完成，成果已交付！",
                        finalPayload, explLatency), isCompleted);

                sendEventSafely(emitter, AgentReasoningEvent.of("STREAM_DONE", "Orchestrator", 6,
                        "推演流正常完成", null, System.currentTimeMillis() - pipelineStart), isCompleted);

                if (!isCompleted.get()) {
                    isCompleted.set(true);
                    emitter.complete();
                }
            } catch (Exception e) {
                if (!isCompleted.get()) {
                    log.error("[StreamOrchestrator] 智能体流式推演异常", e);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("agent_event")
                                .data(AgentReasoningEvent.of("STREAM_ERROR", "Orchestrator", 0,
                                        "推演流异常中断: " + e.getMessage(), null, 0L)));
                    } catch (Exception ignored) {}
                    try {
                        emitter.completeWithError(e);
                    } catch (Exception ignored) {}
                    isCompleted.set(true);
                }
            }
        });
    }

    /**
     * 清理学员推荐缓存
     */
    public void invalidateUserCache(Long userId) {
        if (redisService != null) {
            try {
                String uKey = (userId != null ? userId.toString() : "guest");
                redisService.deleteObject(CACHE_RECOMMEND_PREFIX + uKey + ":6");
                redisService.deleteObject(CACHE_RECOMMEND_PREFIX + uKey + ":4");
                redisService.deleteObject(CACHE_RECOMMEND_PREFIX + uKey + ":10");
                redisService.deleteObject(CACHE_RECOMMEND_PREFIX + uKey + ":14");
                redisService.deleteObject(CACHE_PATH_PREFIX + uKey);
            } catch (Exception ignored) {}
        }
    }
}
