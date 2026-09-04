package com.share.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.common.redis.service.RedisService;
import com.share.customer.config.CustomerAiProperties;
import com.share.customer.domain.CustomerAiConfig;
import com.share.customer.domain.CustomerMessage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 第三方 Pixel AI 客户端契约测试。
 *
 * <p>测试使用本地 HTTP mock，不访问官方 OpenAI，也不会消耗真实第三方 Key。</p>
 */
class CustomerAiClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private RedisService redisService;
    private CustomerAiProperties properties;
    private CustomerAiClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        redisService = mock(RedisService.class);
        when(redisService.getCacheObject("customer:ai:secret")).thenReturn("pixel-test-key");
        properties = new CustomerAiProperties();
        properties.setSecret("");
        client = new CustomerAiClient(restTemplate, new ObjectMapper(), redisService, properties);
    }

    @Test
    void shouldParsePixelResponsesPayloadAndKeepConversationHistory() {
        CustomerAiConfig config = config("/v1/responses");
        CustomerMessage historyMessage = new CustomerMessage();
        historyMessage.setMessageType(1);
        historyMessage.setContent("之前的问题");

        server.expect(requestTo("https://api.ai-pixel.online/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer pixel-test-key"))
                .andExpect(jsonPath("$.model").value("gpt-5.5"))
                .andExpect(jsonPath("$.instructions").isNotEmpty())
                .andExpect(jsonPath("$.input[0].role").value("user"))
                .andExpect(jsonPath("$.input[1].role").value("user"))
                .andExpect(jsonPath("$.input[1].content").value("现在的问题"))
                .andRespond(withSuccess(
                        "{\"id\":\"resp_1\",\"output_text\":\"已收到，我来帮你处理。\","
                                + "\"usage\":{\"input_tokens\":7,\"output_tokens\":5}}",
                        MediaType.APPLICATION_JSON));

        AiReply reply = client.ask(config, List.of(historyMessage), "现在的问题");

        assertThat(reply).isNotNull();
        assertThat(reply.getContent()).isEqualTo("已收到，我来帮你处理。");
        assertThat(reply.getTokenUsage()).isEqualTo(12);
        assertThat(reply.getModel()).isEqualTo("gpt-5.5");
        server.verify();
    }

    @Test
    void shouldParseChatCompletionsCompatiblePayload() {
        CustomerAiConfig config = config("/v1/chat/completions");

        server.expect(requestTo("https://api.ai-pixel.online/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].content").value("你好"))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"role\":\"assistant\","
                                + "\"content\":\"你好，我是智问学伴。\"}}],"
                                + "\"usage\":{\"total_tokens\":9}}",
                        MediaType.APPLICATION_JSON));

        AiReply reply = client.ask(config, List.of(), "你好");

        assertThat(reply).isNotNull();
        assertThat(reply.getContent()).isEqualTo("你好，我是智问学伴。");
        assertThat(reply.getTokenUsage()).isEqualTo(9);
        server.verify();
    }

    @Test
    void shouldRejectNonPixelHostWithoutSendingRequest() {
        CustomerAiConfig config = config("/v1/responses");
        config.setBaseUrl("https://not-allowed.example");

        AiReply reply = client.ask(config, List.of(), "不会发送");

        assertThat(reply).isNull();
        server.verify();
    }

    @Test
    void shouldDegradeWhenThirdPartyKeyIsMissing() {
        when(redisService.getCacheObject("customer:ai:secret")).thenReturn("");
        CustomerAiConfig config = config("/v1/responses");

        AiReply reply = client.ask(config, List.of(), "没有 Key");

        assertThat(reply).isNull();
        server.verify();
    }

    @Test
    void shouldPreferTransientRequestKeyWithoutReadingOrPersistingIt() {
        when(redisService.getCacheObject("customer:ai:secret")).thenReturn("");
        CustomerAiConfig config = config("/v1/responses");

        server.expect(requestTo("https://api.ai-pixel.online/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer request-only-key"))
                .andExpect(jsonPath("$.model").value("gpt-5.5"))
                .andRespond(withSuccess(
                        "{\"output_text\":\"本次请求使用临时 Key。\"}",
                        MediaType.APPLICATION_JSON));

        AiReply reply = client.ask(config, List.of(), "临时 Key 测试", " Bearer request-only-key ");

        assertThat(reply).isNotNull();
        assertThat(reply.getContent()).isEqualTo("本次请求使用临时 Key。");
        server.verify();
    }

    private CustomerAiConfig config(String endpointPath) {
        CustomerAiConfig config = new CustomerAiConfig();
        config.setProvider("pixel");
        config.setBaseUrl("https://api.ai-pixel.online");
        config.setEndpointPath(endpointPath);
        config.setModel("gpt-5.5");
        config.setEnabled(1);
        config.setMaxRetries(0);
        config.setSystemPrompt(properties.getSystemPrompt());
        return config;
    }
}
