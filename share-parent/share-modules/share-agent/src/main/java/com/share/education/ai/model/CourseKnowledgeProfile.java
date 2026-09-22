package com.share.education.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 课程大纲知识认知解构画像模型 (CourseAnalysisAgent 产出)。
 * 结合国际教育学布鲁姆认知分级 (Bloom's Taxonomy) 深度解构课程内涵。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseKnowledgeProfile {

    private Long courseId;
    private String courseName;

    /**
     * 布鲁姆认知层级：
     * REMEMBER (概念识记), UNDERSTAND (原理理解), APPLY (工程应用),
     * ANALYZE (底层剖析), EVALUATE (架构调优), CREATE (系统自研)
     */
    private String bloomTaxonomyLevel;

    /** 布鲁姆认知中文名称 (如 "工程应用级", "架构调优级") */
    private String bloomTaxonomyName;

    /** 实战工程代码密度占比 (0 ~ 100%) */
    private Integer practicalWeight;

    /** 是否包含综合实战大项目/工业级 Capstone Project */
    private Boolean isCapstoneProject;

    /** 核心攻坚技术概念清单 */
    @Builder.Default
    private List<String> coreConcepts = Collections.emptyList();

    /** 强依赖先修技术概念 */
    @Builder.Default
    private List<String> prerequisiteConcepts = Collections.emptyList();

    /** 课程对应的认知台阶递进评估 */
    private String cognitiveProgression;
}
