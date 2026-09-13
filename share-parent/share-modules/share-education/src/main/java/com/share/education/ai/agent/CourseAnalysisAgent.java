package com.share.education.ai.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.education.ai.model.AnalyzedCourseVO;
import com.share.education.ai.model.CandidateCourseDTO;
import com.share.education.ai.model.CourseKnowledgeProfile;
import com.share.education.ai.model.UserProfileContext;
import com.share.education.domain.EduCourseCatalog;
import com.share.education.mapper.EduCourseCatalogMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能体 4：课程大纲认知解构 Agent (CourseAnalysisAgent)。
 *
 * <p>【核心职责】：
 * 1. 深度拆解候选课程大纲知识树与章节拓扑；
 * 2. 引入国际布鲁姆认知分级模型 (Bloom's Taxonomy Levels)，对课程认知梯级进行客观标注；
 * 3. 提取先修前置依赖 (Prerequisites) 与核心实战攻坚知识点；
 * 4. 识别综合实战大项目 (Capstone Projects)，产出标准 CourseKnowledgeProfile。</p>
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

                if (isPracticalKeyword(title)) {
                    practicalCount++;
                    knowledgePoints.add(title);
                }
            }

            if (knowledgePoints.isEmpty() && StringUtils.hasText(c.getSkills())) {
                knowledgePoints.addAll(Arrays.asList(c.getSkills().split("[,，、 ]+")));
            }

            // 3. 计算实战工程化占比
            int totalCatalogs = Math.max(1, catalogs.size());
            int practicalWeight = Math.min(92, Math.max(50, 48 + (practicalCount * 42 / totalCatalogs)));

            // 4. 布鲁姆认知等级解构
            BloomInfo bloom = evaluateBloomTaxonomy(c, catalogs);

            // 5. 综合 Capstone 大项目识别
            boolean isCapstone = detectCapstoneProject(c, catalogs);

            // 6. 评估学员认知难度跨度
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
                .bloomLevel(bloom.level)
                .bloomName(bloom.name)
                .isCapstone(isCapstone)
                .build());
        }

        return analyzedList;
    }

    /**
     * 生成单门课程的标准知识认知 Profile
     */
    public CourseKnowledgeProfile extractKnowledgeProfile(CandidateCourseDTO course, List<EduCourseCatalog> catalogs) {
        BloomInfo bloom = evaluateBloomTaxonomy(course, catalogs);
        boolean isCapstone = detectCapstoneProject(course, catalogs);
        List<String> prereqs = extractPrerequisites(course);

        int practicalCount = 0;
        List<String> concepts = new ArrayList<>();
        if (catalogs != null) {
            for (EduCourseCatalog cat : catalogs) {
                if (cat.getCatalogTitle() != null && isPracticalKeyword(cat.getCatalogTitle())) {
                    practicalCount++;
                    concepts.add(cat.getCatalogTitle());
                }
            }
        }
        int total = catalogs != null && !catalogs.isEmpty() ? catalogs.size() : 1;
        int practicalWeight = Math.min(92, Math.max(50, 48 + (practicalCount * 42 / total)));

        return CourseKnowledgeProfile.builder()
            .courseId(course.getCourseId())
            .courseName(course.getCourseName())
            .bloomTaxonomyLevel(bloom.level)
            .bloomTaxonomyName(bloom.name)
            .practicalWeight(practicalWeight)
            .isCapstoneProject(isCapstone)
            .coreConcepts(concepts.stream().distinct().limit(5).toList())
            .prerequisiteConcepts(prereqs)
            .cognitiveProgression(course.getDifficultyLevel() != null && course.getDifficultyLevel() >= 3 ? "架构攻坚" : "平滑跃升")
            .build();
    }

    private boolean isPracticalKeyword(String title) {
        String lower = title.toLowerCase();
        return lower.contains("实战") || lower.contains("项目") || lower.contains("落地")
            || lower.contains("架构") || lower.contains("调优") || lower.contains("源码")
            || lower.contains("工程") || lower.contains("微服务") || lower.contains("分布式")
            || lower.contains("pipeline") || lower.contains("设计");
    }

    private static final List<Map.Entry<List<String>, List<String>>> PREREQ_RULES = List.of(
        Map.entry(List.of("springcloud", "微服务", "dubbo"), List.of("Java 核心语法", "SpringBoot 基础")),
        Map.entry(List.of("k8s", "kubernetes", "istio"), List.of("Linux 基础操作", "Docker 容器基础")),
        Map.entry(List.of("大模型", "llm", "rag", "agent"), List.of("Python 基础", "基础机器学习概念")),
        Map.entry(List.of("vue3", "react", "next.js"), List.of("HTML5/CSS3", "ES6+ / TypeScript")),
        Map.entry(List.of("flink", "spark", "hadoop"), List.of("Java / Scala 基础", "SQL 复杂查询")),
        Map.entry(List.of("go", "golang", "gin"), List.of("计算机网络基础", "操作系统导论")),
        Map.entry(List.of("redis", "mysql调优", "分库分表"), List.of("SQL 基础", "关系型数据库原理"))
    );

    private List<String> extractPrerequisites(CandidateCourseDTO c) {
        String text = (c.getCourseName() + " " + (c.getSkills() != null ? c.getSkills() : "")).toLowerCase();
        return PREREQ_RULES.stream()
            .filter(r -> r.getKey().stream().anyMatch(text::contains))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseGet(() -> List.of("计算机基础知识"));
    }

    private record BloomInfo(String level, String name) {}

    private BloomInfo evaluateBloomTaxonomy(CandidateCourseDTO c, List<EduCourseCatalog> catalogs) {
        String name = c.getCourseName() != null ? c.getCourseName().toLowerCase() : "";
        int diff = c.getDifficultyLevel() != null ? c.getDifficultyLevel() : 2;

        if (name.contains("底层") || name.contains("源码") || name.contains("内核")) {
            return new BloomInfo("ANALYZE", "底层剖析级");
        } else if (name.contains("调优") || name.contains("架构") || name.contains("高并发") || diff >= 3) {
            return new BloomInfo("EVALUATE", "架构调优级");
        } else if (name.contains("自研") || name.contains("手写") || name.contains("平台")) {
            return new BloomInfo("CREATE", "系统创新级");
        } else if (name.contains("实战") || name.contains("开发") || name.contains("应用") || diff == 2) {
            return new BloomInfo("APPLY", "工程应用级");
        } else if (name.contains("原理") || name.contains("机制") || name.contains("网络")) {
            return new BloomInfo("UNDERSTAND", "原理理解级");
        } else {
            return new BloomInfo("REMEMBER", "核心识记级");
        }
    }

    private boolean detectCapstoneProject(CandidateCourseDTO c, List<EduCourseCatalog> catalogs) {
        String text = (c.getCourseName() + " " + (c.getSkills() != null ? c.getSkills() : "")).toLowerCase();
        if (text.contains("综合项目") || text.contains("全栈实战") || text.contains("企业级项目")
            || text.contains("电商系统") || text.contains("平台开发") || text.contains("微服务商城")
            || text.contains("工业级")) {
            return true;
        }
        if (catalogs != null) {
            for (EduCourseCatalog cat : catalogs) {
                if (cat.getCatalogTitle() != null) {
                    String title = cat.getCatalogTitle().toLowerCase();
                    if (title.contains("综合实战") || title.contains("毕业设计") || title.contains("项目交付") || title.contains("上线发布")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
