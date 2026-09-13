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
            String difficultyAssessment;
            if (courseDiff <= userDifficulty) {
                difficultyAssessment = "平滑承接 · 稳固基石";
            } else if (courseDiff == userDifficulty + 1) {
                difficultyAssessment = "适度挑战 · 技能跃升";
            } else {
                difficultyAssessment = "高阶攻坚 · 架构突破";
            }

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
                .coreKnowledgePoints(knowledgePoints.stream().distinct().limit(4).collect(Collectors.toList()))
                .practicalWeight(practicalWeight)
                .syllabusSummary(syllabusSummary.length() > 0 ? syllabusSummary.toString() : "系统化进阶核心大纲")
                .difficultyAssessment(difficultyAssessment)
                .matchTag(c.getMatchTag())
                .evidencePaths(c.getEvidencePaths() != null ? c.getEvidencePaths() : Collections.emptyList())
                .build());
        }

        return analyzedList;
    }

    private List<String> extractPrerequisites(CandidateCourseDTO c) {
        String name = (c.getCourseName() + " " + c.getSkills()).toLowerCase();
        List<String> prereqs = new ArrayList<>();

        if (name.contains("springcloud") || name.contains("微服务")) {
            prereqs.add("Java 核心语法");
            prereqs.add("SpringBoot 基础");
        } else if (name.contains("k8s") || name.contains("kubernetes")) {
            prereqs.add("Linux 基础操作");
            prereqs.add("Docker 容器基础");
        } else if (name.contains("大模型") || name.contains("llm") || name.contains("rag")) {
            prereqs.add("Python 基础");
            prereqs.add("基础机器学习概念");
        } else if (name.contains("vue3") || name.contains("react")) {
            prereqs.add("HTML5/CSS3");
            prereqs.add("ES6+ / TypeScript");
        } else if (name.contains("flink") || name.contains("spark")) {
            prereqs.add("Java / Scala 基础");
            prereqs.add("SQL 复杂查询");
        } else {
            prereqs.add("计算机基础知识");
        }

        return prereqs;
    }
}
