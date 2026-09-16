package com.share.trade.service.support.payment.strategy;

import com.share.trade.domain.TrOrder;
import com.share.trade.domain.TrPaymentOrder;
import com.share.trade.service.support.payment.PaymentChannelStrategy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.share.trade.service.support.TradeUtils.cents;

/**
 * 微信支付渠道策略 (WeChat Pay Channel Strategy)
 */
@Component
public class WechatPayChannelStrategy implements PaymentChannelStrategy {

    public static final String CHANNEL_CODE = "wechat";

    @Override
    public String getChannelCode() {
        return CHANNEL_CODE;
    }

    @Override
    public String getChannelName() {
        return "微信支付";
    }

    @Override
    public String getChannelType() {
        return CHANNEL_CODE;
    }

    @Override
    public boolean supports(String channelCode) {
        if (channelCode == null) return false;
        String lower = channelCode.trim().toLowerCase();
        return CHANNEL_CODE.equals(lower) || "wxpay".equals(lower) || "weixin".equals(lower);
    }

    @Override
    public Map<String, Object> buildPaymentInfo(TrOrder order, TrPaymentOrder payment) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentNo", payment.getPaymentNo());
        result.put("orderId", order.getId());
        result.put("amount", cents(order.getPayableAmount()));
        result.put("payUrl", "weixin://wxpay/bizpayurl?pr=" + payment.getPaymentNo());
        result.put("codeUrl", "weixin://wxpay/bizpayurl?pr=" + payment.getPaymentNo());
        result.put("channel", CHANNEL_CODE);
        result.put("status", payment.getStatus());
        result.put("demo", true);
        return result;
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
