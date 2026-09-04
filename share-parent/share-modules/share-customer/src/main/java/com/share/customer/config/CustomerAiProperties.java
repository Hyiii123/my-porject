package com.share.customer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 第三方 AI 配置。
 *
 * <p>这里明确使用可配置的第三方 Pixel API，不引入或默认指向官方 OpenAI API。</p>
 */
@Data
@ConfigurationProperties(prefix = "customer.ai")
public class CustomerAiProperties {

    private String baseUrl = "https://api.ai-pixel.online";

    private String endpointPath = "/v1/responses";

    private String model = "gpt-5.5";

    /** 服务端环境变量中的 Key；不返回给前端。 */
    private String secret;

    private int timeoutMs = 30000;

    private int maxRetries = 1;

    private String systemPrompt = "你是智问学伴的在线客服，请使用简洁、友好、准确的中文回答学习平台用户的问题；不确定时请明确说明，不要编造订单或账号信息。";
}
