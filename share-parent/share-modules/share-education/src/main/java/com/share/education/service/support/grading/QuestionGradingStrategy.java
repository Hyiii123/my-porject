package com.share.education.service.support.grading;

/**
 * 试题批改与归一化策略契约 (Question Grading Strategy Pattern)
 */
public interface QuestionGradingStrategy {

    /**
     * 题型标识代码（如 single, multiple, judge, blank）
     */
    String getQuestionType();

    /**
     * 是否支持指定题型
     */
    boolean supports(String questionType);

    /**
     * 规范化与清洗答案格式（去除无效空格、符号，映射标准选项）
     */
    String normalize(String rawAnswer);

    /**
     * 校验用户提交答案与标准参考答案是否一致
     */
    boolean isCorrect(String correctAnswer, String userAnswer);

    /**
     * 策略优先级（越小越优先）
     */
    default int getOrder() {
        return 100;
    }
}
