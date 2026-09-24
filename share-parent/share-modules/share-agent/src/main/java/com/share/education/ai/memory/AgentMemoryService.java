package com.share.education.ai.memory;

import com.share.common.redis.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 智能体跨会话长期语义与艾宾浩斯遗忘曲线衰减记忆中枢 (Ebbinghaus Forgetting Curve Memory Service)
 *
 * <p>基于认知心理学艾宾浩斯记忆留存曲线公式 $R(t) = e^{-\Delta t / S}$，
 * 动态根据时间流逝计算历史卡点与错题的记忆留存率、复习迫切度，
 * 让学情导师在跨会话导学中对即将遗忘的知识盲区发起临界预警。</p>
 */
@Service
public class AgentMemoryService {

    private static final Logger log = LoggerFactory.getLogger(AgentMemoryService.class);

    private static final String REDIS_PREFIX_MEMORY = "edu:ai:memory:user:";
    private static final int MAX_MEMORY_EPISODES = 10;
    private static final double EBBINGHAUS_CRITICAL_RETENTION = 0.58; // 艾宾浩斯 24h 留存临界阈值 (58%)

    private final RedisService redisService;

    @Autowired
    public AgentMemoryService(@Autowired(required = false) RedisService redisService) {
        this.redisService = redisService;
    }

    public static class MemoryEpisode implements Serializable {
        private static final long serialVersionUID = 1L;

        private long timestamp;
        private String role;
        private String keyHurdle;
        private String compromiseDirective;
        private int finalScore;
        private double retentionRate;  // 0.0 ~ 1.0 (根据当前系统时间与衰减系数动态推演)
        private double urgencyScore;   // 0.0 ~ 100.0 (综合留存率与往期薄弱程度)
        private long nextReviewTime;   // 最佳间隔重复复习时间戳
        private boolean reviewUrgent;  // 是否达到遗忘临界告警

        public MemoryEpisode() {}

        public MemoryEpisode(long timestamp, String role, String keyHurdle, String compromiseDirective, int finalScore) {
            this.timestamp = timestamp;
            this.role = role;
            this.keyHurdle = keyHurdle;
            this.compromiseDirective = compromiseDirective;
            this.finalScore = finalScore;
            computeEbbinghausMetrics();
        }

        /**
         * 重新基于当前时间动态刷新艾宾浩斯留存与衰减指标
         */
        public void computeEbbinghausMetrics() {
            long now = System.currentTimeMillis();
            double elapsedHours = Math.max(0.1, (now - this.timestamp) / 3600000.0);
            // 记忆稳固度系数 S (得分越高初始稳固度越强，基准 36h ~ 144h)
            double stability = Math.max(24.0, (this.finalScore > 0 ? this.finalScore : 60) * 1.2);
            // 艾宾浩斯留存指数 R = e^(-t/S)
            double retention = Math.exp(-elapsedHours / stability);
            this.retentionRate = Math.round(Math.max(0.01, Math.min(1.0, retention)) * 1000.0) / 1000.0;
            // 迫切度: (1 - R) * (100 - 分数)
            double gap = Math.max(10.0, 100.0 - this.finalScore);
            this.urgencyScore = Math.round((1.0 - this.retentionRate) * gap * 10.0) / 10.0;
            this.reviewUrgent = this.retentionRate < EBBINGHAUS_CRITICAL_RETENTION;
            // 间隔重复建议时间: 记忆衰减至 60% 的临界时间
            double targetIntervalHours = stability * 0.51; // ln(1/0.6) ≈ 0.51
            this.nextReviewTime = this.timestamp + (long)(targetIntervalHours * 3600000L);
        }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getKeyHurdle() { return keyHurdle; }
        public void setKeyHurdle(String keyHurdle) { this.keyHurdle = keyHurdle; }

        public String getCompromiseDirective() { return compromiseDirective; }
        public void setCompromiseDirective(String compromiseDirective) { this.compromiseDirective = compromiseDirective; }

        public int getFinalScore() { return finalScore; }
        public void setFinalScore(int finalScore) { this.finalScore = finalScore; }

        public double getRetentionRate() { return retentionRate; }
        public void setRetentionRate(double retentionRate) { this.retentionRate = retentionRate; }

        public double getUrgencyScore() { return urgencyScore; }
        public void setUrgencyScore(double urgencyScore) { this.urgencyScore = urgencyScore; }

        public long getNextReviewTime() { return nextReviewTime; }
        public void setNextReviewTime(long nextReviewTime) { this.nextReviewTime = nextReviewTime; }

        public boolean isReviewUrgent() { return reviewUrgent; }
        public void setReviewUrgent(boolean reviewUrgent) { this.reviewUrgent = reviewUrgent; }
    }

    /**
     * 记录一次具有长期伴学价值的情景记忆片段
     */
    public void recordEpisode(Long userId, String role, String hurdle, String directive, int finalScore) {
        if (userId == null || userId <= 0 || redisService == null) {
            return;
        }
        try {
            String key = REDIS_PREFIX_MEMORY + userId;
            MemoryEpisode episode = new MemoryEpisode(
                System.currentTimeMillis(), role, hurdle, directive, finalScore
            );
            List<MemoryEpisode> list = redisService.getCacheList(key);
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(0, episode);
            if (list.size() > MAX_MEMORY_EPISODES) {
                list = list.subList(0, MAX_MEMORY_EPISODES);
            }
            redisService.setCacheList(key, list);
            redisService.expire(key, 30, TimeUnit.DAYS);
            log.info("[AgentMemory] 成功为用户 {} 写入艾宾浩斯衰减情景记忆: 目标={}, 卡点={}, 留存率={}",
                    userId, role, hurdle, episode.getRetentionRate());
        } catch (Exception ex) {
            log.debug("[AgentMemory] 记忆写入异常: {}", ex.getMessage());
        }
    }

    /**
     * 调取该学员的历史情景记忆摘要，按「艾宾浩斯复习迫切度」加权排序后注入导师提示词
     */
    public String retrieveEpisodicContext(Long userId, String intendedRole) {
        if (userId == null || userId <= 0 || redisService == null) {
            return "";
        }
        try {
            List<MemoryEpisode> list = getMemoryProfile(userId);
            if (list != null && !list.isEmpty()) {
                // 优先选取遗忘迫切度最高的历史薄弱点
                MemoryEpisode mostUrgent = list.stream()
                        .max(Comparator.comparingDouble(MemoryEpisode::getUrgencyScore))
                        .orElse(list.get(0));

                if (StringUtils.hasText(mostUrgent.getKeyHurdle())) {
                    if (mostUrgent.isReviewUrgent()) {
                        return String.format("【艾宾浩斯遗忘临界预警】：学员往期【%s】中卡点【%s】记忆留存率已衰减至 %.1f%%（迫切度 %.1f 分），本次规划须前置巩固该核心先修模块！",
                                mostUrgent.getRole(), mostUrgent.getKeyHurdle(), mostUrgent.getRetentionRate() * 100.0, mostUrgent.getUrgencyScore());
                    } else {
                        return String.format("【长期伴学记忆透视】：学员往期【%s】推演卡点【%s】（记忆留存良好 %.1f%%）；可直接进入进阶阶段过渡。",
                                mostUrgent.getRole(), mostUrgent.getKeyHurdle(), mostUrgent.getRetentionRate() * 100.0);
                    }
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    /**
     * 获取学员完整的艾宾浩斯记忆留存曲线画像
     */
    public List<MemoryEpisode> getMemoryProfile(Long userId) {
        if (userId == null || userId <= 0 || redisService == null) {
            return Collections.emptyList();
        }
        try {
            String key = REDIS_PREFIX_MEMORY + userId;
            List<?> rawList = redisService.getCacheList(key);
            if (rawList != null) {
                List<MemoryEpisode> result = new ArrayList<>();
                for (Object item : rawList) {
                    if (item instanceof MemoryEpisode ep) {
                        ep.computeEbbinghausMetrics();
                        result.add(ep);
                    } else if (item instanceof Map<?, ?> map) {
                        MemoryEpisode ep = parseEpisodeMap(map);
                        if (ep != null) {
                            ep.computeEbbinghausMetrics();
                            result.add(ep);
                        }
                    }
                }
                return result;
            }
        } catch (Exception ex) {
            log.debug("[AgentMemory] 读取画像异常: {}", ex.getMessage());
        }
        return Collections.emptyList();
    }

    private MemoryEpisode parseEpisodeMap(Map<?, ?> map) {
        try {
            long ts = map.containsKey("timestamp") && map.get("timestamp") instanceof Number num ? num.longValue() : System.currentTimeMillis();
            String role = map.containsKey("role") && map.get("role") != null ? map.get("role").toString() : "";
            String hurdle = map.containsKey("keyHurdle") && map.get("keyHurdle") != null ? map.get("keyHurdle").toString() : "";
            String directive = map.containsKey("compromiseDirective") && map.get("compromiseDirective") != null ? map.get("compromiseDirective").toString() : "";
            int score = map.containsKey("finalScore") && map.get("finalScore") instanceof Number num ? num.intValue() : 70;
            return new MemoryEpisode(ts, role, hurdle, directive, score);
        } catch (Exception e) {
            return null;
        }
    }
}
