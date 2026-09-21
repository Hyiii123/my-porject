package com.share.trade.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.share.trade.domain.MktCoupon;
import com.share.trade.domain.MktUserCoupon;
import com.share.trade.domain.TrOrder;
import com.share.trade.domain.TrRefundApply;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 交易业务聚合总服务接口（1:1 对应 TradeServiceImpl）
 */
public interface ITradeService {

    // --- 优惠券与营销领域 ---
    List<Map<String, Object>> collectableCoupons();
    Map<String, Object> coupon(Long id);
    IPage<MktCoupon> pageCoupons(String keyword, Integer status, long pageNo, long pageSize);
    Map<String, Object> pageLegacyCoupons(Map<String, ?> params);
    Map<String, Object> saveLegacyCoupon(Map<String, ?> body);
    void deleteCoupon(Long id);
    Map<String, Object> setCouponStatus(Long id, int status);
    Map<String, Object> pageCouponCodes(Map<String, ?> params);
    MktCoupon saveCoupon(MktCoupon value);
    Map<String, Object> userCoupons(Map<String, ?> params);
    Map<String, Object> receiveCoupon(Long couponId);
    Map<String, Object> exchangeCoupon(String code);
    MktCoupon getCouponById(Long id);
    MktUserCoupon getAvailableUserCoupon(Long userId, Long couponParam);
    BigDecimal discount(BigDecimal total, MktCoupon coupon);
    void restoreUserCoupon(Long userCouponId);
    void markUserCouponUsed(Long userCouponId, Long orderId);
    Map<String, Object> couponView(MktCoupon item);
    Map<String, Object> userCouponView(MktUserCoupon item);

    // --- 购物车与订单领域 ---
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

    // --- 支付与退款领域 ---
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

