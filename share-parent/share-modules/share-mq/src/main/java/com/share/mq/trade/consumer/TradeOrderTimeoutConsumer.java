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

/**
 * 交易领域：订单超时未支付自动关单消费者 (基于 RocketMQ 延时消息，调度远程交易服务)
 */
@Component
@RocketMQMessageListener(
    topic = TradeMqConstants.TRADE_ORDER_TIMEOUT_TOPIC,
    consumerGroup = TradeMqConstants.ORDER_TIMEOUT_CONSUMER_GROUP
)
public class TradeOrderTimeoutConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(TradeOrderTimeoutConsumer.class);

    private final RemoteTradeInternalService tradeInternalService;

    public TradeOrderTimeoutConsumer(RemoteTradeInternalService tradeInternalService) {
        this.tradeInternalService = tradeInternalService;
    }

    @Override
    public void onMessage(String message) {
        log.info("【Share-MQ消息中枢-交易领域】接收到订单超时延时关单消息: {}", message);
        try {
            JSONObject data = JSON.parseObject(message);
            if (data == null) {
                return;
            }
            Long orderId = data.getLong("orderId");
            if (orderId == null) {
                return;
            }

            // 通过 Feign 调用交易服务内部关单接口 (附带 InnerAuth 凭证)
            AjaxResult result = tradeInternalService.cancelOrder(orderId, SecurityConstants.INNER);
            log.info("【Share-MQ消息中枢-交易领域】订单超时关单执行完毕, orderId={}, 响应结果={}", orderId, result);
        } catch (Exception e) {
            log.error("【Share-MQ消息中枢-交易领域】处理订单超时延时关单异常, message={}", message, e);
            throw new RuntimeException("处理订单超时消息异常", e);
        }
    }
}
