package com.share.trade.service.support.payment;

import com.share.trade.domain.TrOrder;
import com.share.trade.domain.TrPaymentOrder;

import java.util.Map;

/**
 * 支付渠道策略契约接口 (Payment Channel Strategy Pattern)
 */
public interface PaymentChannelStrategy {

    /**
     * 渠道唯一标识代码（如 wechat, alipay, mock）
     */
    String getChannelCode();

    /**
     * 渠道显示名称（如 微信支付, 支付宝, 模拟支付）
     */
    String getChannelName();

    /**
     * 渠道类型（如 wechat, alipay, mock）
     */
    String getChannelType();

    /**
     * 是否支持指定渠道代码
     */
    boolean supports(String channelCode);

    /**
     * 构建渠道专属的预支付响应数据（如二维码、支付链接、SDK参数）
     */
    Map<String, Object> buildPaymentInfo(TrOrder order, TrPaymentOrder payment);

    /**
     * 渠道展示排序优先级（越小越优先）
     */
    default int getOrder() {
        return 100;
    }

    /**
     * 是否对外公开为常规选择渠道
     */
    default boolean isPublicChannel() {
        return true;
    }
}
