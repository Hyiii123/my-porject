package com.share.education.mq;

import com.share.education.ai.memory.AgentMemoryService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 智能体异步事件消费者 (EduAgentEventListener)。
 * 接收全系统产生的学情、薄弱点与行为事件，自动存入伴学长期记忆，形成闭环反馈。
 */
@Component
@RocketMQMessageListener(
        topic = EduAgentEventPublisher.TOPIC,
        consumerGroup = "edu-agent-event-consumer-group"
)
public class EduAgentEventListener implements RocketMQListener<AgentEventMessage> {

    private static final Logger log = LoggerFactory.getLogger(EduAgentEventListener.class);

    @Autowired(required = false)
    private AgentMemoryService memoryService;

    @Override
    public void onMessage(AgentEventMessage message) {
        if (message == null || message.getUserId() == null) {
            return;
        }
        log.info("[AgentEventListener] 收到跨服务智能体事件: type={}, userId={}, concept={}",
                message.getEventType(), message.getUserId(), message.getConceptName());

        if (memoryService != null && "STUDENT_WEAKNESS".equalsIgnoreCase(message.getEventType())) {
            memoryService.recordEpisode(
                    message.getUserId(),
                    message.getConceptName(),
                    "系统捕获考点阻滞与薄弱项",
                    "自动建议加入前驱基石微课补齐计划",
                    60
            );
            log.info("[AgentEventListener] 已成功将学员考点薄弱事件落盘为伴学长期情景记忆");
        }
    }
}
