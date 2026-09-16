package com.share.customer.mq;

/**
 * AI 客服 RocketMQ 主题与消费组常量
 */
public class CustomerRocketMQConstants {

    /** 知识库/FAQ 异步同步与向量计算 Topic */
    public static final String CUSTOMER_KNOWLEDGE_SYNC_TOPIC = "CUSTOMER_KNOWLEDGE_SYNC_TOPIC";

    /** 知识库异步同步消费者组 */
    public static final String CUSTOMER_KNOWLEDGE_CONSUMER_GROUP = "customer-knowledge-sync-consumer-group";
}
