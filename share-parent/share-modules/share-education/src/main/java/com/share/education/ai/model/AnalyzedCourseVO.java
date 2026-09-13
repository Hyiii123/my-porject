package com.share.education.ai.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 课程深度分析对象 (CourseAnalysisAgent 产出)。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzedCourseVO {
    private Long courseId;
    private String courseName;
    private String coverUrl;
    private Long price;
    private Long originalPrice;
    private String teacherName;
    private Integer difficultyLevel;
    private Integer learnerCount;
    private Double matchScore;

    /** 先修依赖技能 (学这门课前需要掌握的基石) */
    @Builder.Default
    private List<String> prerequisiteSkills = Collections.emptyList();

    /** 核心攻坚知识点列表 (从大纲与标签提取) */
    @Builder.Default
    private List<String> coreKnowledgePoints = Collections.emptyList();

    /** 实战工程化权重占比 (0~100%) */
    private Integer practicalWeight;

    /** 章节大纲核心速览 */
    private String syllabusSummary;

    /** 难度适配评估 ("平滑承接", "适度跨越", "高阶突破") */
    private String difficultyAssessment;

    /** 智能契合标签 */
    private String matchTag;

    /** 知识图谱先修推导路径 (由 DRAG-KP4SR 算法检索产出) */
    @Builder.Default
    private List<String> evidencePaths = Collections.emptyList();
}
