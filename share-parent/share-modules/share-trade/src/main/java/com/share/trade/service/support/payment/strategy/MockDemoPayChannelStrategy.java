package com.share.trade.service.support.payment.strategy;

import com.share.trade.domain.TrOrder;
import com.share.trade.domain.TrPaymentOrder;
import com.share.trade.service.support.payment.PaymentChannelStrategy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.share.trade.service.support.TradeUtils.cents;

/**
 * 平台沙箱模拟支付渠道策略 (Mock/Sandbox Payment Channel Strategy)
 */
@Component
public class MockDemoPayChannelStrategy implements PaymentChannelStrategy {

    public static final String CHANNEL_CODE = "mock";

    @Override
    public String getChannelCode() {
        return CHANNEL_CODE;
    }

    @Override
    public String getChannelName() {
        return "模拟支付";
    }

    @Override
    public String getChannelType() {
        return CHANNEL_CODE;
    }

    @Override
    public boolean supports(String channelCode) {
        if (channelCode == null) return false;
        String lower = channelCode.trim().toLowerCase();
        return CHANNEL_CODE.equals(lower) || "demo".equals(lower) || "test".equals(lower);
    }

    @Override
    public Map<String, Object> buildPaymentInfo(TrOrder order, TrPaymentOrder payment) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentNo", payment.getPaymentNo());
        result.put("orderId", order.getId());
        result.put("amount", cents(order.getPayableAmount()));
        result.put("payUrl", "demo://zhiwen-pay/" + payment.getPaymentNo());
        result.put("channel", CHANNEL_CODE);
        result.put("status", payment.getStatus());
        result.put("demo", true);
        return result;
    }

    @Override
    public int getOrder() {
        return 30;
    }

    @Override
    public boolean isPublicChannel() {
        // 沙箱模拟渠道为开发测试用途，不在公共客户端渠道列表中展示
        return false;
    }
}
