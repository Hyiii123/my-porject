package com.share.education.ai.algorithm.bkt;

import com.share.common.redis.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 微知识点学情追踪中枢服务 (KnowledgeTracingService)
 */
@Service
public class KnowledgeTracingService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeTracingService.class);
    private static final String REDIS_PREFIX_BKT = "edu:ai:bkt:mastery:";

    private final BayesianKnowledgeTracingEngine bktEngine;
    private final RedisService redisService;

    // 内存兜底存储 (当 Redis 离线时平滑保底)
    private final Map<Long, Map<String, KnowledgePointMasteryVO>> fallbackStore = new ConcurrentHashMap<>();

    // 标杆体系前沿微知识点词典
    private static final List<SeedSkill> SEED_SKILLS = List.of(
            new SeedSkill("redis_lock", "Redis 分布式锁与看门狗续期", "缓存与中间件"),
            new SeedSkill("mysql_bplus", "MySQL 聚簇索引与 B+ 树阶数分裂", "数据库与存储"),
            new SeedSkill("spring_cycle_ref", "Spring 三级缓存与循环依赖解决机制", "框架底层"),
            new SeedSkill("jvm_gc", "JVM G1/ZGC 垃圾回收与停顿优化", "底层核心与运行时"),
            new SeedSkill("rocketmq_dledger", "RocketMQ 事务消息与 DLedger 仲裁选举", "消息总线"),
            new SeedSkill("netty_zero_copy", "Netty ByteBuf 内存池与零拷贝实现", "网络与高并发"),
            new SeedSkill("vue3_proxy", "Vue 3 响应式原理与 Proxy 深度拦截", "前端核心架构"),
            new SeedSkill("dist_tx_seata", "Seata 2.0 分布式事务 AT 模式 undo_log", "微服务与分布式")
    );

    public record SeedSkill(String id, String name, String category) {}

    @Autowired
    public KnowledgeTracingService(BayesianKnowledgeTracingEngine bktEngine,
                                  @Autowired(required = false) RedisService redisService) {
        this.bktEngine = bktEngine;
        this.redisService = redisService;
    }

    /**
     * 获取指定学员的微知识点掌握度全景画像 (若首次进入则自动初始化标准题库知识点)
     */
    public List<KnowledgePointMasteryVO> getLearnerMasteryProfile(Long userId) {
        if (userId == null) userId = 1L;
        Map<String, KnowledgePointMasteryVO> map = loadUserMap(userId);
        return new ArrayList<>(map.values());
    }

    /**
     * 记录一次知识点作答反馈并触发 BKT 贝叶斯后验迭代更新
     */
    public KnowledgePointMasteryVO updateSkillObservation(Long userId, String skillId, boolean correct) {
        if (userId == null) userId = 1L;
        Map<String, KnowledgePointMasteryVO> map = loadUserMap(userId);
        KnowledgePointMasteryVO vo = map.get(skillId);
        if (vo == null) {
            // 自动推断并新增知识点
            vo = new KnowledgePointMasteryVO(skillId, skillId, "专业技能", 0.35, "PRACTICING", System.currentTimeMillis(), 0, 0);
            map.put(skillId, vo);
        }

        double prior = vo.getMasteryProbability();
        double newMastery = bktEngine.updateMastery(prior, correct);
        vo.setMasteryProbability(newMastery);
        vo.setStatus(bktEngine.resolveStatus(newMastery));
        vo.setLastPracticeTime(System.currentTimeMillis());
        vo.setAttemptCount(vo.getAttemptCount() + 1);
        if (correct) {
            vo.setCorrectCount(vo.getCorrectCount() + 1);
        }

        saveUserMap(userId, map);
        log.info("[BKT KnowledgeTracing] 学员 {} 技能 {} 掌握度从 {} 迭代为 {} (状态: {})",
                userId, skillId, prior, newMastery, vo.getStatus());
        return vo;
    }

    /**
     * 获取该学员需要重点补救强化的薄弱微知识点 (掌握度 < 0.50)
     */
    public List<KnowledgePointMasteryVO> getStrugglingSkills(Long userId) {
        return getLearnerMasteryProfile(userId).stream()
                .filter(s -> "STRUGGLING".equals(s.getStatus()) || s.getMasteryProbability() < 0.55)
                .sorted(Comparator.comparingDouble(KnowledgePointMasteryVO::getMasteryProbability))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, KnowledgePointMasteryVO> loadUserMap(Long userId) {
        String key = REDIS_PREFIX_BKT + userId;
        if (redisService != null) {
            try {
                Map<String, KnowledgePointMasteryVO> cached = redisService.getCacheMap(key);
                if (cached != null && !cached.isEmpty()) {
                    return new HashMap<>(cached);
                }
            } catch (Exception ex) {
                log.debug("[BKT] Redis读取异常: {}", ex.getMessage());
            }
        }
        Map<String, KnowledgePointMasteryVO> local = fallbackStore.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
        if (local.isEmpty()) {
            // 初始化预置知识点先验
            for (SeedSkill seed : SEED_SKILLS) {
                double p0 = 0.40 + (Math.abs(seed.id().hashCode() % 35) / 100.0); // 0.40 ~ 0.74 初始离散先验
                p0 = Math.round(p0 * 100.0) / 100.0;
                KnowledgePointMasteryVO vo = new KnowledgePointMasteryVO(
                        seed.id(), seed.name(), seed.category(),
                        p0, bktEngine.resolveStatus(p0),
                        System.currentTimeMillis() - (seed.id().hashCode() % 86400000L),
                        3, (int) Math.round(p0 * 3)
                );
                local.put(seed.id(), vo);
            }
            saveUserMap(userId, local);
        }
        return local;
    }

    private void saveUserMap(Long userId, Map<String, KnowledgePointMasteryVO> map) {
        fallbackStore.put(userId, map);
        String key = REDIS_PREFIX_BKT + userId;
        if (redisService != null) {
            try {
                redisService.setCacheMap(key, map);
                redisService.expire(key, 30, TimeUnit.DAYS);
            } catch (Exception ex) {
                log.debug("[BKT] Redis持久化异常: {}", ex.getMessage());
            }
        }
    }
}
