package com.share.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.share.common.core.exception.ServiceException;
import com.share.common.redis.service.RedisService;
import com.share.trade.domain.MktCoupon;
import com.share.trade.domain.MktCouponCode;
import com.share.trade.domain.MktUserCoupon;
import com.share.trade.mapper.MktCouponCodeMapper;
import com.share.trade.mapper.MktCouponMapper;
import com.share.trade.mapper.MktUserCouponMapper;
import com.share.trade.service.ITradeCouponService;
import com.share.trade.service.support.TradeUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.share.trade.service.support.TradeUtils.*;

/**
 * 优惠券与营销领域服务实现类
 */
@Service
@Primary
public class TradeCouponServiceImpl implements ITradeCouponService {

    private final MktCouponMapper couponMapper;
    private final MktCouponCodeMapper couponCodeMapper;
    private final MktUserCouponMapper userCouponMapper;
    private final RedisService redisService;
    private final Environment environment;
    private final com.share.trade.service.support.coupon.CouponDiscountFactory couponDiscountFactory;

    public TradeCouponServiceImpl(MktCouponMapper couponMapper,
                                  MktCouponCodeMapper couponCodeMapper,
                                  MktUserCouponMapper userCouponMapper,
                                  RedisService redisService,
                                  Environment environment,
                                  com.share.trade.service.support.coupon.CouponDiscountFactory couponDiscountFactory) {
        this.couponMapper = couponMapper;
        this.couponCodeMapper = couponCodeMapper;
        this.userCouponMapper = userCouponMapper;
        this.redisService = redisService;
        this.environment = environment;
        this.couponDiscountFactory = couponDiscountFactory;
    }

    @Override
    public List<Map<String, Object>> collectableCoupons() {
        LocalDateTime now = LocalDateTime.now();
        return couponMapper.selectList(new LambdaQueryWrapper<MktCoupon>()
                .eq(MktCoupon::getStatus, 1)
                .and(wrapper -> wrapper.isNull(MktCoupon::getStartTime).or().le(MktCoupon::getStartTime, now))
                .and(wrapper -> wrapper.isNull(MktCoupon::getEndTime).or().ge(MktCoupon::getEndTime, now))
                .and(wrapper -> wrapper.isNull(MktCoupon::getTotalCount).or().eq(MktCoupon::getTotalCount, 0)
                        .or().apply("COALESCE(received_count, 0) < total_count"))
                .orderByAsc(MktCoupon::getEndTime).orderByAsc(MktCoupon::getId))
                .stream().map(this::couponView).toList();
    }

    @Override
    public Map<String, Object> coupon(Long id) {
        MktCoupon coupon = couponMapper.selectById(id);
        if (coupon == null) throw new ServiceException("优惠券不存在");
        return couponView(coupon);
    }

    @Override
    public IPage<MktCoupon> pageCoupons(String keyword, Integer status, long pageNo, long pageSize) {
        Page<MktCoupon> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        return couponMapper.selectPage(page, new LambdaQueryWrapper<MktCoupon>()
                .like(StringUtils.hasText(keyword), MktCoupon::getCouponName, keyword)
                .eq(status != null, MktCoupon::getStatus, status)
                .orderByDesc(MktCoupon::getCreateTime));
    }

    @Override
    public Map<String, Object> pageLegacyCoupons(Map<String, ?> params) {
        long pageNo = number(params, "pageNo", number(params, "pageNum", 1));
        long pageSize = number(params, "pageSize", 10);
        String keyword = defaultText(params == null ? null : params.get("keyword"), null);
        Integer status = intValue(params == null ? null : params.get("status"));
        IPage<MktCoupon> page = pageCoupons(keyword, status, pageNo, pageSize);
        List<Map<String, Object>> rows = page.getRecords().stream().map(this::legacyCouponView).toList();
        return pageView(page.getTotal(), rows);
    }

    @Override
    @Transactional
    public Map<String, Object> saveLegacyCoupon(Map<String, ?> body) {
        MktCoupon value = new MktCoupon();
        value.setId(longValue(body == null ? null : body.get("id")));
        value.setCouponName(defaultText(body == null ? null : body.get("name"), defaultText(body == null ? null : body.get("couponName"), null)));
        require(StringUtils.hasText(value.getCouponName()), "优惠券名称不能为空");
        String type = defaultText(body == null ? null : body.get("type"), "direct");
        value.setCouponType(1);
        value.setDiscountType("percent".equalsIgnoreCase(type) ? 2 : "fixed".equalsIgnoreCase(type) ? 4 : 3);
        BigDecimal val = BigDecimal.valueOf(number(body, "value", number(body, "discountValue", 0)));
        if ("percent".equalsIgnoreCase(type)) {
            value.setDiscountValue(val.divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP));
        } else {
            value.setDiscountValue(val.movePointLeft(2));
        }
        value.setThresholdAmount(BigDecimal.valueOf(number(body, "minAmount", number(body, "thresholdAmount", 0))).movePointLeft(2));
        value.setMaxDiscountAmount(BigDecimal.valueOf(number(body, "maxDiscountAmount", 0)).movePointLeft(2));
        value.setTotalCount((int) number(body, "totalCount", 0));
        value.setStatus(intValue(body == null ? null : body.get("status"), 1));
        value.setDescription(defaultText(body == null ? null : body.get("description"), null));
        return legacyCouponView(saveCoupon(value));
    }

    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        require(id != null, "优惠券编号不能为空");
        couponMapper.deleteById(id);
    }

    @Override
    @Transactional
    public Map<String, Object> setCouponStatus(Long id, int status) {
        MktCoupon value = couponMapper.selectById(id);
        require(value != null, "优惠券不存在");
        value.setStatus(status);
        value.setUpdateTime(LocalDateTime.now());
        couponMapper.updateById(value);
        return legacyCouponView(value);
    }

    @Override
    public Map<String, Object> pageCouponCodes(Map<String, ?> params) {
        long pageNo = number(params, "pageNo", number(params, "pageNum", 1));
        long pageSize = number(params, "pageSize", 10);
        Page<MktCouponCode> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        couponCodeMapper.selectPage(page, new LambdaQueryWrapper<MktCouponCode>()
                .eq(params != null && params.get("couponId") != null, MktCouponCode::getCouponId,
                        longValue(params == null ? null : params.get("couponId")))
                .eq(params != null && params.get("status") != null, MktCouponCode::getStatus,
                        intValue(params == null ? null : params.get("status"), 0))
                .orderByDesc(MktCouponCode::getCreateTime));
        List<Map<String, Object>> rows = page.getRecords().stream().map(this::couponCodeView).toList();
        return pageView(page.getTotal(), rows);
    }

    @Override
    @Transactional
    public MktCoupon saveCoupon(MktCoupon value) {
        require(value != null && StringUtils.hasText(value.getCouponName()), "优惠券名称不能为空");
        LocalDateTime now = LocalDateTime.now();
        if (value.getId() == null) {
            value.setId(newId());
            value.setCouponType(defaultValue(value.getCouponType(), 1));
            value.setDiscountType(defaultValue(value.getDiscountType(), 3));
            value.setDiscountValue(defaultValue(value.getDiscountValue(), BigDecimal.ZERO));
            value.setThresholdAmount(defaultValue(value.getThresholdAmount(), BigDecimal.ZERO));
            value.setTotalCount(defaultValue(value.getTotalCount(), 0));
            value.setReceivedCount(0);
            value.setUsedCount(0);
            value.setStatus(defaultValue(value.getStatus(), 1));
            value.setCreateTime(now);
            value.setUpdateTime(now);
            value.setDelFlag(0);
            value.setVersion(0);
            couponMapper.insert(value);
        } else {
            value.setUpdateTime(now);
            couponMapper.updateById(value);
        }
        return value;
    }

    @Override
    public Map<String, Object> userCoupons(Map<String, ?> params) {
        long pageNo = number(params, "pageNo", 1);
        long pageSize = number(params, "pageSize", 10);
        Integer status = intValue(params == null ? null : params.get("status"));
        Page<MktUserCoupon> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        LambdaQueryWrapper<MktUserCoupon> wrapper = new LambdaQueryWrapper<MktUserCoupon>().eq(MktUserCoupon::getUserId, currentUserId());
        if (status != null) {
            int dbStatus = status == 1 ? 0 : status == 2 ? 1 : 2;
            wrapper.eq(MktUserCoupon::getStatus, dbStatus);
        }
        wrapper.orderByDesc(MktUserCoupon::getReceivedAt);
        userCouponMapper.selectPage(page, wrapper);
        return pageView(page.getTotal(), page.getRecords().stream().map(this::userCouponView).toList());
    }

    @Override
    @Transactional
    public Map<String, Object> receiveCoupon(Long couponId) {
        MktCoupon coupon = couponMapper.selectById(couponId);
        require(coupon != null && Integer.valueOf(1).equals(coupon.getStatus()), "优惠券不可领取");
        LocalDateTime now = LocalDateTime.now();
        require(coupon.getStartTime() == null || !now.isBefore(coupon.getStartTime()), "优惠券尚未生效");
        require(coupon.getEndTime() == null || !now.isAfter(coupon.getEndTime()), "优惠券已过期");

        Long userId = currentUserId();
        require(userId != null, "请先登录后再领取优惠券");
        String lockKey = "trade:lock:coupon:receive:" + couponId + ":" + userId;
        boolean locked = Boolean.TRUE.equals(redisService.setCacheObjectIfAbsent(lockKey, "1", 5L, TimeUnit.SECONDS));
        require(locked, "正在领取中，请勿频繁点击");

        String userSetKey = "trade:seckill:coupon:users:" + couponId;
        String stockKey = "trade:seckill:coupon:stock:" + couponId;

        // 1. Redis Set 防重过滤
        if (Boolean.TRUE.equals(redisService.sIsMember(userSetKey, String.valueOf(userId)))) {
            MktUserCoupon exists = userCouponMapper.selectOne(new LambdaQueryWrapper<MktUserCoupon>()
                    .eq(MktUserCoupon::getUserId, userId).eq(MktUserCoupon::getCouponId, couponId)
                    .in(MktUserCoupon::getStatus, Arrays.asList(0, 1)));
            if (exists != null) return userCouponView(exists);
        }

        MktUserCoupon exists = userCouponMapper.selectOne(new LambdaQueryWrapper<MktUserCoupon>()
                .eq(MktUserCoupon::getUserId, userId).eq(MktUserCoupon::getCouponId, couponId)
                .in(MktUserCoupon::getStatus, Arrays.asList(0, 1)));
        if (exists != null) {
            redisService.sAdd(userSetKey, String.valueOf(userId));
            return userCouponView(exists);
        }

        // 2. Redis 原子预扣库存（有限额券）
        boolean hasLimit = coupon.getTotalCount() != null && coupon.getTotalCount() > 0;
        if (hasLimit) {
            if (redisService.getCacheObject(stockKey) == null) {
                int initStock = Math.max(0, coupon.getTotalCount() - defaultValue(coupon.getReceivedCount(), 0));
                redisService.setCacheObjectIfAbsent(stockKey, initStock, 24L, TimeUnit.HOURS);
            }
            long remaining = redisService.decrement(stockKey);
            if (remaining < 0) {
                redisService.decrement(stockKey, -1);
                throw new ServiceException("优惠券已领完");
            }
        }

        try {
            // 3. 领取库存使用数据库条件更新，做双重兜底
            LambdaUpdateWrapper<MktCoupon> stockUpdate = new LambdaUpdateWrapper<MktCoupon>()
                    .setSql("received_count = COALESCE(received_count, 0) + 1")
                    .set(MktCoupon::getUpdateTime, now)
                    .eq(MktCoupon::getId, couponId).eq(MktCoupon::getStatus, 1)
                    .and(wrapper -> wrapper.isNull(MktCoupon::getTotalCount)
                            .or().eq(MktCoupon::getTotalCount, 0)
                            .or().apply("COALESCE(received_count, 0) < total_count"));
            require(couponMapper.update(null, stockUpdate) == 1, "优惠券已领完");
            MktUserCoupon value = new MktUserCoupon();
            value.setId(newId());
            value.setUserId(userId);
            value.setCouponId(couponId);
            value.setSourceType("receive");
            value.setStatus(0);
            value.setReceivedAt(now);
            value.setExpireAt(coupon.getEndTime());
            value.setCreateTime(now);
            value.setUpdateTime(now);
            value.setDelFlag(0);
            value.setVersion(0);
            userCouponMapper.insert(value);

            // 成功落库后写入 Redis 用户防重集合
            redisService.sAdd(userSetKey, String.valueOf(userId));
            return userCouponView(value);
        } catch (Exception e) {
            if (hasLimit) {
                redisService.decrement(stockKey, -1);
            }
            throw e;
        } finally {
            redisService.deleteObject(lockKey);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> exchangeCoupon(String code) {
        require(StringUtils.hasText(code), "兑换码不能为空");
        String normalizedCode = code.trim();
        Long userId = currentUserId();
        MktUserCoupon duplicate = userCouponMapper.selectOne(new LambdaQueryWrapper<MktUserCoupon>()
                .eq(MktUserCoupon::getReceiveCode, normalizedCode));
        if (duplicate != null) {
            require(Objects.equals(duplicate.getUserId(), userId), "兑换码已使用");
            return userCouponView(duplicate);
        }

        MktCouponCode couponCode = couponCodeMapper.selectOne(new LambdaQueryWrapper<MktCouponCode>()
                .eq(MktCouponCode::getCouponCode, normalizedCode).last("limit 1"));
        require(couponCode != null, "兑换码无效");
        require(Integer.valueOf(0).equals(couponCode.getStatus()), "兑换码已使用或已失效");

        MktCoupon coupon = couponMapper.selectById(couponCode.getCouponId());
        LocalDateTime now = LocalDateTime.now();
        require(coupon != null && Integer.valueOf(1).equals(coupon.getStatus()), "关联优惠券不可用");
        require(coupon.getStartTime() == null || !now.isBefore(coupon.getStartTime()), "优惠券尚未生效");
        require(coupon.getEndTime() == null || !now.isAfter(coupon.getEndTime()), "优惠券已过期");

        int exchanged = couponCodeMapper.update(null, new LambdaUpdateWrapper<MktCouponCode>()
                .set(MktCouponCode::getStatus, 1)
                .set(MktCouponCode::getExchangedUserId, userId)
                .set(MktCouponCode::getExchangedTime, now)
                .eq(MktCouponCode::getId, couponCode.getId())
                .eq(MktCouponCode::getStatus, 0));
        require(exchanged == 1, "兑换码已使用或已失效");

        LambdaUpdateWrapper<MktCoupon> couponUpdate = new LambdaUpdateWrapper<MktCoupon>()
                .setSql("received_count = COALESCE(received_count, 0) + 1")
                .set(MktCoupon::getUpdateTime, now)
                .eq(MktCoupon::getId, coupon.getId())
                .eq(MktCoupon::getStatus, 1)
                .and(wrapper -> wrapper.isNull(MktCoupon::getTotalCount)
                        .or().eq(MktCoupon::getTotalCount, 0)
                        .or().apply("received_count < total_count"));
        require(couponMapper.update(null, couponUpdate) == 1, "优惠券已领完");

        MktUserCoupon value = new MktUserCoupon();
        value.setId(newId());
        value.setUserId(userId);
        value.setCouponId(coupon.getId());
        value.setSourceType("exchange");
        value.setReceiveCode(normalizedCode);
        value.setStatus(0);
        value.setReceivedAt(now);
        value.setExpireAt(coupon.getEndTime());
        value.setCreateTime(now);
        value.setUpdateTime(now);
        value.setDelFlag(0);
        value.setVersion(0);
        userCouponMapper.insert(value);
        return userCouponView(value);
    }

    @Override
    public MktCoupon getCouponById(Long id) {
        return couponMapper.selectById(id);
    }

    @Override
    public MktUserCoupon getAvailableUserCoupon(Long userId, Long couponParam) {
        if (couponParam == null) return null;
        MktUserCoupon target = userCouponMapper.selectOne(new LambdaQueryWrapper<MktUserCoupon>()
                .eq(MktUserCoupon::getId, couponParam)
                .eq(MktUserCoupon::getUserId, userId)
                .eq(MktUserCoupon::getStatus, 0));
        if (target != null) return target;
        return userCouponMapper.selectOne(new LambdaQueryWrapper<MktUserCoupon>()
                .eq(MktUserCoupon::getCouponId, couponParam)
                .eq(MktUserCoupon::getUserId, userId)
                .eq(MktUserCoupon::getStatus, 0)
                .orderByDesc(MktUserCoupon::getReceivedAt)
                .last("limit 1"));
    }

    @Override
    public BigDecimal discount(BigDecimal total, MktCoupon coupon) {
        if (coupon == null) return BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        require(Integer.valueOf(1).equals(coupon.getStatus()), "优惠券不可用");
        require(coupon.getStartTime() == null || !now.isBefore(coupon.getStartTime()), "优惠券尚未生效");
        require(coupon.getEndTime() == null || !now.isAfter(coupon.getEndTime()), "优惠券已过期");
        BigDecimal threshold = defaultValue(coupon.getThresholdAmount(), BigDecimal.ZERO);
        if (total.compareTo(threshold) < 0) return BigDecimal.ZERO;
        return couponDiscountFactory.calculate(total, coupon);
    }

    @Override
    @Transactional
    public void restoreUserCoupon(Long userCouponId) {
        if (userCouponId == null) return;
        MktUserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc != null) {
            uc.setStatus(0);
            uc.setUsedAt(null);
            userCouponMapper.updateById(uc);
            if (uc.getCouponId() != null) {
                couponMapper.update(null, new LambdaUpdateWrapper<MktCoupon>()
                        .setSql("used_count = GREATEST(0, COALESCE(used_count, 0) - 1)")
                        .eq(MktCoupon::getId, uc.getCouponId()));
            }
        }
    }

    @Override
    @Transactional
    public void markUserCouponUsed(Long userCouponId, Long orderId) {
        if (userCouponId == null) return;
        MktUserCoupon target = userCouponMapper.selectById(userCouponId);
        if (target != null) {
            target.setStatus(1);
            target.setUsedAt(LocalDateTime.now());
            userCouponMapper.updateById(target);
            if (target.getCouponId() != null) {
                couponMapper.update(null, new LambdaUpdateWrapper<MktCoupon>()
                        .setSql("used_count = COALESCE(used_count, 0) + 1")
                        .eq(MktCoupon::getId, target.getCouponId()));
            }
        }
    }

    @Override
    public Map<String, Object> couponView(MktCoupon item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("name", item.getCouponName());
        result.put("couponName", item.getCouponName());
        result.put("type", item.getCouponType());
        result.put("discountType", item.getDiscountType());
        result.put("discountValue", cents(item.getDiscountValue()));
        result.put("thresholdAmount", cents(item.getThresholdAmount()));
        result.put("maxDiscountAmount", cents(item.getMaxDiscountAmount()));
        result.put("totalCount", item.getTotalCount());
        result.put("receivedCount", item.getReceivedCount());
        result.put("usedCount", item.getUsedCount());
        result.put("startTime", item.getStartTime());
        result.put("endTime", item.getEndTime());
        result.put("termEndTime", item.getEndTime());
        result.put("status", item.getStatus());
        result.put("description", item.getDescription());
        result.put("available", item.getTotalCount() == null || item.getTotalCount() == 0
                || defaultValue(item.getReceivedCount(), 0) < item.getTotalCount());
        result.put("received", false);
        return result;
    }

    private Map<String, Object> legacyCouponView(MktCoupon item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("name", item.getCouponName());
        result.put("couponName", item.getCouponName());
        String type = item.getDiscountType() != null && item.getDiscountType() == 2 ? "percent"
                : item.getDiscountType() != null && item.getDiscountType() == 4 ? "fixed" : "direct";
        result.put("type", type);
        result.put("discountType", item.getDiscountType());
        result.put("value", "percent".equals(type) ? defaultValue(item.getDiscountValue(), BigDecimal.ZERO).multiply(BigDecimal.TEN).longValue()
                : cents(item.getDiscountValue()));
        result.put("discountValue", result.get("value"));
        result.put("minAmount", cents(item.getThresholdAmount()));
        result.put("thresholdAmount", cents(item.getThresholdAmount()));
        result.put("maxDiscountAmount", cents(item.getMaxDiscountAmount()));
        result.put("totalCount", item.getTotalCount());
        result.put("receivedCount", item.getReceivedCount());
        result.put("usedCount", item.getUsedCount());
        result.put("startTime", item.getStartTime());
        result.put("endTime", item.getEndTime());
        result.put("termEndTime", item.getEndTime());
        result.put("status", item.getStatus());
        result.put("description", item.getDescription());
        result.put("available", item.getStatus() != null && item.getStatus() == 1
                && (item.getTotalCount() == null || item.getTotalCount() == 0
                || defaultValue(item.getReceivedCount(), 0) < item.getTotalCount()));
        return result;
    }

    private Map<String, Object> couponCodeView(MktCouponCode item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("couponId", item.getCouponId());
        result.put("code", item.getCouponCode());
        result.put("couponCode", item.getCouponCode());
        result.put("status", item.getStatus());
        result.put("exchangedUserId", item.getExchangedUserId());
        result.put("exchangedTime", item.getExchangedTime());
        result.put("createTime", item.getCreateTime());
        MktCoupon coupon = couponMapper.selectById(item.getCouponId());
        if (coupon != null) result.put("couponName", coupon.getCouponName());
        return result;
    }

    @Override
    public Map<String, Object> userCouponView(MktUserCoupon item) {
        Map<String, Object> result = couponView(Optional.ofNullable(couponMapper.selectById(item.getCouponId())).orElseGet(MktCoupon::new));
        result.put("id", item.getId());
        result.put("couponId", item.getCouponId());
        result.put("status", item.getStatus() == 0 ? 1 : item.getStatus() == 1 ? 2 : 3);
        result.put("receivedAt", item.getReceivedAt());
        result.put("expireAt", item.getExpireAt());
        result.put("usedAt", item.getUsedAt());
        result.put("receiveCode", item.getReceiveCode());
        return result;
    }
}

