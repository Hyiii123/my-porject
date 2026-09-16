package com.share.trade.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.share.trade.domain.TrRefundApply;

import java.util.List;
import java.util.Map;

/**
 * 支付与退款领域服务接口
 */
public interface ITradePaymentService {
    List<Map<String, Object>> paymentChannels();

    Map<String, Object> createPayment(Map<String, ?> body);

    Map<String, Object> simulatePayment(Long orderId);

    Map<String, Object> paymentState(Long orderId);

    Map<String, Object> applyRefund(Map<String, ?> body);

    Map<String, Object> refund(Long id);

    IPage<TrRefundApply> pageRefunds(Integer status, long pageNo, long pageSize);

    Map<String, Object> pageLegacyRefunds(Map<String, ?> params);

    Map<String, Object> approveRefund(Map<String, ?> body);

    Map<String, Object> nextRefund();

    Map<String, Object> legacyRefundViewForApi(Long id);

    Map<String, Object> statistics();
}
