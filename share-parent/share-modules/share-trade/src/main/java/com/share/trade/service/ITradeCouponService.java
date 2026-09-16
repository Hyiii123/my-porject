package com.share.trade.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.share.trade.domain.MktCoupon;
import com.share.trade.domain.MktUserCoupon;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 优惠券与营销领域服务接口
 */
public interface ITradeCouponService {
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
}
