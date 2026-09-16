package com.share.customer.service.support.security;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 客服安全检查上下文 (Security Check Context)
 */
@Data
@Builder
public class SecurityCheckContext {

    private Long userId;
    private String content;
    private String lowerContent;

    @Builder.Default
    private boolean blocked = false;

    private String blockedMessage;

    @Builder.Default
    private boolean crossUserAttempt = false;

    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    public void block(String message) {
        this.blocked = true;
        this.blockedMessage = message;
    }
}
