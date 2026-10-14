package com.share.education.service.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.share.common.redis.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 工业级双层多级缓存中枢服务 (Two-Level Cache Service)
 * L1: Caffeine 本地堆内极速缓存 (时延 < 0.5ms)
 * L2: Redis 分布式集群持久化缓存
 */
@Service
public class TwoLevelCacheService {

    private static final Logger log = LoggerFactory.getLogger(TwoLevelCacheService.class);
    private static final String NULL_SENTINEL = "__NULL_CACHE_SENTINEL__";

    private final Cache<String, Object> l1Cache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .expireAfterAccess(30, TimeUnit.SECONDS)
            .recordStats()
            .build();

    @Autowired(required = false)
    private RedisService redisService;

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz, Callable<T> loader, long l2Ttl, TimeUnit l2Unit) {
        if (key == null) return null;

        // 1. L1 (Caffeine)
        Object l1Val = l1Cache.getIfPresent(key);
        if (l1Val != null) {
            if (NULL_SENTINEL.equals(l1Val)) return null;
            if (clazz.isInstance(l1Val)) return (T) l1Val;
        }

        // 2. L2 (Redis)
        if (redisService != null) {
            try {
                Object l2Val = redisService.getCacheObject(key);
                if (l2Val != null) {
                    if (NULL_SENTINEL.equals(l2Val)) {
                        l1Cache.put(key, NULL_SENTINEL);
                        return null;
                    }
                    if (clazz.isInstance(l2Val)) {
                        l1Cache.put(key, l2Val);
                        return (T) l2Val;
                    }
                }
            } catch (Exception ex) {
                log.warn("[TwoLevelCache] 读取 L2 Redis 异常: key={}, err={}", key, ex.getMessage());
            }
        }

        // 3. 回源加载防击穿
        synchronized (key.intern()) {
            Object recheck = l1Cache.getIfPresent(key);
            if (recheck != null) {
                return NULL_SENTINEL.equals(recheck) ? null : (T) recheck;
            }

            T loaded = null;
            try {
                if (loader != null) loaded = loader.call();
            } catch (Exception e) {
                log.error("[TwoLevelCache] 回源加载异常: key={}", key, e);
                throw new RuntimeException("缓存回源加载异常: " + key, e);
            }

            if (loaded != null) {
                l1Cache.put(key, loaded);
                if (redisService != null) {
                    try {
                        redisService.setCacheObject(key, loaded, l2Ttl, l2Unit);
                    } catch (Exception e) {
                        log.warn("[TwoLevelCache] 写入 L2 Redis 异常: key={}, err={}", key, e.getMessage());
                    }
                }
            } else {
                l1Cache.put(key, NULL_SENTINEL);
                if (redisService != null) {
                    try {
                        redisService.setCacheObject(key, NULL_SENTINEL, 15L, TimeUnit.SECONDS);
                    } catch (Exception ignored) {}
                }
            }
            return loaded;
        }
    }

    public <T> T get(String key, Class<T> clazz, Callable<T> loader) {
        return get(key, clazz, loader, 10L, TimeUnit.MINUTES);
    }

    public void put(String key, Object value, long l2Ttl, TimeUnit l2Unit) {
        if (key == null) return;
        Object toStore = value == null ? NULL_SENTINEL : value;
        l1Cache.put(key, toStore);
        if (redisService != null) {
            try {
                redisService.setCacheObject(key, toStore, l2Ttl, l2Unit);
            } catch (Exception e) {
                log.warn("[TwoLevelCache] 写入 L2 Redis 异常: key={}, err={}", key, e.getMessage());
            }
        }
    }

    public void evict(String key) {
        if (key == null) return;
        l1Cache.invalidate(key);
        if (redisService != null) {
            try {
                redisService.deleteObject(key);
            } catch (Exception e) {
                log.warn("[TwoLevelCache] 淘汰 L2 Redis 异常: key={}, err={}", key, e.getMessage());
            }
        }
    }

    public void evictPattern(String pattern) {
        if (pattern == null) return;
        l1Cache.invalidateAll();
        if (redisService != null) {
            try {
                Collection<String> keys = redisService.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisService.deleteObject(keys);
                }
            } catch (Exception e) {
                log.warn("[TwoLevelCache] 批量淘汰 L2 Redis 异常: pattern={}", pattern, e);
            }
        }
    }

    public CacheStats getL1Stats() {
        return l1Cache.stats();
    }
}
