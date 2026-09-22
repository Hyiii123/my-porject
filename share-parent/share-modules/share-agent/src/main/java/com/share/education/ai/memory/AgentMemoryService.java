package com.share.education.ai.memory;

import com.share.common.redis.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 智能体跨会话长期语义与情景记忆中枢 (Agent Episodic & Semantic Memory Service)。
 *
 * <p>基于 Redis 结构化持久化存储学员过往推演卡点、偏好记录与阶段达成事件，
 * 让学情成长导师与大厂技术总监在多轮跨会话互动中具备记忆持久力，杜绝“次次失忆”。</p>
 */
@Service
public class AgentMemoryService {

    private static final Logger log = LoggerFactory.getLogger(AgentMemoryService.class);

    private static final String REDIS_PREFIX_MEMORY = "edu:ai:memory:user:";
    private static final int MAX_MEMORY_EPISODES = 5;

    private final RedisService redisService;

    @Autowired
    public AgentMemoryService(@Autowired(required = false) RedisService redisService) {
        this.redisService = redisService;
    }

    public record MemoryEpisode(
        long timestamp,
        String role,
        String keyHurdle,
        String compromiseDirective,
        int finalScore
    ) {}

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
            log.info("[AgentMemory] 成功为用户 {} 写入长期情景记忆: 目标={}, 卡点={}", userId, role, hurdle);
        } catch (Exception ex) {
            log.debug("[AgentMemory] 记忆写入异常: {}", ex.getMessage());
        }
    }

    /**
     * 调取该学员的历史情景记忆摘要，供导师节点注入提示词
     */
    public String retrieveEpisodicContext(Long userId, String intendedRole) {
        if (userId == null || userId <= 0 || redisService == null) {
            return "";
        }
        try {
            String key = REDIS_PREFIX_MEMORY + userId;
            List<MemoryEpisode> list = redisService.getCacheList(key);
            if (list != null && !list.isEmpty()) {
                MemoryEpisode latest = list.get(0);
                if (StringUtils.hasText(latest.keyHurdle())) {
                    return String.format("【长期伴学记忆透视】：学员在往期【%s】推演中曾反馈卡点：%s；本次规划需重点优化先修过渡平滑度。",
                        latest.role(), latest.keyHurdle());
                }
            }
        } catch (Exception ignored) {}
        return "";
    }
}
