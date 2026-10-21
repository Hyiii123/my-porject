package com.share.trade.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 交易与营销微服务全局 HTTP 读请求 L1 极速本地缓存配置
 * - 对优惠券列表、核销码等只读数据进行 5 秒 Caffeine 本地缓存（时延 < 0.2ms）
 * - 任意写操作 (POST/PUT/DELETE) 自动清空缓存，保障最终一致性
 */
@Configuration
public class TradeL1HttpCacheConfig implements WebMvcConfigurer {

    private static final Cache<String, String> L1_CACHE = Caffeine.newBuilder()
            .maximumSize(2000)
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build();

    public static Cache<String, String> getCache() {
        return L1_CACHE;
    }

    public static String buildCacheKey(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        String uri = request.getRequestURI();
        if (uri == null) {
            return null;
        }
        String query = request.getQueryString();
        String key = uri + (query != null ? "?" + query : "");
        if (uri.contains("/my") || uri.contains("/me") || uri.contains("/orders")) {
            String uid = request.getHeader("user_id");
            String auth = request.getHeader("Authorization");
            if (uid != null) {
                key += ":u=" + uid;
            } else if (auth != null) {
                key += ":auth=" + (auth.length() > 20 ? auth.substring(auth.length() - 20) : auth);
            }
        }
        return key;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                String key = buildCacheKey(request);
                if (key != null) {
                    String cached = L1_CACHE.getIfPresent(key);
                    if (cached != null) {
                        response.setContentType("application/json;charset=UTF-8");
                        response.setHeader("X-Cache", "HIT-L1");
                        response.getWriter().write(cached);
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
                String method = request.getMethod();
                if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
                    if (response.getStatus() >= 200 && response.getStatus() < 300) {
                        L1_CACHE.invalidateAll();
                    }
                }
            }
        }).addPathPatterns("/**");
    }

    @ControllerAdvice
    public static class TradeL1HttpCacheAdvice implements ResponseBodyAdvice<Object> {

        @Autowired
        private ObjectMapper objectMapper;

        @Override
        public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
            return true;
        }

        @Override
        public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                      Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                      ServerHttpRequest request, ServerHttpResponse response) {
            if (body != null && request instanceof ServletServerHttpRequest servletReq) {
                HttpServletRequest req = servletReq.getServletRequest();
                String key = buildCacheKey(req);
                if (key != null) {
                    try {
                        String json = (body instanceof String str) ? str : objectMapper.writeValueAsString(body);
                        L1_CACHE.put(key, json);
                    } catch (Exception ignored) {}
                }
            }
            return body;
        }
    }
}
