package com.share.customer.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.crypto.AesCryptoUtil;
import com.share.common.redis.service.RedisService;
import com.share.customer.config.CustomerAiProperties;
import com.share.customer.domain.CustomerAiConfig;
import com.share.customer.mapper.CustomerAiConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 客服与智能体安全防护屏障（租户越权隔离、接口限流、API Key加解密、URL白名单）
 */
@Slf4j
@Component
public class CustomerSecurityShield {

    public static final String SECRET_KEY = "customer:ai:secret";
    public static final String FAQ_CACHE_PREFIX = "cs:faq:list:";
    private static final String AI_RATE_PREFIX = "cs:rate:ask:";
    private static final int AI_RATE_LIMIT = 10;
    private static final long AI_RATE_WINDOW_SECONDS = 60L;
    private static final long CONFIG_ID = 1L;

    private static final Pattern CROSS_USER_RECON_PATTERN = Pattern.compile(
            "(?:查|看|调取|修改|删除|查查|查下|查询|获取).*(?:用户|学员|账号)\\s*(?:id|编号)?\\s*[:：=]?\\s*\\d+",
            Pattern.CASE_INSENSITIVE
    );

    private final RedisService redisService;
    private final CustomerAiConfigMapper aiConfigMapper;
    private final CustomerAiProperties aiProperties;

    public CustomerSecurityShield(
            RedisService redisService,
            CustomerAiConfigMapper aiConfigMapper,
            CustomerAiProperties aiProperties) {
        this.redisService = redisService;
        this.aiConfigMapper = aiConfigMapper;
        this.aiProperties = aiProperties;
    }

    public boolean isCrossUserAttempt(String lower) {
        if (lower.contains("我的") || lower.contains("我自己的") || lower.contains("本人")) {
            return false;
        }
        if (lower.contains("其他人") || lower.contains("别的用户") || lower.contains("其他用户")
                || lower.contains("别的人") || lower.contains("他人的") || lower.contains("别人的")
                || lower.contains("查张三") || lower.contains("查李四") || lower.contains("查王五")) {
            return true;
        }
        return CROSS_USER_RECON_PATTERN.matcher(lower).find();
    }

    public void enforceAskRateLimit(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            long count = redisService.increment(AI_RATE_PREFIX + userId, AI_RATE_WINDOW_SECONDS, TimeUnit.SECONDS);
            if (count > AI_RATE_LIMIT) {
                throw new ServiceException("咨询请求过于频繁，请稍后再试");
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (RuntimeException ignored) {
            // Redis 是可选基础设施，短时不可用时仍允许服务使用本地知识库。
        }
    }

    public void validatePixelAddress(String baseUrl, String endpointPath) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new ServiceException("AI 地址不能为空");
        }
        try {
            URI uri = URI.create(baseUrl.trim());
            String scheme = uri.getScheme();
            if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
                throw new ServiceException("AI 地址必须使用 http 或 https 协议");
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new ServiceException("AI 域名不能为空");
            }
            String allowed = aiProperties.getAllowedHosts();
            if (allowed != null && !allowed.isBlank() && !"*".equals(allowed.trim())) {
                boolean match = false;
                for (String h : allowed.split(",")) {
                    String trimmed = h.trim();
                    if (trimmed.equalsIgnoreCase(host) || host.endsWith("." + trimmed)) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    throw new ServiceException("AI 地址域名不在系统允许的白名单范围内: " + host);
                }
            }
        } catch (IllegalArgumentException ex) {
            throw new ServiceException("AI 地址格式不正确");
        }
        if (!StringUtils.hasText(endpointPath)) {
            throw new ServiceException("接口路径不能为空");
        }
    }

    public String resolveSecret() {
        try {
            String value = redisService.getCacheObject(SECRET_KEY);
            if (StringUtils.hasText(value)) {
                return value;
            }
        } catch (RuntimeException ignored) {
        }
        try {
            CustomerAiConfig config = aiConfigMapper.selectOne(
                    new LambdaQueryWrapper<CustomerAiConfig>()
                            .eq(CustomerAiConfig::getEnabled, 1)
                            .orderByDesc(CustomerAiConfig::getUpdateTime)
                            .last("LIMIT 1")
            );
            if (config == null) {
                config = aiConfigMapper.selectById(CONFIG_ID);
            }
            if (config != null && StringUtils.hasText(config.getApiKeyCiphertext())) {
                String decrypted = AesCryptoUtil.decrypt(config.getApiKeyCiphertext());
                if (StringUtils.hasText(decrypted)) {
                    try {
                        redisService.setCacheObject(SECRET_KEY, decrypted);
                    } catch (Exception ignored) {}
                    return decrypted;
                }
            }
        } catch (Exception ex) {
            log.warn("解密 AI Key 异常: {}", ex.getMessage());
        }
        return aiProperties.getSecret();
    }

    public void evictFaqCache() {
        try {
            List<String> cacheKeys = List.of(
                    FAQ_CACHE_PREFIX + "5",
                    FAQ_CACHE_PREFIX + "10",
                    FAQ_CACHE_PREFIX + "15",
                    FAQ_CACHE_PREFIX + "20",
                    FAQ_CACHE_PREFIX + "30",
                    FAQ_CACHE_PREFIX + "50"
            );
            redisService.deleteObject(cacheKeys);
        } catch (RuntimeException ignored) {}
    }
}
