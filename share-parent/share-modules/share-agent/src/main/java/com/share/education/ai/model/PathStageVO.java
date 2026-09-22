package com.share.education.ai.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 学习路径的单个阶段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PathStageVO {
    /** 阶段序号 (1, 2, 3, 4) */
    private Integer stageIndex;

    /** 阶段名称 (如 "阶段一：工程化基石与核心语法", "阶段二：高并发与分布式进阶") */
    private String stageName;

    /** 阶段成长目标 */
    private String stageGoal;

    /** 预估攻坚学时 (小时) */
    private Integer estimatedHours;

    /** 该阶段包含的推荐课程列表 */
    @Builder.Default
    private List<AnalyzedCourseVO> courses = Collections.emptyList();
}
