package com.share.education.ai.orchestrator;

import com.share.common.redis.service.RedisService;
import com.share.education.ai.agent.*;
import com.share.education.ai.config.AiRecommendProperties;
import com.share.education.ai.evals.AgentEvalMetricsVO;
import com.share.education.ai.evals.AgentEvaluationService;
import com.share.education.ai.model.*;
import com.share.education.ai.workflow.AgentWorkflowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 下一代动态自省多智能体协同导学总编排器 (MultiAgentRecommendOrchestrator)。
 *
 * <p>【架构工作流驱动 (Workflow Execution Engine - L4 闭环架构)】：
 * <pre>
 *   学员/访客
 *      ↓
 *   主动探针 Agent (ActiveProbingAgent, 完备度 < 0.4 时自适应探针)
 *      ↓
 *   用户画像 Agent (UserProfileAgent, 50维画像建模)
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
    private final ActiveProbingAgent probingAgent;
    private final AgentEvaluationService evalService;
    private final RedisService redisService;
    private final AiRecommendProperties properties;

    public MultiAgentRecommendOrchestrator(UserProfileAgent userProfileAgent,
                                          RecommendationAgent recommendationAgent,
                                          CourseAnalysisAgent courseAnalysisAgent,
                                          PathPlanningAgent pathPlanningAgent,
                                          PathCriticAgent pathCriticAgent,
                                          ExplanationGenerationAgent explanationGenerationAgent,
                                          ActiveProbingAgent probingAgent,
                                          AgentEvaluationService evalService,
                                          RedisService redisService,
                                          AiRecommendProperties properties) {
        this.userProfileAgent = userProfileAgent;
        this.recommendationAgent = recommendationAgent;
        this.courseAnalysisAgent = courseAnalysisAgent;
        this.pathPlanningAgent = pathPlanningAgent;
        this.pathCriticAgent = pathCriticAgent;
        this.explanationGenerationAgent = explanationGenerationAgent;
        this.probingAgent = probingAgent;
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
        if (probingAgent.needsProbing(profile)) {
            return probingAgent.generateDiagnosticProbes(profile);
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
