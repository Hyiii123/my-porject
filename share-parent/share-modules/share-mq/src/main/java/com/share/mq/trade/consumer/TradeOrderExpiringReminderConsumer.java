package com.share.mq.trade.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.web.domain.AjaxResult;
import com.share.mq.trade.constant.TradeMqConstants;
import com.share.mq.trade.feign.RemoteTradeInternalService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 交易领域：订单即将超时催付提醒消费者 (提前 5 分钟延时预警)
 */
@Component
@RocketMQMessageListener(
    topic = TradeMqConstants.TRADE_ORDER_EXPIRING_REMINDER_TOPIC,
    consumerGroup = TradeMqConstants.ORDER_EXPIRING_REMINDER_CONSUMER_GROUP
)
public class TradeOrderExpiringReminderConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(TradeOrderExpiringReminderConsumer.class);

    private final RemoteTradeInternalService tradeInternalService;

    public TradeOrderExpiringReminderConsumer(RemoteTradeInternalService tradeInternalService) {
        this.tradeInternalService = tradeInternalService;
    }

    @Override
    public void onMessage(String message) {
        log.info("【Share-MQ消息中枢-交易领域】接收到订单即将超时催付预警消息: {}", message);
        try {
            JSONObject data = JSON.parseObject(message);
            if (data == null) {
                return;
            }
            Long orderId = data.getLong("orderId");
            if (orderId == null) {
                return;
            }

            AjaxResult res = tradeInternalService.getOrder(orderId, SecurityConstants.INNER);
            if (res == null || !res.isSuccess() || res.get("data") == null) {
                log.warn("【Share-MQ消息中枢-交易领域】未能查询到订单信息或已删除, orderId={}", orderId);
                return;
            }

            Map<?, ?> order = (Map<?, ?>) res.get("data");
            Object statusObj = order.get("orderStatus");
            Integer status = statusObj instanceof Number ? ((Number) statusObj).intValue() : null;

            if (status != null && status == 0) {
                log.info("【Share-MQ消息中枢-交易领域】🔔 订单即将失效预警！订单号: {}, 用户: {}, 请尽快完成支付！",
                        order.get("orderNo"), order.get("userId"));
            } else {
                log.info("【Share-MQ消息中枢-交易领域】订单已非待支付状态 (status={}), 无需发送催付提醒, orderId={}", status, orderId);
            }
        } catch (Exception e) {
            log.error("【Share-MQ消息中枢-交易领域】处理催付提醒异常, message={}", message, e);
        }
    }
}
