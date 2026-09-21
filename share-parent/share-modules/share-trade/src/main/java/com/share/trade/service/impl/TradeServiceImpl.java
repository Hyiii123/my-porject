package com.share.trade.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.share.trade.domain.*;
import com.share.trade.service.ITradeCouponService;
import com.share.trade.service.ITradeOrderService;
import com.share.trade.service.ITradePaymentService;
import com.share.trade.service.ITradeService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 交易领域聚合服务实现类（向后兼容门面实现）
 */
@Service
public class TradeServiceImpl implements ITradeService {

    private final ITradeCouponService couponService;
    private final ITradeOrderService orderService;
    private final ITradePaymentService paymentService;

    public TradeServiceImpl(ITradeCouponService couponService,
                            ITradeOrderService orderService,
                            ITradePaymentService paymentService) {
        this.couponService = couponService;
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    // --- Coupon Domain ---
    @Override
    public List<Map<String, Object>> collectableCoupons() {
        return couponService.collectableCoupons();
    }

    @Override
    public Map<String, Object> coupon(Long id) {
        return couponService.coupon(id);
    }

    @Override
    public IPage<MktCoupon> pageCoupons(String keyword, Integer status, long pageNo, long pageSize) {
        return couponService.pageCoupons(keyword, status, pageNo, pageSize);
    }

    @Override
    public Map<String, Object> pageLegacyCoupons(Map<String, ?> params) {
        return couponService.pageLegacyCoupons(params);
    }

    @Override
    public Map<String, Object> saveLegacyCoupon(Map<String, ?> body) {
        return couponService.saveLegacyCoupon(body);
    }

    @Override
    public void deleteCoupon(Long id) {
        couponService.deleteCoupon(id);
    }

    @Override
    public Map<String, Object> setCouponStatus(Long id, int status) {
        return couponService.setCouponStatus(id, status);
    }

    @Override
    public Map<String, Object> pageCouponCodes(Map<String, ?> params) {
        return couponService.pageCouponCodes(params);
    }

    @Override
    public MktCoupon saveCoupon(MktCoupon value) {
        return couponService.saveCoupon(value);
    }

    @Override
    public Map<String, Object> userCoupons(Map<String, ?> params) {
        return couponService.userCoupons(params);
    }

    @Override
    public Map<String, Object> receiveCoupon(Long couponId) {
        return couponService.receiveCoupon(couponId);
    }

    @Override
    public Map<String, Object> exchangeCoupon(String code) {
        return couponService.exchangeCoupon(code);
    }

    @Override
    public MktCoupon getCouponById(Long id) {
        return couponService.getCouponById(id);
    }

    @Override
    public MktUserCoupon getAvailableUserCoupon(Long userId, Long couponParam) {
        return couponService.getAvailableUserCoupon(userId, couponParam);
    }

    @Override
    public BigDecimal discount(BigDecimal total, MktCoupon coupon) {
        return couponService.discount(total, coupon);
    }

    @Override
    public void restoreUserCoupon(Long userCouponId) {
        couponService.restoreUserCoupon(userCouponId);
    }

    @Override
    public void markUserCouponUsed(Long userCouponId, Long orderId) {
        couponService.markUserCouponUsed(userCouponId, orderId);
    }

    @Override
    public Map<String, Object> couponView(MktCoupon item) {
        return couponService.couponView(item);
    }

    @Override
    public Map<String, Object> userCouponView(MktUserCoupon item) {
        return couponService.userCouponView(item);
    }

    // --- Order & Cart Domain ---
    @Override
    public List<Map<String, Object>> carts() {
        return orderService.carts();
    }

    @Override
    public Map<String, Object> addCart(Map<String, ?> body) {
        return orderService.addCart(body);
    }

    @Override
    public void removeCarts(String ids) {
        orderService.removeCarts(ids);
    }

    @Override
    public Map<String, Object> listOrders(Map<String, ?> params, boolean admin) {
        return orderService.listOrders(params, admin);
    }

    @Override
    public String createOrderToken() {
        return orderService.createOrderToken();
    }

    @Override
    public Map<String, Object> order(Long id) {
        return orderService.order(id);
    }

    @Override
    public Map<String, Object> prePlaceOrder(Map<String, ?> params) {
        return orderService.prePlaceOrder(params);
    }

    @Override
    public Map<String, Object> placeOrder(Map<String, ?> body) {
        return orderService.placeOrder(body);
    }

    @Override
    public Map<String, Object> freeCourse(Long courseId) {
        return orderService.freeCourse(courseId);
    }

    @Override
    public Map<String, Object> seckillCourse(Long courseId) {
        return orderService.seckillCourse(courseId);
    }

    @Override
    public Map<String, Object> cancelOrder(Long id) {
        return orderService.cancelOrder(id);
    }

    @Override
    public void deleteOrder(Long id) {
        orderService.deleteOrder(id);
    }

    @Override
    public Map<String, Object> pageLegacyOrderDetails(Map<String, ?> params) {
        return orderService.pageLegacyOrderDetails(params);
    }

    @Override
    public Map<String, Object> legacyOrderDetail(Long id) {
        return orderService.legacyOrderDetail(id);
    }

    @Override
    public TrOrder findOrder(Long id) {
        return orderService.findOrder(id);
    }

    @Override
    public void updateOrderStatus(Long orderId, Integer status) {
        orderService.updateOrderStatus(orderId, status);
    }

    @Override
    public void enrollPurchasedCourses(TrOrder order) {
        orderService.enrollPurchasedCourses(order);
    }

    @Override
    public void revokePurchasedCourses(TrOrder order) {
        orderService.revokePurchasedCourses(order);
    }

    @Override
    public Map<String, Object> orderView(TrOrder item) {
        return orderService.orderView(item);
    }

    @Override
    public Map<String, Object> courseSnapshot(Long courseId) {
        return orderService.courseSnapshot(courseId);
    }

    // --- Payment & Refund Domain ---
    @Override
    public List<Map<String, Object>> paymentChannels() {
        return paymentService.paymentChannels();
    }

    @Override
    public Map<String, Object> createPayment(Map<String, ?> body) {
        return paymentService.createPayment(body);
    }

    @Override
    public Map<String, Object> simulatePayment(Long orderId) {
        return paymentService.simulatePayment(orderId);
    }

    @Override
    public Map<String, Object> paymentState(Long orderId) {
        return paymentService.paymentState(orderId);
    }

    @Override
    public Map<String, Object> applyRefund(Map<String, ?> body) {
        return paymentService.applyRefund(body);
    }

    @Override
    public Map<String, Object> refund(Long id) {
        return paymentService.refund(id);
    }

    @Override
    public IPage<TrRefundApply> pageRefunds(Integer status, long pageNo, long pageSize) {
        return paymentService.pageRefunds(status, pageNo, pageSize);
    }

    @Override
    public Map<String, Object> pageLegacyRefunds(Map<String, ?> params) {
        return paymentService.pageLegacyRefunds(params);
    }

    @Override
    public Map<String, Object> approveRefund(Map<String, ?> body) {
        return paymentService.approveRefund(body);
    }

    @Override
    public Map<String, Object> nextRefund() {
        return paymentService.nextRefund();
    }

    @Override
    public Map<String, Object> legacyRefundViewForApi(Long id) {
        return paymentService.legacyRefundViewForApi(id);
    }

    @Override
    public Map<String, Object> statistics() {
        return paymentService.statistics();
    }
}
