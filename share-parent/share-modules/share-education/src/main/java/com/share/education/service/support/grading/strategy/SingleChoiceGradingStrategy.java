package com.share.education.service.support.grading.strategy;

import com.share.education.service.support.grading.QuestionGradingStrategy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static com.share.education.service.support.EduUtils.intValue;

/**
 * 单选题批改判分策略 (Single Choice Question Grading Strategy)
 */
@Component
public class SingleChoiceGradingStrategy implements QuestionGradingStrategy {

    public static final String TYPE = "single";

    @Override
    public String getQuestionType() {
        return TYPE;
    }

    @Override
    public boolean supports(String questionType) {
        if (!StringUtils.hasText(questionType)) return false;
        String lower = questionType.trim().toLowerCase();
        return TYPE.equals(lower) || "single_choice".equals(lower) || "1".equals(lower) || "单选".equals(lower);
    }

    @Override
    public String normalize(String rawAnswer) {
        if (!StringUtils.hasText(rawAnswer)) return "";
        String raw = rawAnswer.trim();
        String[] tokens = raw.split("[,，、;；\\s]+");
        for (String token : tokens) {
            if (!StringUtils.hasText(token)) continue;
            String item = token.trim();
            if (item.matches("\\d+")) {
                int index = intValue(item, -1);
                if (index >= 1 && index <= 26) {
                    return String.valueOf((char) ('A' + index - 1));
                }
            } else {
                return item.substring(0, 1).toUpperCase();
            }
        }
        return "";
    }

    @Override
    public boolean isCorrect(String correctAnswer, String userAnswer) {
        if (!StringUtils.hasText(correctAnswer) || !StringUtils.hasText(userAnswer)) return false;
        return normalize(correctAnswer).equalsIgnoreCase(normalize(userAnswer));
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
