package com.share.education.ai.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 学员综合画像上下文 (UserProfileAgent 产出)。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileContext {
    /** 学员用户 ID */
    private Long userId;

    /** 目标岗位 (如 "Java 架构师", "前端全栈开发", "AI 算法工程师") */
    private String intendedRole;

    /** 偏好难度 (1: 初级, 2: 中级, 3: 高级) */
    private Integer preferredDifficulty;

    /** 50 维技能掌握度权重映射 (技能名 -> 0~100) */
    @Builder.Default
    private Map<String, Integer> skillWeights = Collections.emptyMap();

    /** 优势技能 (Top 3~5 掌握度最高的技能) */
    @Builder.Default
    private List<String> topSkills = Collections.emptyList();

    /** 待提升技能短板 (Skill Gaps，针对目标岗位尚缺乏的技术点) */
    @Builder.Default
    private List<String> skillGaps = Collections.emptyList();

    /** 自律完课指数 (0~100) */
    private Integer disciplineScore;

    /** 画像标签列表 (如 ["系统学习者", "高完课率", "Spring 生态精通"]) */
    @Builder.Default
    private List<String> userTags = Collections.emptyList();

    /** 已报名的课程 ID 集合 (用于去重) */
    @Builder.Default
    private Set<Long> enrolledCourseIds = Collections.emptySet();

    /** 累计学习时长 (小时) */
    private Double completedHours;

    /** 学员认知能力等级评估 (小白入门 / 初级进阶 / 骨干冲刺 / 架构瓶颈) */
    private String cognitiveLevel;
}
