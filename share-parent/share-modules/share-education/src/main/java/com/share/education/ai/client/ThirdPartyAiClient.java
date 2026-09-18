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
import java.util.concurrent.atomic.AtomicLong;

/**
 * 全平台统一第三方大模型适配客户端 (Pixel API / gpt-5.6-luna)。
 *
 * <p>遵循统一第三方大模型规范，全平台共用同一个第三方 API 端点与密钥。
 * 密钥自动与客服微服务 (Redis key: customer:ai:secret) 共享对齐，实现全站单一真相源。
 * 具备高可用弹性熔断机制：若第三方 API 出现网络超时或抖动，瞬间无感降级至本地确定性知识图谱与拓扑规划规则。</p>
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
    private final java.util.concurrent.locks.ReentrantLock singleConcurrencyGuard = new java.util.concurrent.locks.ReentrantLock();

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
     * 统一调用第三方 Pixel 大模型 (gpt-5.6-luna)
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

        // 单并发保护：第三方 Pixel 账号设有限制，严禁内部并发调用导致 429 限流
        if (!singleConcurrencyGuard.tryLock()) {
            log.info("[ThirdPartyAI] 检测到已有大模型请求进行中，基于单并发配额保护直接启用本地知识图谱规则");
            return null;
        }

        try {
            String baseUrl = properties.getBaseUrl();
            if (!StringUtils.hasText(baseUrl)) {
                baseUrl = "https://ai-pixel.online";
            }
            baseUrl = baseUrl.trim().replaceAll("/v1/?$", "");
            String url = baseUrl + "/v1/chat/completions";

            String model = properties.getModel();
            if (!StringUtils.hasText(model)) {
                model = "gpt-5.6-luna";
            }

            List<Map<String, String>> messages = new ArrayList<>();
            if (StringUtils.hasText(systemPrompt)) {
                Map<String, String> sysMsg = new LinkedHashMap<>();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt.trim());
                messages.add(sysMsg);
            }
            Map<String, String> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", StringUtils.hasText(userPrompt) ? userPrompt.trim() : "请分析");
            messages.add(userMsg);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", properties.getTemperature());
            requestBody.put("max_tokens", 150);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String content = extractContent(root);
                if (StringUtils.hasText(content)) {
                    log.info("[ThirdPartyAI] 成功调用第三方大模型 (Pixel / {}), 返回文本长度: {}", model, content.length());
                    return content.trim();
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

    public ChatClient getChatClient() {
        return chatClient;
    }

    public ChatModel getChatModel() {
        return chatModel;
    }

    public AiRecommendProperties getProperties() {
        return properties;
    }
}
