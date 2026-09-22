package com.share.education.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.common.redis.service.RedisService;
import com.share.education.ai.config.AiRecommendProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 全平台统一第三方大模型适配客户端 (Pixel API / gpt-5.6-luna)。
 *
 * <p>遵循统一第三方大模型规范，全平台共用同一个第三方 API 端点与密钥。
 * 密钥自动与客服微服务 (Redis key: customer:ai:secret) 共享对齐，实现全站单一真相源。
 * 具备排队平滑等待、多智能体语义结果缓存与 20 秒快速熔断自愈能力。</p>
 */
@Component
@Primary
public class ThirdPartyAiClient {

    private static final Logger log = LoggerFactory.getLogger(ThirdPartyAiClient.class);
    private static final String REDIS_SECRET_KEY = "customer:ai:secret";

    private final AiRecommendProperties properties;
    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final RedisService redisService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    private static final long CIRCUIT_BREAKER_DURATION_MS = 20_000L; // 20秒快速自愈
    private final AtomicLong circuitBreakerOpenUntil = new AtomicLong(0L);
    private final ReentrantLock singleConcurrencyGuard = new ReentrantLock();

    private record CacheItem(String content, long expireAt) {}
    private final Map<String, CacheItem> promptCache = new ConcurrentHashMap<>();

    @Autowired
    public ThirdPartyAiClient(AiRecommendProperties properties,
                              @Autowired(required = false) ChatClient chatClient,
                              @Autowired(required = false) ChatModel chatModel,
                              @Autowired(required = false) RedisService redisService) {
        this.properties = properties;
        this.chatClient = chatClient;
        this.chatModel = chatModel;
        this.redisService = redisService;

        int timeout = properties.getTimeoutMs() > 0 ? properties.getTimeoutMs() : 25000;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(timeout);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 解析全站统一的第三方 API Key (优先配置属性，次选 Redis customer:ai:secret 共享密钥)
     */
    public String resolveEffectiveApiKey() {
        String key = properties.getApiKey();
        if (StringUtils.hasText(key) && !"sk-tianji-spring-ai-token".equals(key.trim())) {
            return key.trim();
        }
        if (redisService != null) {
            try {
                String cached = redisService.getCacheObject(REDIS_SECRET_KEY);
                if (StringUtils.hasText(cached)) {
                    return cached.trim().replaceFirst("^Bearer\\s+", "").trim();
                }
            } catch (Exception ex) {
                log.warn("[ThirdPartyAiClient] 从 Redis 读取共享第三方 API 密钥失败: {}", ex.getMessage());
            }
        }
        return null;
    }

    /**
     * 校验第三方大模型是否处于可用状态
     */
    public boolean isAvailable() {
        if (System.currentTimeMillis() < circuitBreakerOpenUntil.get()) {
            return false;
        }
        return properties.isEnabled() && StringUtils.hasText(resolveEffectiveApiKey());
    }

    /**
     * 统一调用第三方 Pixel 大模型 (gpt-5.6-luna)，集成多智能体语义缓存与并发排队
     *
     * @param systemPrompt 系统角色提示词
     * @param userPrompt   用户提示词与上下文
     * @return 第三方大模型生成文本；若失败或熔断则返回 null 触发本地内生规则
     */
    public String generate(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            return null;
        }

        String apiKey = resolveEffectiveApiKey();
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }

        // 1. 语义缓存速查 (TTL 15 分钟)：相同提示词在圆桌推演中毫秒级复用
        String cacheKey = (systemPrompt != null ? systemPrompt : "") + "|||" + (userPrompt != null ? userPrompt : "");
        CacheItem cached = promptCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() < cached.expireAt()) {
            log.info("[ThirdPartyAI] 命中多智能体语义缓存，耗时 0ms 直接交付");
            return cached.content();
        }

        // 2. 单并发保护与平滑排队：允许在 2500ms 窗口内平滑等待上一个智能体调用完成
        boolean acquired = false;
        try {
            acquired = singleConcurrencyGuard.tryLock(2500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }

        if (!acquired) {
            log.info("[ThirdPartyAI] 超过单并发保护等待阈值(2500ms)，平滑降级至本地确定性知识图谱规则");
            return null;
        }

        try {
            String baseUrl = properties.getBaseUrl();
            if (!StringUtils.hasText(baseUrl)) {
                baseUrl = "https://ai-pixel.online";
            }
            baseUrl = baseUrl.trim().replaceAll("/v1/?$", "");
            String url = baseUrl + "/v1/chat/completions";

            List<Map<String, String>> messages = new ArrayList<>();
            if (StringUtils.hasText(systemPrompt)) {
                Map<String, String> sysMsg = new LinkedHashMap<>();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt.trim());
                messages.add(sysMsg);
            }

            Map<String, String> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt != null ? userPrompt.trim() : "");
            messages.add(userMsg);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("Authorization", "Bearer " + apiKey);

            // 多通道智能轮询模型备用池 (主通道 + 降级备用通道)
            List<String> modelCandidates = new ArrayList<>();
            if (StringUtils.hasText(properties.getModel())) {
                modelCandidates.add(properties.getModel().trim());
            }
            for (String fallbackModel : List.of("gpt-5.6-luna", "deepseek-v3", "qwen-turbo")) {
                if (!modelCandidates.contains(fallbackModel)) {
                    modelCandidates.add(fallbackModel);
                }
            }

            for (String model : modelCandidates) {
                try {
                    Map<String, Object> requestBody = new LinkedHashMap<>();
                    requestBody.put("model", model);
                    requestBody.put("messages", messages);
                    requestBody.put("temperature", properties.getTemperature() > 0 ? properties.getTemperature() : 0.6);
                    requestBody.put("max_tokens", 350);

                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                    ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        JsonNode root = objectMapper.readTree(response.getBody());
                        String content = extractContent(root);
                        if (StringUtils.hasText(content)) {
                            String clean = content.trim();
                            log.info("[ThirdPartyAI] 成功调用第三方大模型通道 ({} / {}), 字符数: {}", baseUrl, model, clean.length());
                            promptCache.put(cacheKey, new CacheItem(clean, System.currentTimeMillis() + 15 * 60 * 1000L));
                            if (promptCache.size() > 400) {
                                promptCache.clear();
                            }
                            return clean;
                        }
                    }
                } catch (Exception channelEx) {
                    log.warn("[ThirdPartyAI] 候选模型 [{}] 调用失败，尝试智能轮询下一备用通道: {}", model, channelEx.getMessage());
                }
            }
        } catch (Exception ex) {
            long resumeTime = System.currentTimeMillis() + CIRCUIT_BREAKER_DURATION_MS;
            circuitBreakerOpenUntil.set(resumeTime);
            String model = properties.getModel() != null ? properties.getModel() : "gpt-5.6-luna";
            if (ex.getMessage() != null && (ex.getMessage().contains("429") || ex.getMessage().contains("Concurrency"))) {
                log.warn("[ThirdPartyAI] 第三方大模型 (Pixel / {}) 触发账号单并发限流 (HTTP 429)，已触发20秒保护降级至本地知识图谱规则", model);
            } else {
                log.warn("[ThirdPartyAI] 调用第三方大模型 (Pixel / {}) 发生网络抖动或超时，已触发20秒快速熔断降级至本地知识图谱规则: {}", model, ex.getMessage());
            }
        } finally {
            if (singleConcurrencyGuard.isHeldByCurrentThread()) {
                singleConcurrencyGuard.unlock();
            }
        }

        return null;
    }

    private String extractContent(JsonNode root) {
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.has("error") || root.has("errcode")) {
            log.warn("[ThirdPartyAI] 返回错误报文: {}", root);
            return null;
        }
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode first = choices.get(0);
            JsonNode message = first.path("message");
            if (message.has("content")) {
                return message.path("content").asText();
            }
        }
        return null;
    }
}
