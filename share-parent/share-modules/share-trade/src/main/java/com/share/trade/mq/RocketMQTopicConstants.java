package com.share.trade.mq;

/**
 * RocketMQ 交易消息主题与消费组常量
 */
public class RocketMQTopicConstants {

    /** 订单超时自动关单 Topic (延时消息) */
    public static final String TRADE_ORDER_TIMEOUT_TOPIC = "TRADE_ORDER_TIMEOUT_TOPIC";

    /** 订单支付成功课程履约 Topic (可靠履约消息) */
    public static final String TRADE_ORDER_PAID_TOPIC = "TRADE_ORDER_PAID_TOPIC";

    /** 订单超时关单消费组 */
    public static final String ORDER_TIMEOUT_CONSUMER_GROUP = "trade-order-timeout-consumer-group";

    /** 课程支付履约消费组 */
    public static final String EDU_ENROLL_CONSUMER_GROUP = "edu-course-enroll-consumer-group";
}
