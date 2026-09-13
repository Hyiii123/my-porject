package com.share.education.ai.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 最终展示给学员的个性化推荐数据模型 (包含可解释性 AI 推荐理由与阶段标签)。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalizedRecommendVO {
    private Long id;
    private String title;
    private String cover;
    private String coverUrl;
    private Long price;
    private Long originalPrice;
    private String teacherName;
    private Integer difficulty;
    private Integer learners;

    /** 匹配契合度 (如 98 代表 98%) */
    private Integer matchScore;

    /** 智能契合标签 (如 "目标岗位强契合", "补齐关键短板", "前置必修基石") */
    private String matchTag;

    /** 核心可解释性推荐理由 (ExplanationGenerationAgent + RAG 产出) */
    private String recommendReason;

    /** 处于学习路径的阶段 (如 "阶段二 · 核心进阶") */
    private String learningStage;

    /** 学完本课程补齐的关键技术短板 */
    private String skillGapFilled;

    /** 先修依赖技能 */
    @Builder.Default
    private List<String> prerequisiteSkills = Collections.emptyList();

    /** 知识图谱先修推导链路 (供前端展示前沿进阶脉络) */
    @Builder.Default
    private List<String> evidencePaths = Collections.emptyList();
}
