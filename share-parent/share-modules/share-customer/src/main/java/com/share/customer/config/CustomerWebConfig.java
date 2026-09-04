package com.share.customer.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** 客服服务 HTTP 客户端配置。 */
@Configuration
public class CustomerWebConfig {

    @Bean
    public RestTemplate customerRestTemplate(RestTemplateBuilder builder, CustomerAiProperties properties) {
        int timeout = Math.max(properties.getTimeoutMs(), 1000);
        return builder
                .setConnectTimeout(Duration.ofMillis(timeout))
                .setReadTimeout(Duration.ofMillis(timeout))
                .build();
    }
}
