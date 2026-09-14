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
import org.springframework.util.StringUtils;
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
        String secret = resolveSecret(config, requestApiKey);
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

    /**
     * 执行原始单次 Prompt 对话（用于模拟面试出题、追问、代码审计与报告生成）。
     */
    public String askRaw(CustomerAiConfig config, String systemPrompt, String userMessage) {
        if (config == null || !Integer.valueOf(1).equals(config.getEnabled())) {
            return null;
        }
        String secret = resolveSecret(config, null);
        if (secret == null || secret.isBlank()) {
            return null;
        }
        String url = buildUrl(config.getBaseUrl(), config.getEndpointPath());
        if (url == null) {
            log.warn("Pixel AI 地址校验未通过: {}", config.getBaseUrl());
            return null;
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", config.getModel() != null ? config.getModel() : "gpt-5.6-luna");
        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            Map<String, String> sys = new LinkedHashMap<>();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
            messages.add(sys);
        }
        Map<String, String> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", userMessage);
        messages.add(user);

        String endpoint = config.getEndpointPath() == null ? "" : config.getEndpointPath().toLowerCase();
        if (endpoint.contains("chat/completions")) {
            requestBody.put("messages", messages);
        } else {
            requestBody.put("input", messages);
            requestBody.put("instructions", systemPrompt);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(secret);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        int retryCount = Math.min(Math.max(config.getMaxRetries() == null ? 0 : config.getMaxRetries(), 0), 2);

        // 让 DB cs_ai_config.timeout_ms 真正生效：创建 per-request RestTemplate
        RestTemplate rt = this.restTemplate;
        if (config.getTimeoutMs() != null && config.getTimeoutMs() > 0) {
            org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                    new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Math.min(config.getTimeoutMs(), 10000));
            factory.setReadTimeout(config.getTimeoutMs());
            rt = new RestTemplate(factory);
        }

        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                ResponseEntity<String> response = rt.postForEntity(url, entity, String.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    AiReply reply = parseReply(response.getBody(), config.getModel());
                    if (reply != null && reply.getContent() != null && !reply.getContent().isBlank()) {
                        return reply.getContent().trim();
                    }
                }
            } catch (Exception ex) {
                log.warn("调用 Pixel AI askRaw 异常 (尝试 {}): {}", attempt + 1, ex.getMessage());
            }
            if (attempt < retryCount) {
                try {
                    Thread.sleep(200L * (attempt + 1));
                } catch (InterruptedException ie) {
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

        // BUG-47: 倒序检查历史消息字符预算（上限 6000 字符），防止长上下文导致 HTTP 400 失败
        if (history != null && !history.isEmpty()) {
            int charBudget = 6000;
            int currentChars = 0;
            List<CustomerMessage> recent = history.stream()
                    .skip(Math.max(0, history.size() - 12L))
                    .toList();
            List<Map<String, String>> budgetMessages = new ArrayList<>();
            for (int i = recent.size() - 1; i >= 0; i--) {
                CustomerMessage m = recent.get(i);
                String text = m.getContent() == null ? "" : m.getContent().trim();
                if (text.isEmpty()) continue;
                if (currentChars + text.length() > charBudget) {
                    int remaining = charBudget - currentChars;
                    if (remaining > 100) {
                        Map<String, String> item = new LinkedHashMap<>();
                        item.put("role", m.getMessageType() != null && m.getMessageType() == 1 ? "user" : "assistant");
                        item.put("content", text.substring(text.length() - remaining));
                        budgetMessages.add(0, item);
                    }
                    break;
                }
                currentChars += text.length();
                Map<String, String> item = new LinkedHashMap<>();
                item.put("role", m.getMessageType() != null && m.getMessageType() == 1 ? "user" : "assistant");
                item.put("content", text);
                budgetMessages.add(0, item);
            }
            messages.addAll(budgetMessages);
        }

        // 限制单条用户输入最大长度，防止单次请求过大直接触发网关拦截
        String safeUserMessage = userMessage != null && userMessage.length() > 3000
                ? userMessage.substring(0, 3000) : (userMessage != null ? userMessage.trim() : "");
        Map<String, String> current = new LinkedHashMap<>();
        current.put("role", "user");
        current.put("content", safeUserMessage);
        messages.add(current);

        // BUG-34: OpenAI /v1/chat/completions 标准协议必须将 systemPrompt 放入 messages 的首位
        if (endpoint.contains("chat/completions") && StringUtils.hasText(config.getSystemPrompt())) {
            Map<String, String> sysMsg = new LinkedHashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", config.getSystemPrompt().trim());
            messages.add(0, sysMsg);
        }

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
        if (root == null || root.isNull()) {
            return null;
        }
        // 防御：若返回根节点包含明确的 error 报错标识，绝不可将错误提示信息提取为 AI 答案
        if (root.has("error") || root.has("errcode")) {
            log.warn("第三方 AI 返回错误报文: {}", root);
            return null;
        }
        if (root.has("code") && root.get("code").asInt(0) != 200 && root.get("code").asInt(0) != 0) {
            log.warn("第三方 AI 返回非正常业务状态码: {}", root);
            return null;
        }

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

    private String resolveSecret(CustomerAiConfig config, String requestApiKey) {
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
            log.warn("读取客服 AI Key 缓存失败，将尝试从加密配置恢复", ex);
        }
        if (config != null && com.share.common.core.utils.StringUtils.hasText(config.getApiKeyCiphertext())) {
            String decrypted = com.share.common.core.utils.crypto.AesCryptoUtil.decrypt(config.getApiKeyCiphertext());
            if (com.share.common.core.utils.StringUtils.hasText(decrypted)) {
                try {
                    redisService.setCacheObject(SECRET_KEY, decrypted);
                } catch (Exception ignored) {}
                return decrypted;
            }
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

    private boolean isHostAllowed(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String configured = properties.getAllowedHosts();
        if (configured == null || configured.isBlank() || "*".equals(configured.trim())) {
            return true;
        }
        for (String allowed : configured.split(",")) {
            String trimmed = allowed.trim();
            if (trimmed.equalsIgnoreCase(host) || host.endsWith("." + trimmed)) {
                return true;
            }
        }
        return false;
    }

    private String buildUrl(String baseUrl, String endpointPath) {
        try {
            URI base = URI.create(baseUrl == null ? "" : baseUrl.trim());
            String scheme = base.getScheme();
            if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
                return null;
            }
            String host = base.getHost();
            if (!isHostAllowed(host)) {
                log.warn("AI 目标域名 {} 不在允许的白名单列表中: {}", host, properties.getAllowedHosts());
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
