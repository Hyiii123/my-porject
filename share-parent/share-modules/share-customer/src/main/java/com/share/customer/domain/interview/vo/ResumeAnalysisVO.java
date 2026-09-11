package com.share.customer.domain.interview.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ResumeAnalysisVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String fileName;
    private String rawContent;
    private String targetJob;
    private String targetCompany;
    private Integer matchScore;
    private String matchLevel;
    private List<String> techTags;
    private List<String> projectHighlights;
    private List<String> resumeGaps;
    private List<String> predictedQuestions;
    private String starAdvice;
    private LocalDateTime updateTime;

    /**
     * 基础分 60 + 五大工程技术维度细分评分明细。
     */
    private List<ScoreDimensionItem> scoreDetails;

    @Data
    public static class ScoreDimensionItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private Integer score;
        private Integer maxScore;
        private String rating;
        private String description;

        public ScoreDimensionItem() {}

        public ScoreDimensionItem(String name, Integer score, Integer maxScore, String rating, String description) {
            this.name = name;
            this.score = score;
            this.maxScore = maxScore;
            this.rating = rating;
            this.description = description;
        }
    }
}
