package com.share.education.ai.algorithm;

import com.share.education.ai.model.AlgorithmCandidateDTO;
import com.share.education.ai.model.UserProfileContext;

import java.util.List;

/**
 * 推荐算法引擎统一 SPI 接口。
 *
 * <p>【架构解耦说明】
 * 用户自研的改进推荐算法（如序列推荐、图神经网络、协同过滤、深度匹配模型等）
 * 在离线训练或实验完成后，只需实现此接口并注册为 Spring Bean，
 * 即可直接无缝接入多智能体推荐流水线（RecommendationAgent -> CourseAnalysisAgent -> PathPlanningAgent -> RAG -> ExplanationGenerationAgent）。
 * </p>
 */
public interface IRecommendAlgorithmEngine {

    /**
     * 算法候选集预测召回与打分
     *
     * @param userId 学员 ID (未登录访客可为 null)
     * @param profile 学员多维学情与技能画像上下文
     * @param topK 期望召回的候选数量
     * @return 候选课程打分与特征权重列表
     */
    List<AlgorithmCandidateDTO> recallCandidates(Long userId, UserProfileContext profile, int topK);

    /**
     * 算法引擎名称标识 (如 "HybridVectorGraph", "UserCF", "DIN-Transformer", "CustomGraphSage")
     */
    String getEngineName();
}
