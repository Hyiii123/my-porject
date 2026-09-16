package com.share.mq.trade.constant;

/**
 * 交易领域 RocketMQ 常量
 */
public class TradeMqConstants {
    /** 交易订单超时未支付自动关单延时 Topic */
    public static final String TRADE_ORDER_TIMEOUT_TOPIC = "TRADE_ORDER_TIMEOUT_TOPIC";
    public static final String ORDER_TIMEOUT_CONSUMER_GROUP = "order-timeout-consumer-group";

    /** 交易订单即将失效前 5 分钟催付提醒 Topic */
    public static final String TRADE_ORDER_EXPIRING_REMINDER_TOPIC = "TRADE_ORDER_EXPIRING_REMINDER_TOPIC";
    public static final String ORDER_EXPIRING_REMINDER_CONSUMER_GROUP = "order-expiring-reminder-consumer-group";

    /** 订单退款审批通过逆向履约 Topic */
    public static final String TRADE_ORDER_REFUNDED_TOPIC = "TRADE_ORDER_REFUNDED_TOPIC";
}
