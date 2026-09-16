package com.share.trade.service.support.coupon;

import com.share.trade.domain.MktCoupon;

import java.math.BigDecimal;

/**
 * 优惠券折扣计算策略契约 (Coupon Discount Strategy Pattern)
 */
public interface CouponDiscountStrategy {

    /**
     * 是否支持指定折扣类型
     *
     * @param discountType 优惠券折扣类型（2: 折扣率/打折, 3/4: 固定立减/现金券）
     */
    boolean supports(Integer discountType);

    /**
     * 计算订单优惠减免金额
     *
     * @param total 订单应付总额
     * @param coupon 优惠券实体
     * @return 实际抵扣减免金额
     */
    BigDecimal calculateDiscount(BigDecimal total, MktCoupon coupon);

    /**
     * 策略优先级
     */
    default int getOrder() {
        return 100;
    }
}
