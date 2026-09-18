package com.share.education.ai.composite.model;

import com.share.education.ai.model.AnalyzedCourseVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 智能体博弈过程中提出的学习路径修改补丁 (PlanPatch)。
 * 用于在多智能体圆桌会话中，各专家 Agent 动态对课程草案进行增删换调。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanPatch {

    /** 目标阶段序号 (1 ~ 4) */
    private Integer targetStageIndex;

    /** 建议移除的高难度或先修冲突课程 ID 列表 */
    @Builder.Default
    private List<Long> removeCourseIds = Collections.emptyList();

    /** 建议插入的过渡或实战课程列表 */
    @Builder.Default
    private List<AnalyzedCourseVO> insertCourses = Collections.emptyList();

    /** 建议调整的阶段难度等级 (1:初级, 2:中级, 3:高级) */
    private Integer adjustedDifficulty;

    /** 调整建议的学时变化量 (+/- 小时) */
    private Integer hoursAdjustment;

    /** 补丁理由与学术/工程依据 */
    private String rationale;
}
