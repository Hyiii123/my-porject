package com.share.education.ai.orchestrator;

import com.share.common.redis.service.RedisService;
import com.share.education.ai.agent.UserProfileAgent;
import com.share.education.ai.composite.engine.CompositeMultiAgentGraphEngine;
import com.share.education.ai.composite.state.DebateBlackboardState;
import com.share.education.ai.config.AiRecommendProperties;
import com.share.education.ai.evals.AgentEvalMetricsVO;
import com.share.education.ai.evals.AgentEvaluationService;
import com.share.education.ai.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 复合多智能体协同导学总编排器 (MultiAgentRecommendOrchestrator)。
 * <p>全面采用 Spring AI Alibaba Graph 架构模式重构，统一由 CompositeMultiAgentGraphEngine 驱动：
 * <ul>
 *   <li>模式 2: 动态意图路由分发 (IntentDispatcherNode)</li>
 *   <li>模式 5: 专家圆桌博弈与共识达成 (PedagogyMentorNode vs IndustryArchitectNode & ConsensusArbiterNode)</li>
 *   <li>模式 4: 审判质检法官与 Kahn DAG 认知阶梯反思闭环 (PathCriticNode)</li>
 *   <li>交付层: 方案合成与证据链接地可解释性推理 (ExplanationSynthesisNode)</li>
 * </ul>
 * 保持与前端 AgentReasoningHUD.vue 流式思考心流与后端 Feign 接口 100% 向后兼容。
 */
@Service
public class MultiAgentRecommendOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(MultiAgentRecommendOrchestrator.class);

    private static final String CACHE_RECOMMEND_PREFIX = "edu:ai:recommend:";
    private static final String CACHE_PATH_PREFIX = "edu:ai:path:";

    private final CompositeMultiAgentGraphEngine compositeGraphEngine;
    private final UserProfileAgent userProfileAgent;
    private final AgentEvaluationService evalService;
    private final RedisService redisService;
    private final AiRecommendProperties properties;

    public MultiAgentRecommendOrchestrator(CompositeMultiAgentGraphEngine compositeGraphEngine,
                                          UserProfileAgent userProfileAgent,
                                          AgentEvaluationService evalService,
                                          RedisService redisService,
                                          AiRecommendProperties properties) {
        this.compositeGraphEngine = compositeGraphEngine;
        this.userProfileAgent = userProfileAgent;
        this.evalService = evalService;
        this.redisService = redisService;
        this.properties = properties;
    }

    /**
     * 运行完整复合多智能体流水线，产出高契合度个性化推荐课程流
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

        // 2. 构造共享辩论黑板状态机载体
        String targetRole = extractTargetRole(customOverrides);

        DebateBlackboardState state = DebateBlackboardState.builder()
            .sessionId(UUID.randomUUID().toString())
            .userId(userId)
            .intendedRole(targetRole != null && !targetRole.isBlank() ? targetRole.trim() : "Java全栈架构师")
            .probeAnswers(probeAnswers != null ? probeAnswers : Collections.emptyMap())
            .customOverrides(customOverrides != null ? customOverrides : Collections.emptyMap())
            .build();

        // 3. 执行复合多智能体图引擎协同计算
        DebateBlackboardState finalState = compositeGraphEngine.runWorkflow(state, safeLimit);
        List<PersonalizedRecommendVO> result = finalState.getFinalRecommendations();

        // 4. 写入缓存 (仅在无自定义微调时)
        if (!hasCustomParams && redisService != null && result != null && !result.isEmpty()) {
            try {
                redisService.setCacheObject(cacheKey, result, (long) properties.getCacheTtlMinutes(), TimeUnit.MINUTES);
            } catch (Exception ignored) {}
        }

        return result != null ? result : Collections.emptyList();
    }

    /**
     * 获取带有终审质检报告的学员专属结构化全景学习成长进阶路径规划
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

        String targetRole = extractTargetRole(customOverrides);

        DebateBlackboardState state = DebateBlackboardState.builder()
            .sessionId(UUID.randomUUID().toString())
            .userId(userId)
            .intendedRole(targetRole != null && !targetRole.isBlank() ? targetRole.trim() : "Java全栈架构师")
            .probeAnswers(probeAnswers != null ? probeAnswers : Collections.emptyMap())
            .customOverrides(customOverrides != null ? customOverrides : Collections.emptyMap())
            .build();

        DebateBlackboardState finalState = compositeGraphEngine.runWorkflow(state, 6);
        LearningPathPlan plan = finalState.getCurrentDraftPlan();

        if (!hasCustom && redisService != null && plan != null) {
            try {
                redisService.setCacheObject(cacheKey, plan, (long) properties.getCacheTtlMinutes(), TimeUnit.MINUTES);
            } catch (Exception ignored) {}
        }

        return plan;
    }

    /**
     * 统一编排入口：单次图计算完成课程推荐与成长进阶路线规划
     */
    public Map<String, Object> orchestrate(Long userId, String targetRole, Integer limit) {
        return orchestrate(userId, targetRole, limit, null);
    }

    public Map<String, Object> orchestrate(Long userId, String targetRole, Integer limit, Integer difficulty) {
        return orchestrate(userId, targetRole, limit, difficulty, null);
    }

    public Map<String, Object> orchestrate(Long userId, String targetRole, Integer limit, Integer difficulty, String query) {
        int safeLimit = limit != null && limit > 0 ? Math.min(limit, 10) : 4;
        Map<String, Object> overrides = new LinkedHashMap<>();
        if (targetRole != null && !targetRole.isBlank()) {
            overrides.put("targetRole", targetRole.trim());
            overrides.put("customRole", targetRole.trim());
        }
        if (query != null && !query.isBlank()) {
            overrides.put("query", query.trim());
            overrides.put("userPrompt", query.trim());
        }
        if (difficulty != null && difficulty > 0) {
            overrides.put("preferredDifficulty", difficulty);
            overrides.put("difficulty", difficulty);
        } else if (targetRole != null && (targetRole.contains("实习") || targetRole.contains("校招") || targetRole.contains("初级") || targetRole.contains("入门"))) {
            overrides.put("preferredDifficulty", 1);
            overrides.put("difficulty", 1);
        }

        String intendedRole = targetRole != null && !targetRole.isBlank() ? targetRole.trim() : "Java全栈架构师";

        DebateBlackboardState state = DebateBlackboardState.builder()
            .sessionId(UUID.randomUUID().toString())
            .userId(userId)
            .intendedRole(intendedRole)
            .probeAnswers(Collections.emptyMap())
            .customOverrides(overrides)
            .build();

        DebateBlackboardState finalState = compositeGraphEngine.runWorkflow(state, safeLimit);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("targetRole", intendedRole);
        result.put("recommendations", finalState.getFinalRecommendations());
        result.put("learningPath", finalState.getCurrentDraftPlan());
        if (finalState.getCriticReport() != null) {
            result.put("criticReport", finalState.getCriticReport());
        }
        if (finalState.getConsensusSummary() != null) {
            result.put("consensusSummary", finalState.getConsensusSummary());
        }
        if (finalState.getDialogueTurns() != null) {
            result.put("dialogueTurns", finalState.getDialogueTurns());
        }
        return result;
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
        DebateBlackboardState state = DebateBlackboardState.builder()
            .sessionId(UUID.randomUUID().toString())
            .userId(userId)
            .intendedRole("Java全栈架构师")
            .probeAnswers(answers != null ? answers : Collections.emptyMap())
            .customOverrides(Collections.emptyMap())
            .build();

        DebateBlackboardState finalState = compositeGraphEngine.runWorkflow(state, 6);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("calibrated", true);
        result.put("recommendations", finalState.getFinalRecommendations());
        result.put("learningPath", finalState.getCurrentDraftPlan());
        return result;
    }

    /**
     * 人机协同微调：局部重入推演
     */
    public Map<String, Object> refineLearningPath(Long userId, Map<String, Object> overrides) {
        invalidateUserCache(userId);
        String targetRole = extractTargetRole(overrides);
        DebateBlackboardState state = DebateBlackboardState.builder()
            .sessionId(UUID.randomUUID().toString())
            .userId(userId)
            .intendedRole(targetRole != null && !targetRole.isBlank() ? targetRole.trim() : "Java全栈架构师")
            .probeAnswers(Collections.emptyMap())
            .customOverrides(overrides != null ? overrides : Collections.emptyMap())
            .build();

        DebateBlackboardState finalState = compositeGraphEngine.runWorkflow(state, 6);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("refined", true);
        result.put("recommendations", finalState.getFinalRecommendations());
        result.put("learningPath", finalState.getCurrentDraftPlan());
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
    public void streamReasoning(Long userId, String targetRole, SseEmitter emitter) {
        streamReasoning(userId, targetRole, null, emitter);
    }

    public void streamReasoning(Long userId, String targetRole, String query, SseEmitter emitter) {
        Map<String, Object> customOverrides = new HashMap<>();
        if (targetRole != null && !targetRole.isBlank()) {
            customOverrides.put("customRole", targetRole.trim());
            customOverrides.put("targetRole", targetRole.trim());
        }
        if (query != null && !query.isBlank()) {
            customOverrides.put("query", query.trim());
            customOverrides.put("userPrompt", query.trim());
        }

        String intendedRole = targetRole != null && !targetRole.isBlank() ? targetRole.trim() : "Java全栈架构师";

        DebateBlackboardState state = DebateBlackboardState.builder()
            .sessionId(UUID.randomUUID().toString())
            .userId(userId)
            .intendedRole(intendedRole)
            .probeAnswers(Collections.emptyMap())
            .customOverrides(customOverrides)
            .build();

        compositeGraphEngine.streamWorkflow(state, 4, emitter);
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
                redisService.deleteObject(CACHE_RECOMMEND_PREFIX + uKey + ":20");
                redisService.deleteObject(CACHE_PATH_PREFIX + uKey);
            } catch (Exception ignored) {}
        }
    }

    private String extractTargetRole(Map<String, Object> customOverrides) {
        if (customOverrides == null) {
            return null;
        }
        if (customOverrides.containsKey("customRole") && customOverrides.get("customRole") != null) {
            return customOverrides.get("customRole").toString();
        }
        if (customOverrides.containsKey("targetRole") && customOverrides.get("targetRole") != null) {
            return customOverrides.get("targetRole").toString();
        }
        return null;
    }
}

