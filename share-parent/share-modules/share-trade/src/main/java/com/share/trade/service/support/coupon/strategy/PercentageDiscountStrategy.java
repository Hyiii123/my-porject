package com.share.trade.service.support.coupon.strategy;

import com.share.trade.domain.MktCoupon;
import com.share.trade.service.support.coupon.CouponDiscountStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.share.trade.service.support.TradeUtils.defaultValue;

/**
 * 百分比打折折扣策略 (Percentage Discount Strategy, discountType=2)
 */
@Component
public class PercentageDiscountStrategy implements CouponDiscountStrategy {

    public static final int DISCOUNT_TYPE = 2;

    @Override
    public boolean supports(Integer discountType) {
        return discountType != null && discountType == DISCOUNT_TYPE;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal total, MktCoupon coupon) {
        BigDecimal value = defaultValue(coupon.getDiscountValue(), BigDecimal.ZERO);
        BigDecimal discountRate;
        if (value.compareTo(BigDecimal.valueOf(10)) > 0) {
            // 如 80 表示 8 折 (80%)
            discountRate = value.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        } else if (value.compareTo(BigDecimal.ONE) > 0) {
            // 如 8 表示 8 折
            discountRate = value.divide(BigDecimal.TEN, 4, RoundingMode.HALF_UP);
        } else if (value.compareTo(BigDecimal.ZERO) > 0) {
            // 如 0.8 表示 8 折
            discountRate = value;
        } else {
            discountRate = BigDecimal.ONE;
        }

        BigDecimal discount = total.multiply(BigDecimal.ONE.subtract(discountRate));
        if (coupon.getMaxDiscountAmount() != null && coupon.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discount = discount.min(coupon.getMaxDiscountAmount());
        }
        return discount.max(BigDecimal.ZERO).min(total);
    }

    @Override
    public int getOrder() {
        return 10; // 优先精确匹配
    }
}
