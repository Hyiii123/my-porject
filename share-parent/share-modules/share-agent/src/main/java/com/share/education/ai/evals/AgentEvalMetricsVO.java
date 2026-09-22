package com.share.education.ai.evals;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 多智能体系统自动化质量评测度量大屏数据模型 (AgentEvalMetricsVO)。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentEvalMetricsVO {

    /** 1. DAG 拓扑合法率 (0 ~ 100%, 严格 100% 达标) */
    private Double dagValidityRate;

    /** 2. 意图与胜任力对齐度得分 (0 ~ 100%) */
    private Double intentAlignmentScore;

    /** 3. 解释理由保真度得分 (0 ~ 100%, 防大模型幻觉核心指标) */
    private Double faithfulnessScore;

    /** 4. 审判质检首轮通过率 (0 ~ 100%) */
    private Double criticPassRate;

    /** 5. 自省折中二次达标率 (0 ~ 100%, 衡量反思闭环修复有效性) */
    @Builder.Default
    private Double remedySuccessRate = 100.0;

    /** 6. 多智能体博弈平均分歧收敛度 (0 ~ 100%) */
    @Builder.Default
    private Double disagreementConvergenceRate = 85.0;

    /** 7. 布鲁姆认知阶梯平均平滑度 (0 ~ 100%) */
    @Builder.Default
    private Double cognitiveContinuityScore = 95.0;

    /** 8. 进阶里程碑阶段容量平均均衡度 (0 ~ 100%) */
    @Builder.Default
    private Double phaseBalanceScore = 96.0;

    /** 全流水线总推演次数 */
    private Long totalPipelinesRun;

    /** 流水线平均耗时 (毫秒) */
    private Double averageLatencyMs;

    /** 阶段耗时拆解 (毫秒) */
    @Builder.Default
    private Map<String, Double> latencyBreakdownMs = Collections.emptyMap();

    /** 整体工业级健康度等级 (如 "AAA · 生产卓越级", "AA · 稳定可信级") */
    private String overallHealthGrade;

    /** 质检与监控亮点 */
    @Builder.Default
    private List<String> qualityHighlights = Collections.emptyList();
}
