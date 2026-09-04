package com.share.customer.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 发送客服消息请求。 */
@Data
public class SendMessageRequest {
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000个字符")
    private String content;

    /**
     * 用户在当前浏览器输入的第三方 Pixel Key。
     *
     * <p>该字段只用于当前请求，不会写入 MySQL、Redis，也不会出现在任何响应中。
     * 这样由客服服务端代理第三方调用，避免浏览器直连时被 CORS 拦截。</p>
     */
    @Size(max = 512, message = "AI API Key 长度不正确")
    private String apiKey;

    /** 用户选择的第三方模型；为空时使用服务端配置。 */
    @Size(max = 100, message = "AI 模型名称过长")
    private String model;
}
