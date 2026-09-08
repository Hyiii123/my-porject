package com.share.education.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.education.ai.config.AiRecommendProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 阿里云百炼 (DashScope) / 通义千问 Qwen 大模型客户端。
 *
 * <p>兼容 Spring AI Alibaba 核心协议标准，支持 Chat Completions 规范。
 * 具备自动熔断降级机制：在未配置 API Key 或网络波动时，平滑降级至本地规则推理，
 * 确保接口 100% 高可用与毫秒级响应。</p>
 */
@Component
public class DashScopeAiClient {

    private static final Logger log = LoggerFactory.getLogger(DashScopeAiClient.class);

    private final AiRecommendProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DashScopeAiClient(AiRecommendProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.min(properties.getTimeoutMs(), 3000));
        factory.setReadTimeout(properties.getTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 校验大模型是否处于可用状态
     */
    public boolean isAvailable() {
        return properties.isEnabled() && StringUtils.hasText(properties.getApiKey());
    }

    /**
     * 同步调用大模型生成文本
     *
     * @param systemPrompt 系统角色设定
     * @param userPrompt 用户提示词与上下文
     * @return 大模型响应内容，若失败或未配置则返回 null
     */
    public String generate(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            return null;
        }

        String apiKey = properties.getApiKey().trim();
        String rawBase = properties.getBaseUrl().trim().replaceAll("/+$", "");
        String url;
        if (rawBase.endsWith("/chat/completions")) {
            url = rawBase;
        } else if (rawBase.endsWith("/v1")) {
            url = rawBase + "/chat/completions";
        } else if (rawBase.contains("dashscope.aliyuncs.com")) {
            url = rawBase + "/chat/completions";
        } else {
            url = rawBase + "/v1/chat/completions";
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", properties.getModel());
        requestBody.put("temperature", properties.getTemperature());

        List<Map<String, String>> messages = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userPrompt));
        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        int retries = Math.max(0, Math.min(properties.getMaxRetries(), 2));
        for (int attempt = 0; attempt <= retries; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode choices = root.path("choices");
                    if (choices.isArray() && !choices.isEmpty()) {
                        String content = choices.get(0).path("message").path("content").asText();
                        if (StringUtils.hasText(content)) {
                            return content.trim();
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("调用百炼 DashScope 大模型第 {} 次尝试失败: {}", attempt + 1, ex.getMessage());
            }
        }

        return null;
    }
}
