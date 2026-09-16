package com.share.trade.mq;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.trade.domain.MktUserCoupon;
import com.share.trade.domain.TrOrder;
import com.share.trade.mapper.MktUserCouponMapper;
import com.share.trade.mapper.TrOrderMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时未支付自动关单消费者 (基于 RocketMQ 延时消息)
 */
@Component
@RocketMQMessageListener(
    topic = RocketMQTopicConstants.TRADE_ORDER_TIMEOUT_TOPIC,
    consumerGroup = RocketMQTopicConstants.ORDER_TIMEOUT_CONSUMER_GROUP
)
public class TradeOrderTimeoutConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(TradeOrderTimeoutConsumer.class);

    private final TrOrderMapper orderMapper;
    private final MktUserCouponMapper userCouponMapper;

    public TradeOrderTimeoutConsumer(TrOrderMapper orderMapper, MktUserCouponMapper userCouponMapper) {
        this.orderMapper = orderMapper;
        this.userCouponMapper = userCouponMapper;
    }

    @Override
    public void onMessage(String message) {
        log.info("【RocketMQ延时关单】接收到订单超时延时消息: {}", message);
        try {
            JSONObject data = JSON.parseObject(message);
            if (data == null) {
                return;
            }
            Long orderId = data.getLong("orderId");
            if (orderId == null) {
                return;
            }

            TrOrder order = orderMapper.selectById(orderId);
            if (order == null || (order.getDelFlag() != null && order.getDelFlag() == 1)) {
                log.warn("【RocketMQ延时关单】订单不存在或已删除, orderId={}", orderId);
                return;
            }

            // 严格仅针对待支付状态 (0) 进行超时自动关闭，保证绝对幂等性
            if (order.getOrderStatus() != null && order.getOrderStatus() == 0) {
                LocalDateTime now = LocalDateTime.now();
                order.setOrderStatus(4); // 4 表示已关闭
                order.setUpdateTime(now);
                orderMapper.updateById(order);

                // 释放并退还用户已锁定的优惠券
                restoreOrderCoupon(order.getId(), now);
                log.info("【RocketMQ延时关单】订单超时关单成功并释放优惠券, orderId={}", orderId);
            } else {
                log.info("【RocketMQ延时关单】订单非待支付状态(status={}), 无需执行关单, orderId={}", order.getOrderStatus(), orderId);
            }
        } catch (Exception e) {
            log.error("【RocketMQ延时关单】处理订单超时消息异常, message={}", message, e);
            throw new RuntimeException("处理订单超时消息异常", e);
        }
    }

    private void restoreOrderCoupon(Long orderId, LocalDateTime now) {
        if (orderId == null) {
            return;
        }
        List<MktUserCoupon> usedCoupons = userCouponMapper.selectList(new LambdaQueryWrapper<MktUserCoupon>()
                .eq(MktUserCoupon::getUsedOrderId, orderId));
        for (MktUserCoupon uc : usedCoupons) {
            if (uc.getStatus() != null && uc.getStatus() == 1) {
                if ("direct_order".equals(uc.getSourceType())) {
                    uc.setStatus(2);
                } else {
                    int restoredStatus = (uc.getExpireAt() != null && now.isAfter(uc.getExpireAt())) ? 2 : 0;
                    uc.setStatus(restoredStatus);
                }
                uc.setUsedAt(null);
                uc.setUsedOrderId(null);
                uc.setUpdateTime(now);
                userCouponMapper.updateById(uc);
            }
        }
    }
}
