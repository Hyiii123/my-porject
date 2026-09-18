package com.share.education.ai.composite.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 多智能体圆桌辩论单轮发言记录 (AgentDebateTurn)。
 * 记录发言人、受指责/建议对象、核心立场与方案补丁。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentDebateTurn {

    /** 辩论轮次 (Round 1, Round 2) */
    private Integer round;

    /** 发言智能体标识 (PedagogyMentor, IndustryArchitect, PathCritic, ConsensusArbiter) */
    private String speaker;

    /** 智能体可读名称 (如 "学情成长导师", "大厂技术总监", "审判质检法官", "圆桌首席仲裁者") */
    private String speakerName;

    /** 辩论立场: PROPOSE(提议), CHALLENGE(质疑), COMPROMISE(妥协折中), AUDIT_PASS(质检通过), AUDIT_REJECT(质检打回), CONSENSUS(共识确认) */
    private String stance;

    /** 针对的目标智能体或方案模块 */
    private String targetObject;

    /** 核心发言论点阐述 */
    private String argument;

    /** 提出的方案修改补丁 (可选) */
    private PlanPatch proposedPatch;

    /** 当前维度的量化评估或指标摘要 */
    private String metricSummary;

    /** 发言耗时 (ms) */
    private Long latencyMs;

    /** 发言时间戳 */
    private Long timestamp;
}
