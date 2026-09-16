package com.share.education.service.support.grading.strategy;

import com.share.education.service.support.grading.QuestionGradingStrategy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 判断题批改判分策略 (Judge Question Grading Strategy)
 *
 * <p>支持多维语义归一化（如 true/正确/1/A 统一归一化为 A，false/错误/0/2/B 统一归一化为 B）。</p>
 */
@Component
public class JudgeGradingStrategy implements QuestionGradingStrategy {

    public static final String TYPE = "judge";

    @Override
    public String getQuestionType() {
        return TYPE;
    }

    @Override
    public boolean supports(String questionType) {
        if (!StringUtils.hasText(questionType)) return false;
        String lower = questionType.trim().toLowerCase();
        return TYPE.equals(lower) || "判断".equals(lower) || "3".equals(lower);
    }

    @Override
    public String normalize(String rawAnswer) {
        if (!StringUtils.hasText(rawAnswer)) return "";
        String raw = rawAnswer.trim();
        if ("true".equalsIgnoreCase(raw) || "正确".equals(raw) || "1".equals(raw) || "A".equalsIgnoreCase(raw)) {
            return "A";
        }
        if ("false".equalsIgnoreCase(raw) || "错误".equals(raw) || "0".equals(raw) || "2".equals(raw) || "B".equalsIgnoreCase(raw)) {
            return "B";
        }
        return raw.replaceAll("[\\s,，、]", "").toUpperCase();
    }

    @Override
    public boolean isCorrect(String correctAnswer, String userAnswer) {
        if (!StringUtils.hasText(correctAnswer) || !StringUtils.hasText(userAnswer)) return false;
        return normalize(correctAnswer).equalsIgnoreCase(normalize(userAnswer));
    }

    @Override
    public int getOrder() {
        return 30;
    }
}
