package com.share.education.ai.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.education.ai.algorithm.IRecommendAlgorithmEngine;
import com.share.education.ai.model.AlgorithmCandidateDTO;
import com.share.education.ai.model.CandidateCourseDTO;
import com.share.education.ai.model.UserProfileContext;
import com.share.education.domain.EduCategory;
import com.share.education.domain.EduCourse;
import com.share.education.mapper.EduCategoryMapper;
import com.share.education.mapper.EduCourseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能体 2：推荐 Agent (RecommendationAgent)。
 *
 * <p>【核心职责与算法插槽】：
 * 1. 负责调用算法实验层 SPI (IRecommendAlgorithmEngine) 召回核心候选集（支持用户训练好的算法无缝替换）；
 * 2. 结合分类多样性 (Diversity)、排除已购课程、冷启动保护等多路召回机制；
 * 3. 产出结构化的候选课程集合，输入给下一环“课程分析 Agent”。</p>
 */
@Component
public class RecommendationAgent {

    private static final Logger log = LoggerFactory.getLogger(RecommendationAgent.class);

    private final IRecommendAlgorithmEngine algorithmEngine;
    private final EduCourseMapper courseMapper;
    private final EduCategoryMapper categoryMapper;

    public RecommendationAgent(IRecommendAlgorithmEngine algorithmEngine,
                               EduCourseMapper courseMapper,
                               EduCategoryMapper categoryMapper) {
        this.algorithmEngine = algorithmEngine;
        this.courseMapper = courseMapper;
        this.categoryMapper = categoryMapper;
    }

    /**
     * 推荐候选集召回与多路打散
     *
     * @param profile 学员画像上下文
     * @param targetCount 期望初筛候选数量 (通常为 8 ~ 12 门，供后续路径规划与分析)
     * @return 结构化候选课程列表
     */
    public List<CandidateCourseDTO> recallCandidates(UserProfileContext profile, int targetCount) {
        int recallPoolSize = Math.max(targetCount * 2, 20);

        // 1. 调用算法引擎 SPI 召回首批候选 (包含用户自研模型的打分)
        List<AlgorithmCandidateDTO> algoCandidates = algorithmEngine.recallCandidates(
            profile.getUserId(), profile, recallPoolSize
        );

        if (algoCandidates == null || algoCandidates.isEmpty()) {
            log.warn("算法引擎 [{}] 召回为空，启用系统兜底召回", algorithmEngine.getEngineName());
            return fallbackRecall(profile, targetCount);
        }

        // 2. 批量加载课程与分类详情
        List<Long> courseIds = algoCandidates.stream()
            .map(AlgorithmCandidateDTO::getCourseId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        Map<Long, EduCourse> courseMap = courseMapper.selectBatchIds(courseIds).stream()
            .collect(Collectors.toMap(EduCourse::getId, c -> c, (a, b) -> a));

        Map<Long, String> categoryMap = categoryMapper.selectList(new LambdaQueryWrapper<EduCategory>()).stream()
            .collect(Collectors.toMap(EduCategory::getId, EduCategory::getCategoryName, (a, b) -> a));

        // 3. 组装候选 DTO 并进行多样性打散 (同一大类不超过 2 门，保证视野宽阔)
        List<CandidateCourseDTO> result = new ArrayList<>();
        Map<Long, Integer> categoryCount = new HashMap<>();

        for (AlgorithmCandidateDTO ac : algoCandidates) {
            EduCourse c = courseMap.get(ac.getCourseId());
            if (c == null || c.getStatus() == null || c.getStatus() != 1) {
                continue;
            }

            Long catId = c.getCategoryId();
            int currentCatCount = categoryCount.getOrDefault(catId, 0);

            // 多样性控制：单门类不超过 3 门
            if (currentCatCount >= 3 && result.size() < targetCount) {
                continue;
            }

            categoryCount.put(catId, currentCatCount + 1);

            result.add(CandidateCourseDTO.builder()
                .courseId(c.getId())
                .courseName(c.getCourseName())
                .coverUrl(c.getCoverUrl())
                .categoryId(catId)
                .categoryName(categoryMap.getOrDefault(catId, "前沿技术"))
                .price(c.getPrice() != null ? c.getPrice().longValue() : 0L)
                .originalPrice(c.getOriginalPrice() != null ? c.getOriginalPrice().longValue() : 0L)
                .teacherName("智问教研团队")
                .difficultyLevel(c.getDifficultyLevel() != null ? c.getDifficultyLevel() : 2)
                .skills(c.getSkills())
                .targetRole(c.getTargetRole())
                .learnerCount(c.getLearnerCount() != null ? c.getLearnerCount() : 0)
                .algorithmScore(ac.getScore())
                .recallChannel(algorithmEngine.getEngineName())
                .build());

            if (result.size() >= targetCount) {
                break;
            }
        }

        return result;
    }

    private List<CandidateCourseDTO> fallbackRecall(UserProfileContext profile, int targetCount) {
        List<EduCourse> hots = courseMapper.selectList(
            new LambdaQueryWrapper<EduCourse>()
                .eq(EduCourse::getStatus, 1)
                .orderByDesc(EduCourse::getLearnerCount)
                .last("LIMIT " + targetCount)
        );

        return hots.stream().map(c -> CandidateCourseDTO.builder()
            .courseId(c.getId())
            .courseName(c.getCourseName())
            .coverUrl(c.getCoverUrl())
            .categoryId(c.getCategoryId())
            .categoryName("热门好课")
            .price(c.getPrice() != null ? c.getPrice().longValue() : 0L)
            .originalPrice(c.getOriginalPrice() != null ? c.getOriginalPrice().longValue() : 0L)
            .teacherName("智问教研团队")
            .difficultyLevel(c.getDifficultyLevel() != null ? c.getDifficultyLevel() : 2)
            .skills(c.getSkills())
            .targetRole(c.getTargetRole())
            .learnerCount(c.getLearnerCount() != null ? c.getLearnerCount() : 0)
            .algorithmScore(88.0)
            .recallChannel("FALLBACK_POPULAR")
            .build()
        ).collect(Collectors.toList());
    }
}
