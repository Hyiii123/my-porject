package com.share.customer.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 第三方 AI 配置请求。API Key 只在请求中短暂传输，服务端不会回显。 */
@Data
public class AiConfigRequest {
    @NotBlank(message = "第三方 API 地址不能为空")
    @Size(max = 500, message = "API 地址不能超过500个字符")
    private String baseUrl;

    @NotBlank(message = "接口路径不能为空")
    @Size(max = 200, message = "接口路径不能超过200个字符")
    private String endpointPath;

    @NotBlank(message = "模型名称不能为空")
    @Size(max = 128, message = "模型名称不能超过128个字符")
    private String model;

    private Integer enabled;

    @Min(value = 1000, message = "超时时间不能低于1000毫秒")
    @Max(value = 120000, message = "超时时间不能超过120000毫秒")
    private Integer timeoutMs;

    @Min(value = 0, message = "重试次数不能小于0")
    @Max(value = 3, message = "重试次数不能超过3次")
    private Integer maxRetries;

    @Size(max = 4000, message = "系统提示词不能超过4000个字符")
    private String systemPrompt;

    /** 留空表示保留服务端已有 Key。 */
    @Size(max = 500, message = "API Key 长度异常")
    private String apiKey;
}
