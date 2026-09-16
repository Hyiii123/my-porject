package com.share.trade.service.support.coupon;

import com.share.trade.domain.MktCoupon;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 优惠券折扣策略工厂 (Coupon Discount Strategy Factory)
 */
@Slf4j
@Component
public class CouponDiscountFactory {

    private final List<CouponDiscountStrategy> strategies;

    public CouponDiscountFactory(List<CouponDiscountStrategy> strategies) {
        if (strategies == null || strategies.isEmpty()) {
            this.strategies = Collections.emptyList();
            log.warn("【CouponDiscountFactory】未注册任何优惠券折扣策略！");
        } else {
            this.strategies = strategies.stream()
                    .sorted(Comparator.comparingInt(CouponDiscountStrategy::getOrder))
                    .collect(Collectors.toList());
            log.info("【CouponDiscountFactory】成功加载优惠券折扣策略，共 {} 个: {}",
                    this.strategies.size(),
                    this.strategies.stream().map(s -> s.getClass().getSimpleName()).toList());
        }
    }

    /**
     * 根据优惠券折扣类型选择对应策略并计算减免金额
     */
    public BigDecimal calculate(BigDecimal total, MktCoupon coupon) {
        if (coupon == null || total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        Integer discountType = coupon.getDiscountType();
        for (CouponDiscountStrategy strategy : strategies) {
            if (strategy.supports(discountType)) {
                return strategy.calculateDiscount(total, coupon);
            }
        }
        log.warn("【CouponDiscountFactory】未找到 discountType={} 的折扣策略，默认不抵扣", discountType);
        return BigDecimal.ZERO;
    }
}
