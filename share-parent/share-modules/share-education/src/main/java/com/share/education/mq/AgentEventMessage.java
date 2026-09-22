package com.share.education.mq;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 多智能体异步事件总线消息载荷 (Agent Event Message)。
 */
@Data
public class AgentEventMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 事件类型: STUDENT_WEAKNESS (考点薄弱), PORTRAIT_CALIBRATED (画像校准), ROADMAP_REFINED (路线精修) */
    private String eventType;

    /** 学员 ID */
    private Long userId;

    /** 涉及的核心技术概念或学科 */
    private String conceptName;

    /** 扩展属性负载 */
    private Map<String, Object> payload;

    /** 事件产生时间戳 */
    private Long timestamp;

    public AgentEventMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public AgentEventMessage(String eventType, Long userId, String conceptName, Map<String, Object> payload) {
        this.eventType = eventType;
        this.userId = userId;
        this.conceptName = conceptName;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }
}
