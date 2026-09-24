package com.share.education.ai.algorithm.bkt;

import org.springframework.stereotype.Component;

/**
 * 工业级贝叶斯知识追踪算法引擎 (Bayesian Knowledge Tracing Engine, BKT)
 *
 * <p>基于认知科学与教育数据挖掘标准隐马尔可夫模型 (Corbett & Anderson HMM)，
 * 动态根据学员的做题正误序列实时后验推导微知识点掌握概率 $P(L_t)$。</p>
 */
@Component
public class BayesianKnowledgeTracingEngine {

    // 默认 BKT 认知参数 (针对计算机工科高难度算法与微服务场景标定)
    private static final double DEFAULT_P_L0 = 0.25; // 初始掌握先验概率 P(L0)
    private static final double DEFAULT_P_T  = 0.18; // 学懂转移概率 P(T) (经过单次练习从未掌握跃迁至掌握)
    private static final double DEFAULT_P_G  = 0.15; // 猜对概率 P(G) (未掌握但蒙对，编程题猜对概率低)
    private static final double DEFAULT_P_S  = 0.08; // 失误概率 P(S) (已掌握因粗心答错)

    public static final double MASTERY_THRESHOLD = 0.85;   // 达标精通阈值
    public static final double STRUGGLING_THRESHOLD = 0.50;// 薄弱卡点阈值

    /**
     * 根据单次作答结果更新掌握度后验概率
     *
     * @param currentPrior 当前掌握度先验概率 P(L_{t-1})
     * @param correct      本次作答是否正确 (true: 正确, false: 错误)
     * @return 更新后的知识点掌握概率 P(L_t)
     */
    public double updateMastery(double currentPrior, boolean correct) {
        double prior = Math.max(0.01, Math.min(0.99, currentPrior <= 0.0 ? DEFAULT_P_L0 : currentPrior));
        double pG = DEFAULT_P_G;
        double pS = DEFAULT_P_S;
        double pT = DEFAULT_P_T;

        double posteriorGivenObs;
        if (correct) {
            // P(L_{t-1} | obs=1) = (P(L_{t-1}) * (1 - P(S))) / (P(L_{t-1}) * (1 - P(S)) + (1 - P(L_{t-1})) * P(G))
            double numerator = prior * (1.0 - pS);
            double denominator = numerator + ((1.0 - prior) * pG);
            posteriorGivenObs = denominator > 0.0 ? numerator / denominator : prior;
        } else {
            // P(L_{t-1} | obs=0) = (P(L_{t-1}) * P(S)) / (P(L_{t-1}) * P(S) + (1 - P(L_{t-1})) * (1 - P(G)))
            double numerator = prior * pS;
            double denominator = numerator + ((1.0 - prior) * (1.0 - pG));
            posteriorGivenObs = denominator > 0.0 ? numerator / denominator : prior;
        }

        // P(L_t) = P(L_{t-1} | obs) + (1 - P(L_{t-1} | obs)) * P(T)
        double updatedMastery = posteriorGivenObs + ((1.0 - posteriorGivenObs) * pT);
        // 关键防御 (BKT Degeneracy Defense)：作答错误时，掌握度严禁发生反向暴涨超过初始先验
        if (!correct && updatedMastery >= prior) {
            updatedMastery = Math.min(prior * 0.85, posteriorGivenObs);
        }
        return Math.round(Math.max(0.0, Math.min(1.0, updatedMastery)) * 1000.0) / 1000.0;
    }

    /**
     * 判断当前知识点认知掌握状态
     */
    public String resolveStatus(double mastery) {
        if (mastery >= MASTERY_THRESHOLD) {
            return "MASTERED"; // 已精通
        } else if (mastery >= STRUGGLING_THRESHOLD) {
            return "PRACTICING"; // 练习强化中
        } else {
            return "STRUGGLING"; // 严重薄弱/断层
        }
    }
}
