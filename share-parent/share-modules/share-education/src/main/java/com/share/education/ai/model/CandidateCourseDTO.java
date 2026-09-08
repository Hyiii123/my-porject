package com.share.education.ai.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 推荐 Agent 初排产出的候选课程对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateCourseDTO {
    private Long courseId;
    private String courseName;
    private String coverUrl;
    private Long categoryId;
    private String categoryName;
    private Long price;
    private Long originalPrice;
    private String teacherName;
    private Integer difficultyLevel;
    private String skills;
    private String targetRole;
    private Integer learnerCount;
    private Double algorithmScore;
    private String recallChannel; // 召回渠道: "ALGORITHM_INFERENCE", "GRAPH_SIMILARITY", "CAREER_GOAL", "HOT_DISCOVERY"
}
