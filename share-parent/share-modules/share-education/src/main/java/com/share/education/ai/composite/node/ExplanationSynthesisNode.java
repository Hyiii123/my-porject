package com.share.education.ai.composite.node;

import com.share.education.ai.agent.ExplanationGenerationAgent;
import com.share.education.ai.composite.state.DebateBlackboardState;
import com.share.education.ai.model.LearningPathPlan;
import com.share.education.ai.model.PersonalizedRecommendVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 复合架构交付层：方案合成与可解释性推理节点 (ExplanationSynthesisNode)。
 * 依托 Spring AI 与 RAG 胜任力知识切片，为三方共识达成的终审方案生成通俗、富有逻辑的可解释性推荐理由。
 */
@Component
public class ExplanationSynthesisNode {

    private static final Logger log = LoggerFactory.getLogger(ExplanationSynthesisNode.class);

    private final ExplanationGenerationAgent explanationAgent;

    public ExplanationSynthesisNode(ExplanationGenerationAgent explanationAgent) {
        this.explanationAgent = explanationAgent;
    }

    /**
     * 合成最终交付成果
     */
    public List<PersonalizedRecommendVO> synthesizeDelivery(DebateBlackboardState state, int limit) {
        long tStart = System.currentTimeMillis();
        LearningPathPlan plan = state.getCurrentDraftPlan();
        if (plan == null) {
            return Collections.emptyList();
        }

        // 挂载终审法官报告
        plan.setCriticReport(state.getCriticReport());

        // 生成带可解释性理由的个性化推荐课列表
        List<PersonalizedRecommendVO> recs = explanationAgent.generateExplanations(
            state.getUserProfile(), plan, limit
        );
        state.setFinalRecommendations(recs);

        long latency = System.currentTimeMillis() - tStart;
        state.recordLatency("ExplanationSynthesisNode", latency);
        log.info("[ExplanationSynthesisNode] 方案合成完成: 推荐课程数={}, 耗时={}ms", recs.size(), latency);
        return recs;
    }
}
