package com.share.customer.service;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 第三方 AI 返回的最小结果。 */
@Data
@AllArgsConstructor
public class AiReply {
    private String content;
    private Integer tokenUsage;
    private String model;
}
