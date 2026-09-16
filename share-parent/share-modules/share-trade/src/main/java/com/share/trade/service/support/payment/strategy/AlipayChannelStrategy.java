package com.share.trade.service.support.payment.strategy;

import com.share.trade.domain.TrOrder;
import com.share.trade.domain.TrPaymentOrder;
import com.share.trade.service.support.payment.PaymentChannelStrategy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.share.trade.service.support.TradeUtils.cents;

/**
 * 支付宝支付渠道策略 (Alipay Channel Strategy)
 */
@Component
public class AlipayChannelStrategy implements PaymentChannelStrategy {

    public static final String CHANNEL_CODE = "alipay";

    @Override
    public String getChannelCode() {
        return CHANNEL_CODE;
    }

    @Override
    public String getChannelName() {
        return "支付宝";
    }

    @Override
    public String getChannelType() {
        return CHANNEL_CODE;
    }

    @Override
    public boolean supports(String channelCode) {
        if (channelCode == null) return false;
        String lower = channelCode.trim().toLowerCase();
        return CHANNEL_CODE.equals(lower) || "ali".equals(lower) || "zhifubao".equals(lower);
    }

    @Override
    public Map<String, Object> buildPaymentInfo(TrOrder order, TrPaymentOrder payment) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentNo", payment.getPaymentNo());
        result.put("orderId", order.getId());
        result.put("amount", cents(order.getPayableAmount()));
        result.put("payUrl", "https://openapi.alipay.com/gateway.do?out_trade_no=" + payment.getPaymentNo());
        result.put("qrCode", "https://qr.alipay.com/" + payment.getPaymentNo());
        result.put("channel", CHANNEL_CODE);
        result.put("status", payment.getStatus());
        result.put("demo", true);
        return result;
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
