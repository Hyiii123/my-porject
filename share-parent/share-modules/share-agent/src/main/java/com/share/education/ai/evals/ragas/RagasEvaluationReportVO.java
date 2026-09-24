package com.share.education.ai.evals.ragas;

import java.io.Serializable;
import java.util.List;

/**
 * RAGAS 自动化质量评估大盘报告
 */
public class RagasEvaluationReportVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int totalSamples;
    private double avgFaithfulness;     // 忠实度 (0.0 ~ 1.0)
    private double avgAnswerRelevance;  // 答案相关度 (0.0 ~ 1.0)
    private double avgContextPrecision; // 检索精确度 (0.0 ~ 1.0)
    private double avgContextRecall;    // 检索召回率 (0.0 ~ 1.0)
    private double compositeRagasScore; // RAGAS 综合调和平均分 (0.0 ~ 1.0)
    private String grade;               // EXCELLENT (>=0.85), GOOD (>=0.75), PASS (>=0.60)
    private long evaluationTimeMs;
    private List<RagasSampleResult> samples;

    public static class RagasSampleResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private String query;
        private int retrievedCount;
        private double faithfulness;
        private double answerRelevance;
        private double contextPrecision;
        private double contextRecall;
        private double sampleScore;
        private String verdict;

        public RagasSampleResult() {}

        public RagasSampleResult(String query, int retrievedCount, double faithfulness,
                                double answerRelevance, double contextPrecision,
                                double contextRecall, double sampleScore, String verdict) {
            this.query = query;
            this.retrievedCount = retrievedCount;
            this.faithfulness = faithfulness;
            this.answerRelevance = answerRelevance;
            this.contextPrecision = contextPrecision;
            this.contextRecall = contextRecall;
            this.sampleScore = sampleScore;
            this.verdict = verdict;
        }

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public int getRetrievedCount() { return retrievedCount; }
        public void setRetrievedCount(int retrievedCount) { this.retrievedCount = retrievedCount; }

        public double getFaithfulness() { return faithfulness; }
        public void setFaithfulness(double faithfulness) { this.faithfulness = faithfulness; }

        public double getAnswerRelevance() { return answerRelevance; }
        public void setAnswerRelevance(double answerRelevance) { this.answerRelevance = answerRelevance; }

        public double getContextPrecision() { return contextPrecision; }
        public void setContextPrecision(double contextPrecision) { this.contextPrecision = contextPrecision; }

        public double getContextRecall() { return contextRecall; }
        public void setContextRecall(double contextRecall) { this.contextRecall = contextRecall; }

        public double getSampleScore() { return sampleScore; }
        public void setSampleScore(double sampleScore) { this.sampleScore = sampleScore; }

        public String getVerdict() { return verdict; }
        public void setVerdict(String verdict) { this.verdict = verdict; }
    }

    public int getTotalSamples() { return totalSamples; }
    public void setTotalSamples(int totalSamples) { this.totalSamples = totalSamples; }

    public double getAvgFaithfulness() { return avgFaithfulness; }
    public void setAvgFaithfulness(double avgFaithfulness) { this.avgFaithfulness = avgFaithfulness; }

    public double getAvgAnswerRelevance() { return avgAnswerRelevance; }
    public void setAvgAnswerRelevance(double avgAnswerRelevance) { this.avgAnswerRelevance = avgAnswerRelevance; }

    public double getAvgContextPrecision() { return avgContextPrecision; }
    public void setAvgContextPrecision(double avgContextPrecision) { this.avgContextPrecision = avgContextPrecision; }

    public double getAvgContextRecall() { return avgContextRecall; }
    public void setAvgContextRecall(double avgContextRecall) { this.avgContextRecall = avgContextRecall; }

    public double getCompositeRagasScore() { return compositeRagasScore; }
    public void setCompositeRagasScore(double compositeRagasScore) { this.compositeRagasScore = compositeRagasScore; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public long getEvaluationTimeMs() { return evaluationTimeMs; }
    public void setEvaluationTimeMs(long evaluationTimeMs) { this.evaluationTimeMs = evaluationTimeMs; }

    public List<RagasSampleResult> getSamples() { return samples; }
    public void setSamples(List<RagasSampleResult> samples) { this.samples = samples; }
}
