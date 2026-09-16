package com.share.education.mq;

/**
 * 教育微服务 RocketMQ 主题与消费组常量
 */
public class EduRocketMQConstants {

    /** 逆向履约：订单退款成功通知 Topic (监听交易服务发出的退款履约消息) */
    public static final String TRADE_ORDER_REFUNDED_TOPIC = "TRADE_ORDER_REFUNDED_TOPIC";

    /** 逆向履约：教育微服务退款撤课消费者组 */
    public static final String EDU_REFUND_CONSUMER_GROUP = "edu-course-refund-consumer-group";

    /** 学情广播：学员完课打卡广播 Topic (学情 100% 达成，广播发放积分与勋章) */
    public static final String EDU_COURSE_COMPLETED_TOPIC = "EDU_COURSE_COMPLETED_TOPIC";

    /** 学情广播：学员完课打卡消费者组 */
    public static final String EDU_COURSE_COMPLETED_CONSUMER_GROUP = "edu-course-completed-consumer-group";
}
