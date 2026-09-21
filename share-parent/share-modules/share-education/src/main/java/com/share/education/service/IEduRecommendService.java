package com.share.education.service;

import com.share.education.ai.evals.AgentEvalMetricsVO;
import com.share.education.ai.model.ActiveProbeQuestion;
import com.share.education.ai.model.LearningPathPlan;
import com.share.education.domain.EduUserPortrait;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 智能推荐、多智能体导学与学员画像领域接口
 */
public interface IEduRecommendService {

    List<Map<String, Object>> recommendations(String type);

    List<Map<String, Object>> personalizedRecommendations(int limit);

    LearningPathPlan getPersonalizedLearningPath();

    Map<String, Object> refinePersonalizedLearningPath(Map<String, Object> overrides);

    List<ActiveProbeQuestion> getActiveProbingQuestions();

    Map<String, Object> submitActiveProbingAnswers(Map<String, String> answers);

    AgentEvalMetricsVO getAgentEvaluationMetrics();

    SseEmitter streamPersonalizedReasoning(String targetRole);
    default SseEmitter streamPersonalizedReasoning(String targetRole, String query) {
        return streamPersonalizedReasoning(targetRole);
    }

    Map<String, Object> orchestrateAgentRecommend(Long userId, String targetRole, Integer limit, Integer difficulty);
    default Map<String, Object> orchestrateAgentRecommend(Long userId, String targetRole, Integer limit, Integer difficulty, String query) {
        return orchestrateAgentRecommend(userId, targetRole, limit, difficulty);
    }
    default Map<String, Object> orchestrateAgentRecommend(Long userId, String targetRole, Integer limit) {
        return orchestrateAgentRecommend(userId, targetRole, limit, null);
    }

    Map<String, Object> getUserPortrait(Long userId);

    Map<String, Object> updateUserPortraitPreferences(Long userId, Map<String, Object> body);

    Map<String, Object> portraitView(EduUserPortrait item);
}

