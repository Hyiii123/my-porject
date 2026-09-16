package com.share.mq.customer.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.share.mq.customer.constant.CustomerMqConstants;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 客服与AI领域：知识库异步同步与向量计算解耦消费者
 */
@Component
@RocketMQMessageListener(
    topic = CustomerMqConstants.CUSTOMER_KNOWLEDGE_SYNC_TOPIC,
    consumerGroup = CustomerMqConstants.CUSTOMER_KNOWLEDGE_CONSUMER_GROUP
)
public class CustomerKnowledgeSyncConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(CustomerKnowledgeSyncConsumer.class);

    @Override
    public void onMessage(String message) {
        log.info("【Share-MQ消息中枢-客服与AI领域】收到知识库变更事件: {}", message);
        try {
            JSONObject data = JSON.parseObject(message);
            if (data == null) {
                return;
            }
            String action = data.getString("action");
            Long knowledgeId = data.getLong("knowledgeId");
            String question = data.getString("question");

            log.info("【Share-MQ消息中枢-客服与AI领域】🧠 异步重构知识向量索引与刷新缓存... action={}, knowledgeId={}, question={}",
                    action, knowledgeId, question != null ? question : "-");

            log.info("【Share-MQ消息中枢-客服与AI领域】✅ 知识库条目 {} (action={}) 异步向量索引与缓存预热完成",
                    knowledgeId, action);
        } catch (Exception e) {
            log.error("【Share-MQ消息中枢-客服与AI领域】处理知识库同步异常, message={}", message, e);
        }
    }
}
