package com.share.education.ai.algorithm.bkt;

import java.io.Serializable;

/**
 * 微知识点贝叶斯掌握度画像
 */
public class KnowledgePointMasteryVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String skillId;
    private String skillName;
    private String category;
    private double masteryProbability; // 0.0 ~ 1.0
    private String status; // MASTERED (>=0.85), PRACTICING (0.50~0.85), STRUGGLING (<0.50)
    private long lastPracticeTime;
    private int attemptCount;
    private int correctCount;

    public KnowledgePointMasteryVO() {}

    public KnowledgePointMasteryVO(String skillId, String skillName, String category,
                                  double masteryProbability, String status,
                                  long lastPracticeTime, int attemptCount, int correctCount) {
        this.skillId = skillId;
        this.skillName = skillName;
        this.category = category;
        this.masteryProbability = masteryProbability;
        this.status = status;
        this.lastPracticeTime = lastPracticeTime;
        this.attemptCount = attemptCount;
        this.correctCount = correctCount;
    }

    public String getSkillId() { return skillId; }
    public void setSkillId(String skillId) { this.skillId = skillId; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getMasteryProbability() { return masteryProbability; }
    public void setMasteryProbability(double masteryProbability) { this.masteryProbability = masteryProbability; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getLastPracticeTime() { return lastPracticeTime; }
    public void setLastPracticeTime(long lastPracticeTime) { this.lastPracticeTime = lastPracticeTime; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
}
