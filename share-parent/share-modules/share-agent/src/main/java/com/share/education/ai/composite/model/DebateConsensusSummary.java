package com.share.education.ai.composite.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 多智能体博弈收敛形成的最终共识摘要。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebateConsensusSummary {

    /** 历经辩论总轮次 (1 ~ 2) */
    private Integer totalRounds;

    /** 是否圆满达成三方共识 */
    private Boolean consensusReached;

    /** 初始冲突焦点总结 */
    private String initialConflictSummary;

    /** 双方协商妥协的最终决议 */
    private String compromiseResolution;

    /** 审判法官终审质检得分 (0 ~ 100) */
    private Integer finalCriticScore;

    /** 方案终审评级 (如 "AAA · 卓越级", "A+ · 优良级") */
    private String qualityGrade;

    /** 核心共识达成要点清单 */
    @Builder.Default
    private List<String> keyAgreements = Collections.emptyList();
}
