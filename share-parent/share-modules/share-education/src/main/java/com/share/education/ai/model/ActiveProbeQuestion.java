package com.share.education.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 冷启动主动探针交互问卷模型 (ActiveProbingAgent 产出)。
 * 用于在学员无数据或画像缺失时主动发起 1-Turn 结构化探针，极速建立精准学情基线。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveProbeQuestion {

    /** 问题唯一标识 (如 "probe_target_role", "probe_current_level", "probe_time_commitment") */
    private String questionId;

    /** 问题标题 (如 "你当前最渴望突破的目标技术岗位是？") */
    private String title;

    /** 补充说明/引导文案 */
    private String description;

    /** 诊断维度类别 ("CAREER_TARGET", "CURRENT_LEVEL", "TIME_COMMITMENT") */
    private String category;

    /** 结构化可选项 */
    @Builder.Default
    private List<ProbeOption> options = Collections.emptyList();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProbeOption {
        /** 选项标识 (如 "java_architect", "llm_agent", "beginner", "junior") */
        private String key;

        /** 选项展示文案 */
        private String label;

        /** 选项详细副标题/说明 */
        private String desc;

        /** 选中后映射的特征标签与技能权重增量 */
        @Builder.Default
        private Map<String, Integer> inferredSkillWeights = Collections.emptyMap();
    }
}
