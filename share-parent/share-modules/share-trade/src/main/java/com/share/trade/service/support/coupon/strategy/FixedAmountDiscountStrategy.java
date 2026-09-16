package com.share.trade.service.support.coupon.strategy;

import com.share.trade.domain.MktCoupon;
import com.share.trade.service.support.coupon.CouponDiscountStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.share.trade.service.support.TradeUtils.defaultValue;

/**
 * 固定满减/立减折扣策略 (Fixed Amount Discount Strategy)
 */
@Component
public class FixedAmountDiscountStrategy implements CouponDiscountStrategy {

    @Override
    public boolean supports(Integer discountType) {
        // 3: 满减券, 4: 现金直减券, 或未指定默认均走固定满减
        return discountType == null || discountType != 2;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal total, MktCoupon coupon) {
        BigDecimal value = defaultValue(coupon.getDiscountValue(), BigDecimal.ZERO);
        BigDecimal discount = value;
        if (coupon.getMaxDiscountAmount() != null && coupon.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discount = discount.min(coupon.getMaxDiscountAmount());
        }
        return discount.max(BigDecimal.ZERO).min(total);
    }

    @Override
    public int getOrder() {
        return 100; // 兜底策略，排在后面
    }
}
