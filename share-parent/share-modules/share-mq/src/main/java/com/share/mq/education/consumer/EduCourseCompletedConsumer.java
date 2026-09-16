package com.share.mq.education.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.share.mq.education.constant.EducationMqConstants;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 教育领域：学员完课打卡异步广播消费者 (异步结算学分奖励、勋章发放与成就记录)
 */
@Component
@RocketMQMessageListener(
    topic = EducationMqConstants.EDU_COURSE_COMPLETED_TOPIC,
    consumerGroup = EducationMqConstants.EDU_COURSE_COMPLETED_CONSUMER_GROUP
)
public class EduCourseCompletedConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(EduCourseCompletedConsumer.class);

    @Override
    public void onMessage(String message) {
        log.info("【Share-MQ消息中枢-教育领域】收到学员完课打卡广播: {}", message);
        try {
            JSONObject data = JSON.parseObject(message);
            if (data == null) {
                return;
            }
            Long userId = data.getLong("userId");
            Long courseId = data.getLong("courseId");
            Integer rewardPoints = data.getInteger("rewardPoints");
            String badge = data.getString("badge");

            if (userId == null || courseId == null) {
                log.warn("【Share-MQ消息中枢-教育领域】完课消息参数不全, message={}", message);
                return;
            }

            // 异步结算学分与发放勋章成就
            log.info("【Share-MQ消息中枢-教育领域】🏆 恭喜用户 {} 顺利完成课程 {}！已结算并发放奖励学分: {}, 授予专属成就徽章: [{}]",
                    userId, courseId, rewardPoints != null ? rewardPoints : 50, badge != null ? badge : "COURSE_MASTER");
        } catch (Exception e) {
            log.error("【Share-MQ消息中枢-教育领域】处理完课打卡广播异常, message={}", message, e);
        }
    }
}
