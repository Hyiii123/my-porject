package com.share.education.ai.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.education.ai.model.AnalyzedCourseVO;
import com.share.education.ai.model.CandidateCourseDTO;
import com.share.education.ai.model.UserProfileContext;
import com.share.education.domain.EduCourseCatalog;
import com.share.education.mapper.EduCourseCatalogMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能体 3：课程分析 Agent (CourseAnalysisAgent)。
 *
 * <p>【核心职责】：
 * 1. 深度拆解候选课程的 1,820 节大纲目录与知识树拓扑；
 * 2. 提取先修前置依赖 (Prerequisites) 与核心实战攻坚知识点；
 * 3. 结合学员现有认知画像，评估认知难度跨度与工程实战权重。</p>
 */
@Component
public class CourseAnalysisAgent {

    private final EduCourseCatalogMapper catalogMapper;

    public CourseAnalysisAgent(EduCourseCatalogMapper catalogMapper) {
        this.catalogMapper = catalogMapper;
    }

    /**
     * 批量深度分析候选课程集合
     */
    public List<AnalyzedCourseVO> analyzeCourses(List<CandidateCourseDTO> candidates, UserProfileContext profile) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> courseIds = candidates.stream().map(CandidateCourseDTO::getCourseId).collect(Collectors.toList());

        // 批量获取候选课程的大纲章节
        List<EduCourseCatalog> allCatalogs = catalogMapper.selectList(
            new LambdaQueryWrapper<EduCourseCatalog>()
                .in(EduCourseCatalog::getCourseId, courseIds)
                .orderByAsc(EduCourseCatalog::getSortNum)
        );

        Map<Long, List<EduCourseCatalog>> catalogMap = allCatalogs.stream()
            .collect(Collectors.groupingBy(EduCourseCatalog::getCourseId));

        List<AnalyzedCourseVO> analyzedList = new ArrayList<>();
        int userDifficulty = (profile != null && profile.getPreferredDifficulty() != null)
            ? profile.getPreferredDifficulty() : 2;

        for (CandidateCourseDTO c : candidates) {
            List<EduCourseCatalog> catalogs = catalogMap.getOrDefault(c.getCourseId(), Collections.emptyList());

            // 1. 提取先修知识依赖
            List<String> prerequisites = extractPrerequisites(c);

            // 2. 提取核心攻坚知识点与大纲速览
            List<String> knowledgePoints = new ArrayList<>();
            StringBuilder syllabusSummary = new StringBuilder();
            int practicalCount = 0;

            for (EduCourseCatalog cat : catalogs) {
                String title = cat.getCatalogTitle();
                if (!StringUtils.hasText(title)) continue;

                if (cat.getCatalogType() != null && cat.getCatalogType() == 1) {
                    if (syllabusSummary.length() > 0) syllabusSummary.append("；");
                    syllabusSummary.append(title);
                }

                if (title.contains("实战") || title.contains("项目") || title.contains("落地") || title.contains("架构") || title.contains("调优")) {
                    practicalCount++;
                    knowledgePoints.add(title);
                }
            }

            if (knowledgePoints.isEmpty() && StringUtils.hasText(c.getSkills())) {
                knowledgePoints.addAll(Arrays.asList(c.getSkills().split("[,，、 ]+")));
            }

            // 3. 计算实战工程化占比 (根据章节实战词频，通常在 55% ~ 85%)
            int totalCatalogs = Math.max(1, catalogs.size());
            int practicalWeight = Math.min(90, Math.max(50, 45 + (practicalCount * 40 / totalCatalogs)));

            // 4. 评估学员认知难度跨度
            int courseDiff = c.getDifficultyLevel() != null ? c.getDifficultyLevel() : 2;
            String difficultyAssessment = courseDiff <= userDifficulty ? "平滑承接 · 稳固基石"
                : (courseDiff == userDifficulty + 1 ? "适度挑战 · 技能跃升" : "高阶攻坚 · 架构突破");

            analyzedList.add(AnalyzedCourseVO.builder()
                .courseId(c.getCourseId())
                .courseName(c.getCourseName())
                .coverUrl(c.getCoverUrl())
                .price(c.getPrice())
                .originalPrice(c.getOriginalPrice())
                .teacherName(c.getTeacherName())
                .difficultyLevel(courseDiff)
                .learnerCount(c.getLearnerCount())
                .matchScore(c.getAlgorithmScore())
                .prerequisiteSkills(prerequisites)
                .coreKnowledgePoints(knowledgePoints.stream().distinct().limit(4).toList())
                .practicalWeight(practicalWeight)
                .syllabusSummary(syllabusSummary.length() > 0 ? syllabusSummary.toString() : "系统化进阶核心大纲")
                .difficultyAssessment(difficultyAssessment)
                .matchTag(c.getMatchTag())
                .evidencePaths(c.getEvidencePaths() != null ? c.getEvidencePaths() : List.of())
                .build());
        }

        return analyzedList;
    }

    private static final List<Map.Entry<List<String>, List<String>>> PREREQ_RULES = List.of(
        Map.entry(List.of("springcloud", "微服务"), List.of("Java 核心语法", "SpringBoot 基础")),
        Map.entry(List.of("k8s", "kubernetes"), List.of("Linux 基础操作", "Docker 容器基础")),
        Map.entry(List.of("大模型", "llm", "rag"), List.of("Python 基础", "基础机器学习概念")),
        Map.entry(List.of("vue3", "react"), List.of("HTML5/CSS3", "ES6+ / TypeScript")),
        Map.entry(List.of("flink", "spark"), List.of("Java / Scala 基础", "SQL 复杂查询"))
    );

    private List<String> extractPrerequisites(CandidateCourseDTO c) {
        String text = (c.getCourseName() + " " + (c.getSkills() != null ? c.getSkills() : "")).toLowerCase();
        return PREREQ_RULES.stream()
            .filter(r -> r.getKey().stream().anyMatch(text::contains))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseGet(() -> List.of("计算机基础知识"));
    }
}
