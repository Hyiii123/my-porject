package com.share.customer.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 浏览器直连第三方 Pixel API 后，将问答结果写回客服会话的请求。 */
@Data
public class AiReplyRecordRequest {
    /** 兼容旧版前端把会话编号放在 JSON 请求体中的调用方式。 */
    private Long sessionId;

    @NotBlank(message = "问题不能为空")
    @Size(max = 2000, message = "问题不能超过2000个字符")
    private String question;

    @NotBlank(message = "AI 回复不能为空")
    @Size(max = 10000, message = "AI 回复不能超过10000个字符")
    private String answer;
}
