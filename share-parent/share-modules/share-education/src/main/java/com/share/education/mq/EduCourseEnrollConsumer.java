package com.share.education.mq;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.share.education.service.IEduLearningService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 课程支付履约消费者 (监听 RocketMQ 支付成功消息，异步开通课程学习权限与学情记录)
 */
@Component
@RocketMQMessageListener(
    topic = "TRADE_ORDER_PAID_TOPIC",
    consumerGroup = "edu-course-enroll-consumer-group"
)
public class EduCourseEnrollConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(EduCourseEnrollConsumer.class);

    private final IEduLearningService learningService;

    public EduCourseEnrollConsumer(IEduLearningService learningService) {
        this.learningService = learningService;
    }

    @Override
    public void onMessage(String message) {
        log.info("【RocketMQ课程履约】收到支付成功开课消息: {}", message);
        try {
            JSONObject data = JSON.parseObject(message);
            if (data == null) {
                return;
            }
            Long userId = data.getLong("userId");
            Long orderId = data.getLong("orderId");
            JSONArray courses = data.getJSONArray("courseIds");
            if (userId == null || courses == null || courses.isEmpty()) {
                log.warn("【RocketMQ课程履约】消息参数不全, message={}", message);
                return;
            }

            List<Long> courseIds = new ArrayList<>();
            for (int i = 0; i < courses.size(); i++) {
                Long cid = courses.getLong(i);
                if (cid != null) {
                    courseIds.add(cid);
                }
            }

            for (Long courseId : courseIds) {
                learningService.enrollCourseForUser(userId, courseId);
                log.info("【RocketMQ课程履约】成功为用户 {} 开通课程 {} (来自订单 {})", userId, courseId, orderId);
            }
            log.info("【RocketMQ课程履约】订单 {} 全部课程履约开通完毕, 用户: {}, 课程数: {}", orderId, userId, courseIds.size());
        } catch (Exception e) {
            log.error("【RocketMQ课程履约】开课处理异常, message={}", message, e);
            throw new RuntimeException("处理课程履约异常", e);
        }
    }
}
