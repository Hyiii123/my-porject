package com.share.education.service.support.grading.strategy;

import com.share.education.service.support.grading.QuestionGradingStrategy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.share.education.service.support.EduUtils.intValue;

/**
 * 多选题批改判分策略 (Multiple Choice Question Grading Strategy)
 *
 * <p>支持选项无序性容错（如用户输入 "B,A" 与标准答案 "A,B" 均统一归一化为 "AB"）。</p>
 */
@Component
public class MultipleChoiceGradingStrategy implements QuestionGradingStrategy {

    public static final String TYPE = "multiple";

    @Override
    public String getQuestionType() {
        return TYPE;
    }

    @Override
    public boolean supports(String questionType) {
        if (!StringUtils.hasText(questionType)) return false;
        String lower = questionType.trim().toLowerCase();
        return TYPE.equals(lower) || "multiple_choice".equals(lower) || "2".equals(lower) || "多选".equals(lower);
    }

    @Override
    public String normalize(String rawAnswer) {
        if (!StringUtils.hasText(rawAnswer)) return "";
        String raw = rawAnswer.trim();
        String[] tokens = raw.split("[,，、;；\\s]+");
        if (tokens.length == 1 && tokens[0].length() > 1 && tokens[0].matches("[A-Za-z0-9]+")) {
            tokens = tokens[0].split("");
        }
        List<String> normalized = new ArrayList<>();
        for (String token : tokens) {
            if (!StringUtils.hasText(token)) continue;
            String item = token.trim();
            if (item.matches("\\d+")) {
                int index = intValue(item, -1);
                if (index >= 1 && index <= 26) {
                    item = String.valueOf((char) ('A' + index - 1));
                }
            } else {
                item = item.toUpperCase();
            }
            normalized.add(item);
        }
        return normalized.stream().distinct().sorted().collect(Collectors.joining());
    }

    @Override
    public boolean isCorrect(String correctAnswer, String userAnswer) {
        if (!StringUtils.hasText(correctAnswer) || !StringUtils.hasText(userAnswer)) return false;
        return normalize(correctAnswer).equalsIgnoreCase(normalize(userAnswer));
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
