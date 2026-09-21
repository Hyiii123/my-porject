package com.share.trade.service;

import com.share.trade.domain.TrOrder;

import java.util.List;
import java.util.Map;

/**
 * 购物车与订单领域服务接口
 */
public interface ITradeOrderService {
    List<Map<String, Object>> carts();

    Map<String, Object> addCart(Map<String, ?> body);

    void removeCarts(String ids);

    Map<String, Object> listOrders(Map<String, ?> params, boolean admin);

    String createOrderToken();

    Map<String, Object> order(Long id);

    Map<String, Object> prePlaceOrder(Map<String, ?> params);

    Map<String, Object> placeOrder(Map<String, ?> body);

    Map<String, Object> freeCourse(Long courseId);

    Map<String, Object> seckillCourse(Long courseId);

    Map<String, Object> cancelOrder(Long id);

    void deleteOrder(Long id);

    Map<String, Object> pageLegacyOrderDetails(Map<String, ?> params);

    Map<String, Object> legacyOrderDetail(Long id);

    TrOrder findOrder(Long id);

    void updateOrderStatus(Long orderId, Integer status);

    void enrollPurchasedCourses(TrOrder order);

    void revokePurchasedCourses(TrOrder order);

    Map<String, Object> orderView(TrOrder item);

    Map<String, Object> courseSnapshot(Long courseId);
}

