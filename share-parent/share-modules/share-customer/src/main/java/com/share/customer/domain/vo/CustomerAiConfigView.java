package com.share.customer.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/** 脱敏后的第三方 AI 配置。 */
@Data
public class CustomerAiConfigView {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String provider;
    private String baseUrl;
    private String endpointPath;
    private String model;
    private Integer enabled;
    private Integer timeoutMs;
    private Integer maxRetries;
    private String systemPrompt;
    private Boolean apiKeyConfigured;
}
