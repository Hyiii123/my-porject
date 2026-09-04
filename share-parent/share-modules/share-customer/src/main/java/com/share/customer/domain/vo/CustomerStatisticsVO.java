package com.share.customer.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/** 客服管理首页统计数据。 */
@Data
public class CustomerStatisticsVO {
    private Long totalSessions;
    private Long activeSessions;
    private Long todaySessions;
    private Long totalMessages;
    private Long aiMessages;
    private Long fallbackMessages;
    private Long knowledgeCount;
    private Long faqCount;
    private BigDecimal averageScore;
    private Long aiResolved;
    private Integer satisfactionRate;
    private Integer averageMessages;
    private List<TopQuestion> topQuestions;
    private List<TrendItem> trend;

    @Data
    public static class TopQuestion {
        private String question;
        private Long count;

        public TopQuestion() {
        }

        public TopQuestion(String question, Long count) {
            this.question = question;
            this.count = count;
        }
    }

    @Data
    public static class TrendItem {
        private String date;
        private Long sessions;
        private Long resolved;

        public TrendItem() {
        }

        public TrendItem(String date, Long sessions, Long resolved) {
            this.date = date;
            this.sessions = sessions;
            this.resolved = resolved;
        }
    }
}
