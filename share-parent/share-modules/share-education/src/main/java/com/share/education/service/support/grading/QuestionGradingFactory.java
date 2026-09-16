package com.share.education.service.support.grading;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 试题判卷策略工厂 (Question Grading Strategy Factory)
 *
 * <p>基于策略工厂模式统一管理各题型归一化与比对策略，解耦复杂题型判分逻辑。</p>
 */
@Slf4j
@Component
public class QuestionGradingFactory {

    private final List<QuestionGradingStrategy> strategies;
    private final QuestionGradingStrategy defaultStrategy;

    public QuestionGradingFactory(List<QuestionGradingStrategy> strategies) {
        if (strategies == null || strategies.isEmpty()) {
            this.strategies = Collections.emptyList();
            this.defaultStrategy = null;
            log.warn("【QuestionGradingFactory】未注册任何试题判卷策略！");
        } else {
            this.strategies = strategies.stream()
                    .sorted(Comparator.comparingInt(QuestionGradingStrategy::getOrder))
                    .collect(Collectors.toList());
            this.defaultStrategy = this.strategies.get(this.strategies.size() - 1);
            log.info("【QuestionGradingFactory】成功装配试题判卷策略，共 {} 个: {}",
                    this.strategies.size(),
                    this.strategies.stream().map(s -> s.getQuestionType() + "(" + s.getClass().getSimpleName() + ")").toList());
        }
    }

    /**
     * 根据题型代码获取匹配的判分策略
     */
    public QuestionGradingStrategy getStrategy(String questionType) {
        for (QuestionGradingStrategy strategy : strategies) {
            if (strategy.supports(questionType)) {
                return strategy;
            }
        }
        return defaultStrategy;
    }

    /**
     * 规范化用户答案
     */
    public String normalizeAnswer(String rawAnswer, String questionType) {
        QuestionGradingStrategy strategy = getStrategy(questionType);
        return strategy != null ? strategy.normalize(rawAnswer) : (rawAnswer != null ? rawAnswer.trim() : "");
    }

    /**
     * 比对用户答案与参考答案
     */
    public boolean isCorrect(String correctAnswer, String userAnswer, String questionType) {
        QuestionGradingStrategy strategy = getStrategy(questionType);
        return strategy != null && strategy.isCorrect(correctAnswer, userAnswer);
    }
}
