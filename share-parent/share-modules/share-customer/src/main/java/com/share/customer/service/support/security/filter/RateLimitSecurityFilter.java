package com.share.customer.service.support.security.filter;

import com.share.common.core.exception.ServiceException;
import com.share.common.redis.service.RedisService;
import com.share.customer.service.support.security.SecurityCheckContext;
import com.share.customer.service.support.security.SecurityCheckFilter;
import com.share.customer.service.support.security.SecurityFilterChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 责任链节点 1：API 调用频率限流防御过滤器 (Rate Limit Security Filter)
 */
@Slf4j
@Component
public class RateLimitSecurityFilter implements SecurityCheckFilter {

    private static final String AI_RATE_PREFIX = "cs:rate:ask:";
    private static final int AI_RATE_LIMIT = 10;
    private static final long AI_RATE_WINDOW_SECONDS = 60L;

    private final RedisService redisService;

    public RateLimitSecurityFilter(RedisService redisService) {
        this.redisService = redisService;
    }

    @Override
    public void doFilter(SecurityCheckContext context, SecurityFilterChain chain) {
        if (context == null) {
            return;
        }
        Long userId = context.getUserId();
        if (userId != null) {
            try {
                long count = redisService.increment(AI_RATE_PREFIX + userId, AI_RATE_WINDOW_SECONDS, TimeUnit.SECONDS);
                if (count > AI_RATE_LIMIT) {
                    context.block("咨询请求过于频繁，请稍后再试");
                    throw new ServiceException("咨询请求过于频繁，请稍后再试");
                }
            } catch (ServiceException ex) {
                throw ex;
            } catch (RuntimeException ignored) {
                // Redis 异常时平滑降级
            }
        }
        chain.doFilter(context);
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
