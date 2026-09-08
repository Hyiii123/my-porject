package com.share.education.ai.orchestrator;

import com.share.common.redis.service.RedisService;
import com.share.education.ai.agent.*;
import com.share.education.ai.config.AiRecommendProperties;
import com.share.education.ai.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 多智能体推荐与路径规划总编排器 (MultiAgentRecommendOrchestrator)。
 *
 * <p>【架构工作流驱动 (Workflow Execution Engine)】：
 * 按照多智能体协同流水线严格顺序执行：
 * <pre>
 *   用户 ➔ 用户画像Agent ➔ 推荐Agent (集成自研算法SPI)
 *               ↓
 *          课程分析Agent
 *               ↓
 *          路径规划Agent
 *               ↓
 *              RAG
 *               ↓
 *          解释生成Agent
 *               ↓
 *           个性化推荐
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
    private final ExplanationGenerationAgent explanationGenerationAgent;
    private final RedisService redisService;
    private final AiRecommendProperties properties;

    public MultiAgentRecommendOrchestrator(UserProfileAgent userProfileAgent,
                                          RecommendationAgent recommendationAgent,
                                          CourseAnalysisAgent courseAnalysisAgent,
                                          PathPlanningAgent pathPlanningAgent,
                                          ExplanationGenerationAgent explanationGenerationAgent,
                                          RedisService redisService,
                                          AiRecommendProperties properties) {
        this.userProfileAgent = userProfileAgent;
        this.recommendationAgent = recommendationAgent;
        this.courseAnalysisAgent = courseAnalysisAgent;
        this.pathPlanningAgent = pathPlanningAgent;
        this.explanationGenerationAgent = explanationGenerationAgent;
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
        int safeLimit = Math.max(1, Math.min(limit, 20));

        // 1. 尝试从 Redis 缓存获取，保障毫秒级瞬时响应
        String cacheKey = CACHE_RECOMMEND_PREFIX + (userId != null ? userId : "guest") + ":" + safeLimit;
        if (redisService != null) {
            try {
                List<PersonalizedRecommendVO> cached = redisService.getCacheObject(cacheKey);
                if (cached != null && !cached.isEmpty()) {
                    return cached;
                }
            } catch (Exception ignored) {}
        }

        long start = System.currentTimeMillis();

        // 步骤 1: 用户画像 Agent 提取多维学情与技能画像
        UserProfileContext profile = userProfileAgent.buildProfile(userId);

        // 步骤 2: 推荐 Agent 调度算法引擎 SPI 进行多路初排召回 (预选 12 门供分析筛选)
        List<CandidateCourseDTO> candidates = recommendationAgent.recallCandidates(profile, 12);

        // 步骤 3: 课程分析 Agent 深度拆解大纲知识拓扑与先修依赖
        List<AnalyzedCourseVO> analyzedCourses = courseAnalysisAgent.analyzeCourses(candidates, profile);

        // 步骤 4: 路径规划 Agent 进行阶段化进阶路线拓扑编排
        LearningPathPlan pathPlan = pathPlanningAgent.planPath(profile, analyzedCourses);

        // 步骤 5: 解释生成 Agent 融合 RAG 行业标准，生成可解释性推荐理由
        List<PersonalizedRecommendVO> result = explanationGenerationAgent.generateExplanations(profile, pathPlan, safeLimit);

        long cost = System.currentTimeMillis() - start;
        log.info("多智能体个性化推荐流水线执行完成，用户: {}, 耗时: {}ms, 推荐条数: {}", userId, cost, result.size());

        // 写入缓存
        if (redisService != null && !result.isEmpty()) {
            try {
                redisService.setCacheObject(cacheKey, result, (long) properties.getCacheTtlMinutes(), TimeUnit.MINUTES);
            } catch (Exception ignored) {}
        }

        return result;
    }

    /**
     * 为学员生成结构化的完整全景学习成长路径规划 (学习路线规划图)
     *
     * @param userId 学员用户 ID
     * @return 包含各个进阶阶段与课程链条的路线图
     */
    public LearningPathPlan getLearningPath(Long userId) {
        String cacheKey = CACHE_PATH_PREFIX + (userId != null ? userId : "guest");
        if (redisService != null) {
            try {
                LearningPathPlan cached = redisService.getCacheObject(cacheKey);
                if (cached != null) {
                    return cached;
                }
            } catch (Exception ignored) {}
        }

        UserProfileContext profile = userProfileAgent.buildProfile(userId);
        List<CandidateCourseDTO> candidates = recommendationAgent.recallCandidates(profile, 12);
        List<AnalyzedCourseVO> analyzedCourses = courseAnalysisAgent.analyzeCourses(candidates, profile);
        LearningPathPlan plan = pathPlanningAgent.planPath(profile, analyzedCourses);

        if (redisService != null && plan != null) {
            try {
                redisService.setCacheObject(cacheKey, plan, (long) properties.getCacheTtlMinutes(), TimeUnit.MINUTES);
            } catch (Exception ignored) {}
        }

        return plan;
    }

    /**
     * 清理学员推荐缓存 (当学员更新画像偏好或完成新课程时调用)
     */
    public void invalidateUserCache(Long userId) {
        if (userId != null && redisService != null) {
            try {
                redisService.deleteObject(CACHE_RECOMMEND_PREFIX + userId + ":6");
                redisService.deleteObject(CACHE_RECOMMEND_PREFIX + userId + ":4");
                redisService.deleteObject(CACHE_RECOMMEND_PREFIX + userId + ":10");
                redisService.deleteObject(CACHE_PATH_PREFIX + userId);
            } catch (Exception ignored) {}
        }
    }
}
