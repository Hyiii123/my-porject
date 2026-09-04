package com.share.customer.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 创建客服会话请求。 */
@Data
public class CreateSessionRequest {
    @Size(max = 128, message = "用户昵称不能超过128个字符")
    private String userName;
}
