package com.share.customer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.common.redis.service.RedisService;
import com.share.customer.config.CustomerAiProperties;
import com.share.customer.domain.CustomerAiConfig;
import com.share.customer.domain.CustomerMessage;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Pixel 第三方 AI 客户端。
 *
 * <p>客户端只允许访问 https://api.ai-pixel.online，支持 Responses 和
 * Chat Completions 两类兼容格式，方便后续只修改 Nacos 配置即可切换接口路径。
 * 请求和响应均按 JSON 协议声明，降低第三方网关内容协商不一致的概率。</p>
 */
@Service
public class CustomerAiClient {
    private static final Logger log = LoggerFactory.getLogger(CustomerAiClient.class);
    private static final String SECRET_KEY = "customer:ai:secret";
    private static final String ALLOWED_HOST = "api.ai-pixel.online";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RedisService redisService;
    private final CustomerAiProperties properties;

    public CustomerAiClient(RestTemplate restTemplate, ObjectMapper objectMapper,
            RedisService redisService, CustomerAiProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.redisService = redisService;
        this.properties = properties;
    }

    /**
     * 调用第三方 AI。未启用、未配置 Key、地址不在允许范围或调用失败时返回 null，
     * 由上层使用本地知识库降级回答。
     */
    public AiReply ask(CustomerAiConfig config, List<CustomerMessage> history, String userMessage) {
        return ask(config, history, userMessage, null);
    }

    /**
     * 使用本次请求临时传入的 Key 调用 Pixel API。
     *
     * <p>临时 Key 优先级高于 Redis/环境配置，但只存在于当前方法调用栈中，
     * 不会被缓存或持久化。</p>
     */
    public AiReply ask(CustomerAiConfig config, List<CustomerMessage> history, String userMessage,
            String requestApiKey) {
        if (config == null || !Integer.valueOf(1).equals(config.getEnabled())) {
            return null;
        }
        String secret = resolveSecret(requestApiKey);
        if (secret == null || secret.isBlank()) {
            return null;
        }
        String url = buildUrl(config.getBaseUrl(), config.getEndpointPath());
        if (url == null) {
            log.warn("客服 AI 地址未通过第三方 Pixel 地址校验");
            return null;
        }

        Map<String, Object> requestBody = buildRequest(config, history, userMessage);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(secret);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        int retryCount = Math.min(Math.max(config.getMaxRetries() == null ? 0 : config.getMaxRetries(), 0), 3);
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    AiReply reply = parseReply(response.getBody(), config.getModel());
                    if (reply != null && reply.getContent() != null && !reply.getContent().isBlank()) {
                        return reply;
                    }
                    log.warn("第三方 AI 返回成功但没有可识别的文本内容");
                    return null;
                }
                log.warn("第三方 AI 返回非成功状态: {}", response.getStatusCode().value());
            } catch (RuntimeException ex) {
                log.warn("调用第三方 Pixel AI 失败，第{}次尝试", attempt + 1, ex);
            }
            if (attempt < retryCount) {
                try {
                    Thread.sleep(200L * (attempt + 1));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    private Map<String, Object> buildRequest(CustomerAiConfig config, List<CustomerMessage> history,
            String userMessage) {
        String endpoint = config.getEndpointPath() == null ? "" : config.getEndpointPath().toLowerCase();
        List<Map<String, String>> messages = new ArrayList<>();
        if (history != null) {
            history.stream().skip(Math.max(0, history.size() - 12L)).forEach(message -> {
                String role = message.getMessageType() != null && message.getMessageType() == 1
                        ? "user" : "assistant";
                Map<String, String> item = new LinkedHashMap<>();
                item.put("role", role);
                item.put("content", message.getContent());
                messages.add(item);
            });
        }
        // 当前问题必须放进发送给第三方的上下文中。此前 Responses 分支先把 input
        // 放入 body、再追加当前问题，导致首条咨询实际没有提交给 AI。
        Map<String, String> current = new LinkedHashMap<>();
        current.put("role", "user");
        current.put("content", userMessage);
        messages.add(current);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModel());
        if (endpoint.contains("chat/completions")) {
            body.put("messages", messages);
        } else {
            body.put("input", messages);
            body.put("instructions", config.getSystemPrompt());
        }
        return body;
    }

    private AiReply parseReply(String body, String model) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String text = firstText(root);
            if (text == null || text.isBlank()) {
                return null;
            }
            Integer tokenUsage = null;
            JsonNode usage = root.path("usage");
            if (usage.has("total_tokens")) {
                tokenUsage = usage.path("total_tokens").asInt();
            } else if (usage.has("input_tokens") || usage.has("output_tokens")) {
                tokenUsage = usage.path("input_tokens").asInt(0) + usage.path("output_tokens").asInt(0);
            }
            return new AiReply(text.trim(), tokenUsage, model);
        } catch (Exception ex) {
            log.warn("解析第三方 AI 响应失败", ex);
            return null;
        }
    }

    private String firstText(JsonNode root) {
        String direct = textValue(root, "output_text");
        if (direct != null) {
            return direct;
        }
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode first = choices.get(0);
            String message = textValue(first.path("message"), "content");
            if (message != null) {
                return message;
            }
            String text = textValue(first, "text");
            if (text != null) {
                return text;
            }
        }
        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (content.isArray()) {
                    for (JsonNode contentItem : content) {
                        String text = textValue(contentItem, "text");
                        if (text != null) {
                            return text;
                        }
                    }
                }
                String text = textValue(item, "text");
                if (text != null) {
                    return text;
                }
            }
        }
        for (String field : List.of("reply", "answer", "content", "message")) {
            String text = textValue(root, field);
            if (text != null) {
                return text;
            }
        }
        JsonNode data = root.path("data");
        if (data.isObject()) {
            return firstText(data);
        }
        return null;
    }

    private String textValue(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isTextual() || value.isNumber() || value.isBoolean()) {
            return value.asText();
        }
        if (value.isArray()) {
            StringBuilder result = new StringBuilder();
            for (JsonNode item : value) {
                if (item.isTextual()) {
                    result.append(item.asText());
                } else {
                    String text = textValue(item, "text");
                    if (text != null) {
                        result.append(text);
                    }
                }
            }
            return result.isEmpty() ? null : result.toString();
        }
        return null;
    }

    private String resolveSecret(String requestApiKey) {
        String requestSecret = normalizeSecret(requestApiKey);
        if (requestSecret != null) {
            return requestSecret;
        }
        try {
            String cached = redisService.getCacheObject(SECRET_KEY);
            String normalizedCached = normalizeSecret(cached);
            if (normalizedCached != null) {
                return normalizedCached;
            }
        } catch (RuntimeException ex) {
            log.warn("读取客服 AI Key 缓存失败，将尝试读取服务端环境配置", ex);
        }
        return normalizeSecret(properties.getSecret());
    }

    private String normalizeSecret(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceFirst("^Bearer\\s+", "").trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String buildUrl(String baseUrl, String endpointPath) {
        try {
            URI base = URI.create(baseUrl == null ? "" : baseUrl.trim());
            if (!"https".equalsIgnoreCase(base.getScheme())
                    || !ALLOWED_HOST.equalsIgnoreCase(base.getHost())) {
                return null;
            }
            String path = endpointPath == null ? "" : endpointPath.trim();
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            return base.toString().replaceAll("/+$", "") + path;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
