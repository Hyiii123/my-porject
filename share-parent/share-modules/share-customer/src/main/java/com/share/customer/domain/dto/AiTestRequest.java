package com.share.customer.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** AI 连通性测试请求。 */
@Data
public class AiTestRequest {
    @NotBlank(message = "测试消息不能为空")
    @Size(max = 500, message = "测试消息不能超过500个字符")
    private String message;
}
