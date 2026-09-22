package com.share.education.mq;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 智能体异步事件总线发布者 (EduAgentEventPublisher)。
 */
@Component
public class EduAgentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EduAgentEventPublisher.class);
    public static final String TOPIC = "EDU_AGENT_EVENT_TOPIC";

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    public void publishEvent(AgentEventMessage message) {
        if (rocketMQTemplate == null) {
            log.debug("[AgentEventPublisher] RocketMQTemplate 未就绪，跳过事件广播");
            return;
        }
        try {
            rocketMQTemplate.asyncSend(TOPIC, MessageBuilder.withPayload(message).build(), new org.apache.rocketmq.client.producer.SendCallback() {
                @Override
                public void onSuccess(org.apache.rocketmq.client.producer.SendResult sendResult) {
                    log.info("[AgentEventPublisher] 成功广播智能体事件: type={}, userId={}, msgId={}",
                            message.getEventType(), message.getUserId(), sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable e) {
                    log.warn("[AgentEventPublisher] 广播智能体事件异常: {}", e.getMessage());
                }
            });
        } catch (Exception ex) {
            log.warn("[AgentEventPublisher] 触发事件发送失败: {}", ex.getMessage());
        }
    }
}
