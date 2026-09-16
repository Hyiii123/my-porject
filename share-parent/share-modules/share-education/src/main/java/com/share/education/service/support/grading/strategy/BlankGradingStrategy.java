package com.share.education.service.support.grading.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.education.service.support.grading.QuestionGradingStrategy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 填空与主观题批改策略 (Blank/Essay Question Grading Strategy)
 */
@Component
public class BlankGradingStrategy implements QuestionGradingStrategy {

    public static final String TYPE = "blank";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String getQuestionType() {
        return TYPE;
    }

    @Override
    public boolean supports(String questionType) {
        // 作为兜底策略，支持 fill, blank, essay, 4, 5，或者未识别题型
        if (!StringUtils.hasText(questionType)) return true;
        String lower = questionType.trim().toLowerCase();
        return TYPE.equals(lower) || "fill".equals(lower) || "essay".equals(lower)
                || "4".equals(lower) || "5".equals(lower) || "填空".equals(lower) || "简答".equals(lower);
    }

    @Override
    public String normalize(String rawAnswer) {
        if (!StringUtils.hasText(rawAnswer)) return "";
        String raw = rawAnswer.trim();
        if (raw.startsWith("[") && raw.endsWith("]")) {
            try {
                List<?> values = OBJECT_MAPPER.readValue(raw, List.class);
                raw = values.stream().map(String::valueOf).collect(Collectors.joining(","));
            } catch (Exception ignored) {}
        }
        return raw;
    }

    @Override
    public boolean isCorrect(String correctAnswer, String userAnswer) {
        if (!StringUtils.hasText(correctAnswer) || !StringUtils.hasText(userAnswer)) return false;
        String normCorrect = normalize(correctAnswer).replaceAll("\\s+", " ").trim();
        String normUser = normalize(userAnswer).replaceAll("\\s+", " ").trim();
        return normCorrect.equalsIgnoreCase(normUser);
    }

    @Override
    public int getOrder() {
        return 100; // 兜底策略，排在最后
    }
}
