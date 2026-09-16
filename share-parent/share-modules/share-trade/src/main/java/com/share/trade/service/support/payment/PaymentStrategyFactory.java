package com.share.trade.service.support.payment;

import com.share.trade.domain.TrOrder;
import com.share.trade.domain.TrPaymentOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 支付渠道策略工厂 (Payment Channel Strategy Factory)
 *
 * <p>基于策略工厂模式统一管理各支付渠道，负责动态路由分发与支付信息构建。</p>
 */
@Slf4j
@Component
public class PaymentStrategyFactory {

    private final List<PaymentChannelStrategy> strategies;
    private final PaymentChannelStrategy defaultStrategy;

    public PaymentStrategyFactory(List<PaymentChannelStrategy> strategies) {
        if (strategies == null || strategies.isEmpty()) {
            this.strategies = Collections.emptyList();
            this.defaultStrategy = null;
            log.warn("【PaymentStrategyFactory】未发现任何支付渠道策略实现！");
        } else {
            this.strategies = strategies.stream()
                    .sorted(Comparator.comparingInt(PaymentChannelStrategy::getOrder))
                    .collect(Collectors.toList());
            this.defaultStrategy = this.strategies.get(0);
            log.info("【PaymentStrategyFactory】成功加载支付渠道策略，共 {} 个: {}",
                    this.strategies.size(),
                    this.strategies.stream().map(s -> s.getChannelCode() + "(" + s.getChannelName() + ")").toList());
        }
    }

    /**
     * 获取对外公开的所有有效支付渠道列表
     */
    public List<Map<String, Object>> getAvailableChannels() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (PaymentChannelStrategy strategy : strategies) {
            if (strategy.isPublicChannel()) {
                Map<String, Object> channel = new LinkedHashMap<>();
                channel.put("id", strategy.getChannelCode());
                channel.put("name", strategy.getChannelName());
                channel.put("type", strategy.getChannelType());
                list.add(channel);
            }
        }
        return list;
    }

    /**
     * 根据渠道编码获取匹配的支付策略
     */
    public PaymentChannelStrategy getStrategy(String channelCode) {
        if (channelCode != null && !channelCode.isBlank()) {
            for (PaymentChannelStrategy strategy : strategies) {
                if (strategy.supports(channelCode)) {
                    return strategy;
                }
            }
        }
        log.warn("【PaymentStrategyFactory】未找到渠道代码 [{}] 对应的支付策略，回退至默认策略: {}",
                channelCode, defaultStrategy != null ? defaultStrategy.getChannelCode() : "null");
        return defaultStrategy;
    }

    /**
     * 构建渠道专属支付响应数据
     */
    public Map<String, Object> buildPayment(TrOrder order, TrPaymentOrder payment, String channelCode) {
        PaymentChannelStrategy strategy = getStrategy(channelCode);
        if (strategy == null) {
            throw new IllegalStateException("无可用的支付渠道策略");
        }
        return strategy.buildPaymentInfo(order, payment);
    }
}
