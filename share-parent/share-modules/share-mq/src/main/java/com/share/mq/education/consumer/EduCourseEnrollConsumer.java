package com.share.mq.education.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.share.common.core.constant.SecurityConstants;
import com.share.mq.education.constant.EducationMqConstants;
import com.share.mq.education.feign.RemoteEducationInternalService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 教育领域：课程支付履约消费者 (监听支付成功消息，调度教育服务为用户开课)
 */
@Component
@RocketMQMessageListener(
    topic = EducationMqConstants.TRADE_ORDER_PAID_TOPIC,
    consumerGroup = EducationMqConstants.EDU_ENROLL_CONSUMER_GROUP
)
public class EduCourseEnrollConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(EduCourseEnrollConsumer.class);

    private final RemoteEducationInternalService educationInternalService;

    public EduCourseEnrollConsumer(RemoteEducationInternalService educationInternalService) {
        this.educationInternalService = educationInternalService;
    }

    @Override
    public void onMessage(String message) {
        log.info("【Share-MQ消息中枢-教育领域】收到支付成功开课履约消息: {}", message);
        try {
            JSONObject data = JSON.parseObject(message);
            if (data == null) {
                return;
            }
            Long userId = data.getLong("userId");
            Long orderId = data.getLong("orderId");
            JSONArray courses = data.getJSONArray("courseIds");
            if (userId == null || courses == null || courses.isEmpty()) {
                log.warn("【Share-MQ消息中枢-教育领域】开课消息参数不全, message={}", message);
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
                educationInternalService.enrollCourseForUser(courseId, userId, SecurityConstants.INNER);
                log.info("【Share-MQ消息中枢-教育领域】成功调用教育微服务为用户 {} 开通课程 {} (来自订单 {})", userId, courseId, orderId);
            }
            log.info("【Share-MQ消息中枢-教育领域】订单 {} 全部课程履约开通完毕, 用户: {}, 课程数: {}", orderId, userId, courseIds.size());
        } catch (Exception e) {
            log.error("【Share-MQ消息中枢-教育领域】开课履约处理异常, message={}", message, e);
            throw new RuntimeException("处理课程履约异常", e);
        }
    }
}
