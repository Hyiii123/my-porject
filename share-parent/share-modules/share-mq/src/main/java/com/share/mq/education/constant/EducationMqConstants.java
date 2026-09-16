package com.share.mq.education.constant;

/**
 * 教育领域 RocketMQ 常量
 */
public class EducationMqConstants {
    /** 支付成功开课履约 Topic */
    public static final String TRADE_ORDER_PAID_TOPIC = "TRADE_ORDER_PAID_TOPIC";
    public static final String EDU_ENROLL_CONSUMER_GROUP = "edu-course-enroll-consumer-group";

    /** 退款审批通过逆向撤课 Topic */
    public static final String TRADE_ORDER_REFUNDED_TOPIC = "TRADE_ORDER_REFUNDED_TOPIC";
    public static final String EDU_REFUND_CONSUMER_GROUP = "edu-course-refund-consumer-group";

    /** 学员完课打卡广播 Topic */
    public static final String EDU_COURSE_COMPLETED_TOPIC = "EDU_COURSE_COMPLETED_TOPIC";
    public static final String EDU_COURSE_COMPLETED_CONSUMER_GROUP = "edu-course-completed-consumer-group";
}
