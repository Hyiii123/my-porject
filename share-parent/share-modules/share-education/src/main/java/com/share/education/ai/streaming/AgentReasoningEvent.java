package com.share.education.ai.streaming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 智能体流式思考事件载荷 (SSE Agent Reasoning Event Payload)
 * 用于下一代 L5 架构向前端逐阶段发射智能体思考心流、中间决策数据与审判质检结论。
 *
 * @author Zhiwen Study Companion
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentReasoningEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件类型枚举:
     * PROBE_CHECK: 冷启动完备度探针检测
     * PROFILE_BUILT: 学情画像特征与短板建模完成
     * COURSE_ANALYSIS: 布鲁姆认知等级与大纲结构化解构
     * PATH_PLANNED: DAG 拓扑阶段进阶规划完成
     * CRITIC_AUDIT: PathCritic 量化审判质检与图论验证
     * REFLECTION_DIRECTIVE: 触发受控反思回路修正指令
     * FINAL_RESULT: 推荐与全景路线最终交付
     * STREAM_DONE: 全链路思考流结束
     */
    private String eventType;

    /**
     * 执行当前步骤的智能体名称 (如 UserProfileAgent, PathCriticAgent)
     */
    private String agentName;

    /**
     * 步骤编号 (1 ~ 6)
     */
    private Integer stepIndex;

    /**
     * 智能体思考过程片段 / 审计结论解说词
     */
    private String thoughtChunk;

    /**
     * 结构化负载数据 (如 CriticReport, ProfileVO, CourseList 等)
     */
    private Object dataPayload;

    /**
     * 当前阶段耗时 (毫秒)
     */
    private Long latencyMs;

    /**
     * 事件生成时间戳
     */
    @Builder.Default
    private Long timestamp = System.currentTimeMillis();

    /**
     * 快速构建事件工厂方法
     */
    public static AgentReasoningEvent of(String eventType, String agentName, Integer stepIndex, String thoughtChunk, Object dataPayload, Long latencyMs) {
        return AgentReasoningEvent.builder()
                .eventType(eventType)
                .agentName(agentName)
                .stepIndex(stepIndex)
                .thoughtChunk(thoughtChunk)
                .dataPayload(dataPayload)
                .latencyMs(latencyMs)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
