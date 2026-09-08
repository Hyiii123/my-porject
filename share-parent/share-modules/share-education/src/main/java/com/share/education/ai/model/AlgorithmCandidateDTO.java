package com.share.education.ai.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * 推荐算法引擎输出的候选预测项 (用于对接用户自研实验算法)。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgorithmCandidateDTO {
    /** 课程 ID */
    private Long courseId;

    /** 算法预测打分 (0.0 ~ 100.0) */
    private Double score;

    /** 算法给出的候选匹配标签 (如 "技术栈强匹配", "序列推荐预测", "冷启动探索引擎") */
    private String matchTag;

    /** 算法提取的特征映射或注意力权重 (用于后续 Agent 解释与路径分析) */
    private Map<String, Object> featureMap;
}
