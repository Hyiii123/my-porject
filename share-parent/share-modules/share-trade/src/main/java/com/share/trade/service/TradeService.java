package com.share.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.share.common.core.exception.ServiceException;
import com.share.common.security.utils.SecurityUtils;
import com.share.common.core.web.domain.AjaxResult;
import com.share.education.api.RemoteEducationService;
import com.share.trade.domain.MktCoupon;
import com.share.trade.domain.MktCouponCode;
import com.share.trade.domain.MktUserCoupon;
import com.share.trade.domain.TrCart;
import com.share.trade.domain.TrOrder;
import com.share.trade.domain.TrOrderItem;
import com.share.trade.domain.TrPaymentOrder;
import com.share.trade.domain.TrRefundApply;
import com.share.trade.mapper.MktCouponMapper;
import com.share.trade.mapper.MktCouponCodeMapper;
import com.share.trade.mapper.MktUserCouponMapper;
import com.share.trade.mapper.TrCartMapper;
import com.share.trade.mapper.TrOrderItemMapper;
import com.share.trade.mapper.TrOrderMapper;
import com.share.trade.mapper.TrPaymentOrderMapper;
import com.share.trade.mapper.TrRefundApplyMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 交易领域服务。真实支付渠道没有接入，支付单和二维码采用可追踪的演示状态，
 * 订单、优惠券、退款仍然完整落库，后续接入渠道时只需替换支付适配器。
 */
@Service
public class TradeService {
    private final MktCouponMapper couponMapper;
    private final MktCouponCodeMapper couponCodeMapper;
    private final MktUserCouponMapper userCouponMapper;
    private final TrCartMapper cartMapper;
    private final TrOrderMapper orderMapper;
    private final TrOrderItemMapper itemMapper;
    private final TrPaymentOrderMapper paymentMapper;
    private final TrRefundApplyMapper refundMapper;
    private final RemoteEducationService educationService;

    public TradeService(MktCouponMapper couponMapper, MktCouponCodeMapper couponCodeMapper, MktUserCouponMapper userCouponMapper,
            TrCartMapper cartMapper, TrOrderMapper orderMapper, TrOrderItemMapper itemMapper,
            TrPaymentOrderMapper paymentMapper, TrRefundApplyMapper refundMapper,
            RemoteEducationService educationService) {
        this.couponMapper = couponMapper;
        this.couponCodeMapper = couponCodeMapper;
        this.userCouponMapper = userCouponMapper;
        this.cartMapper = cartMapper;
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.paymentMapper = paymentMapper;
        this.refundMapper = refundMapper;
        this.educationService = educationService;
    }

    public List<Map<String, Object>> collectableCoupons() {
        return couponMapper.selectList(new LambdaQueryWrapper<MktCoupon>()
                .eq(MktCoupon::getStatus, 1).orderByAsc(MktCoupon::getEndTime).orderByAsc(MktCoupon::getId))
                .stream().map(this::couponView).toList();
    }

    public Map<String, Object> coupon(Long id) {
        MktCoupon coupon = couponMapper.selectById(id);
        if (coupon == null) throw new ServiceException("优惠券不存在");
        return couponView(coupon);
    }

    public IPage<MktCoupon> pageCoupons(String keyword, Integer status, long pageNo, long pageSize) {
        Page<MktCoupon> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        return couponMapper.selectPage(page, new LambdaQueryWrapper<MktCoupon>()
                .like(StringUtils.hasText(keyword), MktCoupon::getCouponName, keyword)
                .eq(status != null, MktCoupon::getStatus, status).orderByDesc(MktCoupon::getCreateTime));
    }

    /** 旧营销页使用 name/type/minAmount/value 字段和 TableDataInfo。 */
    public Map<String, Object> pageLegacyCoupons(Map<String, ?> params) {
        long pageNo = number(params, "pageNo", number(params, "pageNum", 1));
        long pageSize = number(params, "pageSize", 10);
        Integer status = intValue(params == null ? null : params.get("status"));
        String keyword = defaultText(params == null ? null : params.get("keyword"), null);
        String type = defaultText(params == null ? null : params.get("type"), null);
        IPage<MktCoupon> page = pageCoupons(keyword, status, pageNo, pageSize);
        List<Map<String, Object>> rows = page.getRecords().stream().map(this::legacyCouponView)
                .filter(row -> !StringUtils.hasText(type) || type.equals(row.get("type"))).toList();
        return pageView(rows.size() == page.getRecords().size() ? page.getTotal() : rows.size(), rows);
    }

    @Transactional
    public Map<String, Object> saveLegacyCoupon(Map<String, ?> body) {
        Long id = longValue(body == null ? null : body.get("id"));
        MktCoupon value = id == null ? new MktCoupon() : couponMapper.selectById(id);
        if (value == null) throw new ServiceException("优惠券不存在");
        String name = defaultText(body == null ? null : body.get("name"),
                defaultText(body == null ? null : body.get("couponName"), null));
        require(StringUtils.hasText(name), "优惠券名称不能为空");
        String type = defaultText(body == null ? null : body.get("type"), "direct");
        value.setCouponName(name.trim());
        value.setCouponType(1);
        value.setDiscountType("percent".equals(type) ? 2 : "fixed".equals(type) ? 4 : 3);
        BigDecimal legacyValue = decimalValue(body == null ? null : body.get("value"), BigDecimal.ZERO);
        value.setDiscountValue("percent".equals(type) ? legacyValue.movePointLeft(1) : legacyValue.movePointLeft(2));
        value.setThresholdAmount(decimalValue(body == null ? null : body.get("minAmount"), BigDecimal.ZERO).movePointLeft(2));
        value.setMaxDiscountAmount(value.getDiscountType() == 2 ? value.getDiscountValue().movePointLeft(0) : value.getDiscountValue());
        value.setTotalCount(intValue(body == null ? null : body.get("totalCount"), 0));
        value.setStatus(intValue(body == null ? null : body.get("status"), defaultValue(value.getStatus(), 1)));
        value.setStartTime(dateTime(body == null ? null : body.get("startTime"), LocalDateTime.now()));
        value.setEndTime(dateTime(body == null ? null : body.get("endTime"), value.getStartTime().plusYears(1)));
        value.setDescription(defaultText(body == null ? null : body.get("description"), value.getDescription()));
        LocalDateTime now = LocalDateTime.now();
        if (value.getId() == null) {
            value.setId(newId()); value.setReceivedCount(0); value.setUsedCount(0); value.setCreateTime(now);
            value.setUpdateTime(now); value.setDelFlag(0); value.setVersion(0); couponMapper.insert(value);
        } else { value.setUpdateTime(now); couponMapper.updateById(value); }
        return legacyCouponView(value);
    }

    @Transactional
    public void deleteCoupon(Long id) {
        require(id != null, "优惠券编号不能为空");
        couponMapper.deleteById(id);
    }

    @Transactional
    public Map<String, Object> setCouponStatus(Long id, int status) {
        MktCoupon value = couponMapper.selectById(id);
        require(value != null, "优惠券不存在");
        value.setStatus(status); value.setUpdateTime(LocalDateTime.now()); couponMapper.updateById(value);
        return legacyCouponView(value);
    }

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

    @Transactional
    public MktCoupon saveCoupon(MktCoupon value) {
        require(value != null && StringUtils.hasText(value.getCouponName()), "优惠券名称不能为空");
        LocalDateTime now = LocalDateTime.now();
        if (value.getId() == null) {
            value.setId(newId()); value.setCouponType(defaultValue(value.getCouponType(), 1)); value.setDiscountType(defaultValue(value.getDiscountType(), 3));
            value.setDiscountValue(defaultValue(value.getDiscountValue(), BigDecimal.ZERO)); value.setThresholdAmount(defaultValue(value.getThresholdAmount(), BigDecimal.ZERO));
            value.setTotalCount(defaultValue(value.getTotalCount(), 0)); value.setReceivedCount(0); value.setUsedCount(0); value.setStatus(defaultValue(value.getStatus(), 1));
            value.setCreateTime(now); value.setUpdateTime(now); value.setDelFlag(0); value.setVersion(0); couponMapper.insert(value);
        } else { value.setUpdateTime(now); couponMapper.updateById(value); }
        return value;
    }

    public Map<String, Object> userCoupons(Map<String, ?> params) {
        long pageNo = number(params, "pageNo", 1); long pageSize = number(params, "pageSize", 10);
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

    @Transactional
    public Map<String, Object> receiveCoupon(Long couponId) {
        MktCoupon coupon = couponMapper.selectById(couponId);
        require(coupon != null && Integer.valueOf(1).equals(coupon.getStatus()), "优惠券不可领取");
        MktUserCoupon exists = userCouponMapper.selectOne(new LambdaQueryWrapper<MktUserCoupon>()
                .eq(MktUserCoupon::getUserId, currentUserId()).eq(MktUserCoupon::getCouponId, couponId)
                .in(MktUserCoupon::getStatus, Arrays.asList(0, 1)));
        if (exists != null) return userCouponView(exists);
        int received = defaultValue(coupon.getReceivedCount(), 0);
        if (coupon.getTotalCount() != null && coupon.getTotalCount() > 0 && received >= coupon.getTotalCount()) {
            throw new ServiceException("优惠券已领完");
        }
        MktUserCoupon value = new MktUserCoupon();
        value.setId(newId()); value.setUserId(currentUserId()); value.setCouponId(couponId); value.setSourceType("receive");
        value.setStatus(0); value.setReceivedAt(LocalDateTime.now()); value.setExpireAt(coupon.getEndTime()); value.setCreateTime(LocalDateTime.now()); value.setUpdateTime(LocalDateTime.now()); value.setDelFlag(0); value.setVersion(0);
        userCouponMapper.insert(value);
        coupon.setReceivedCount(received + 1); coupon.setUpdateTime(LocalDateTime.now()); couponMapper.updateById(coupon);
        return userCouponView(value);
    }

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

        // 兑换码状态使用条件更新，两个并发请求只有一个能把 0 更新为 1。
        int exchanged = couponCodeMapper.update(null, new LambdaUpdateWrapper<MktCouponCode>()
                .set(MktCouponCode::getStatus, 1)
                .set(MktCouponCode::getExchangedUserId, userId)
                .set(MktCouponCode::getExchangedTime, now)
                .eq(MktCouponCode::getId, couponCode.getId())
                .eq(MktCouponCode::getStatus, 0));
        require(exchanged == 1, "兑换码已使用或已失效");

        // 兑换同样计入优惠券发放量，并用条件更新防止超过总量。
        LambdaUpdateWrapper<MktCoupon> couponUpdate = new LambdaUpdateWrapper<MktCoupon>()
                .setSql("received_count = COALESCE(received_count, 0) + 1")
                .set(MktCoupon::getUpdateTime, now)
                .eq(MktCoupon::getId, coupon.getId())
                .eq(MktCoupon::getStatus, 1)
                .and(wrapper -> wrapper.isNull(MktCoupon::getTotalCount)
                        .or().eq(MktCoupon::getTotalCount, 0)
                        .or().apply("received_count < total_count"));
        require(couponMapper.update(null, couponUpdate) == 1, "优惠券已领完");

        MktUserCoupon value = new MktUserCoupon(); value.setId(newId()); value.setUserId(userId); value.setCouponId(coupon.getId()); value.setSourceType("exchange"); value.setReceiveCode(normalizedCode); value.setStatus(0); value.setReceivedAt(now); value.setExpireAt(coupon.getEndTime()); value.setCreateTime(now); value.setUpdateTime(now); value.setDelFlag(0); value.setVersion(0); userCouponMapper.insert(value);
        return userCouponView(value);
    }

    public List<Map<String, Object>> carts() {
        return cartMapper.selectList(new LambdaQueryWrapper<TrCart>().eq(TrCart::getUserId, currentUserId()).orderByDesc(TrCart::getUpdateTime))
                .stream().map(this::cartView).toList();
    }

    @Transactional
    public Map<String, Object> addCart(Map<String, ?> body) {
        Long courseId = longValue(body == null ? null : body.get("courseId"));
        require(courseId != null, "课程编号不能为空");
        Long userId = currentUserId();
        TrCart value = cartMapper.selectOne(new LambdaQueryWrapper<TrCart>().eq(TrCart::getUserId, userId).eq(TrCart::getCourseId, courseId));
        if (value == null) {
            value = new TrCart(); value.setId(newId()); value.setUserId(userId); value.setCourseId(courseId);
            fillCartSnapshot(value, courseSnapshot(courseId));
            value.setQuantity(1); value.setSelected(1); value.setCreateTime(LocalDateTime.now()); value.setUpdateTime(LocalDateTime.now()); value.setDelFlag(0);
            try {
                cartMapper.insert(value);
            } catch (DuplicateKeyException ex) {
                // 两个请求同时加购时由有效购物车唯一索引仲裁，返回已经成功写入的记录。
                value = cartMapper.selectOne(new LambdaQueryWrapper<TrCart>()
                        .eq(TrCart::getUserId, userId).eq(TrCart::getCourseId, courseId));
                require(value != null, "购物车写入失败，请稍后重试");
            }
        }
        return cartView(value);
    }

    @Transactional
    public void removeCarts(String ids) {
        if (!StringUtils.hasText(ids)) return;
        for (String id : ids.split(",")) {
            Long cartId = longValue(id);
            if (cartId != null) cartMapper.delete(new LambdaQueryWrapper<TrCart>().eq(TrCart::getId, cartId).eq(TrCart::getUserId, currentUserId()));
        }
    }

    public Map<String, Object> listOrders(Map<String, ?> params, boolean admin) {
        long pageNo = number(params, "pageNo", 1); long pageSize = number(params, "pageSize", 10);
        Integer oldStatus = legacyOrderStatus(params == null ? null : params.get("status"));
        Page<TrOrder> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        LambdaQueryWrapper<TrOrder> wrapper = new LambdaQueryWrapper<TrOrder>()
                .eq(!admin, TrOrder::getUserId, currentUserId());
        if (oldStatus != null && oldStatus > 0) wrapper.eq(TrOrder::getOrderStatus, toDbOrderStatus(oldStatus));
        wrapper.orderByDesc(TrOrder::getCreateTime);
        orderMapper.selectPage(page, wrapper);
        return pageView(page.getTotal(), page.getRecords().stream().map(this::orderView).toList());
    }

    public Map<String, Object> order(Long id) {
        TrOrder value = findOrder(id);
        if (!Objects.equals(value.getUserId(), currentUserId()) && !SecurityUtils.isAdmin(currentUserId())) throw new ServiceException("无权查看该订单");
        return orderView(value);
    }

    public Map<String, Object> prePlaceOrder(Map<String, ?> params) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("availableCoupons", collectableCoupons());
        result.put("coupons", collectableCoupons());
        List<Map<String, Object>> cartItems = carts();
        List<Long> requestedIds = longValues(params == null ? null : params.get("ids"));
        if (requestedIds.isEmpty()) requestedIds = longValues(params == null ? null : params.get("cartIds"));
        if (!requestedIds.isEmpty()) {
            List<Long> finalRequestedIds = requestedIds;
            cartItems = cartItems.stream().filter(item -> finalRequestedIds.contains(longValue(item.get("id")))).toList();
        }
        Long directCourseId = longValue(params == null ? null : params.get("courseId"));
        if (cartItems.isEmpty() && directCourseId != null) {
            Map<String, Object> snapshot = courseSnapshot(directCourseId);
            Map<String, Object> directItem = new LinkedHashMap<>();
            directItem.put("id", null); directItem.put("courseId", directCourseId); directItem.put("quantity", 1);
            directItem.put("courseName", defaultText(snapshot.get("title"), defaultText(snapshot.get("courseName"), "课程 " + directCourseId)));
            directItem.put("teacherName", defaultText(snapshot.get("teacherName"), "讲师团队"));
            directItem.put("cover", defaultText(snapshot.get("cover"), defaultText(snapshot.get("coverUrl"), "")));
            directItem.put("price", number(snapshot, "price", defaultPriceCents(directCourseId)));
            directItem.put("originalPrice", number(snapshot, "originalPrice", number(snapshot, "price", defaultPriceCents(directCourseId))));
            cartItems = List.of(directItem);
        }
        result.put("items", cartItems);
        result.put("totalAmount", cartItems.stream().mapToLong(item -> number(item, "price", 0) * Math.max(1, number(item, "quantity", 1))).sum());
        result.put("cartIds", cartItems.stream().map(item -> longValue(item.get("id"))).filter(Objects::nonNull).toList());
        return result;
    }

    @Transactional
    public Map<String, Object> placeOrder(Map<String, ?> body) {
        List<Map<String, ?>> sourceItems = new ArrayList<>();
        boolean fromCart = false;
        Object itemObject = body == null ? null : body.get("items");
        if (itemObject instanceof List<?> list) {
            for (Object item : list) if (item instanceof Map<?, ?> map) sourceItems.add(castMap(map));
        }
        if (sourceItems.isEmpty()) {
            Long courseId = longValue(body == null ? null : body.get("courseId"));
            if (courseId != null) { Map<String, Object> one = new LinkedHashMap<>(); one.put("courseId", courseId); one.putAll(body); sourceItems.add(one); }
        }
        if (sourceItems.isEmpty()) {
            List<Long> requestedIds = longValues(body == null ? null : body.get("cartIds"));
            if (requestedIds.isEmpty()) requestedIds = longValues(body == null ? null : body.get("ids"));
            List<Map<String, Object>> cartItems = carts();
            if (!requestedIds.isEmpty()) {
                List<Long> finalRequestedIds = requestedIds;
                cartItems = cartItems.stream().filter(item -> finalRequestedIds.contains(longValue(item.get("id")))).toList();
            }
            sourceItems.addAll(cartItems);
            fromCart = true;
        }
        require(!sourceItems.isEmpty(), "订单课程不能为空");
        BigDecimal total = BigDecimal.ZERO;
        List<TrOrderItem> items = new ArrayList<>();
        for (Map<String, ?> source : sourceItems) {
            Long courseId = longValue(source.get("courseId")); require(courseId != null, "订单课程编号不能为空");
            Map<String, Object> snapshot = courseSnapshot(courseId);
            Object sourceName = source.get("courseName");
            boolean forceFree = bool(source.get("free")) || bool(source.get("isFree"))
                    || (source.containsKey("price") && number(source, "price", -1) == 0
                    && sourceName != null && String.valueOf(sourceName).startsWith("免费课程"));
            long cents = forceFree ? 0 : number(snapshot, "price", number(source, "price", defaultPriceCents(courseId)));
            BigDecimal amount = BigDecimal.valueOf(cents).movePointLeft(2);
            String courseName = forceFree ? defaultText(source.get("courseName"), "免费课程 " + courseId)
                    : defaultText(snapshot.get("title"), defaultText(snapshot.get("courseName"), defaultText(source.get("courseName"), defaultText(source.get("title"), "课程 " + courseId))));
            String cover = defaultText(snapshot.get("cover"), defaultText(snapshot.get("coverUrl"), defaultText(source.get("cover"), null)));
            TrOrderItem item = new TrOrderItem(); item.setId(newId()); item.setCourseId(courseId); item.setCourseName(courseName); item.setCourseCoverUrl(cover); item.setUnitPrice(amount); item.setQuantity(Math.max(1, (int) number(source, "quantity", 1))); item.setDiscountAmount(BigDecimal.ZERO); item.setPayableAmount(amount.multiply(BigDecimal.valueOf(item.getQuantity()))); item.setCreateTime(LocalDateTime.now()); items.add(item); total = total.add(item.getPayableAmount());
        }
        Long couponId = longValue(body == null ? null : body.get("couponId"));
        BigDecimal discount = discount(total, couponId);
        BigDecimal payable = total.subtract(discount).max(BigDecimal.ZERO);
        LocalDateTime now = LocalDateTime.now();
        TrOrder order = new TrOrder(); order.setId(newId()); order.setOrderNo("TJ" + now.toString().replaceAll("[-:T]", "").substring(0, 14) + String.format("%04d", orderMapper.selectCount(new LambdaQueryWrapper<>() ) + 1)); order.setUserId(currentUserId()); order.setTotalAmount(total); order.setDiscountAmount(discount); order.setPayableAmount(payable); order.setPaidAmount(BigDecimal.ZERO); order.setCouponId(couponId); order.setOrderStatus(0); order.setPaymentStatus(0); order.setExpireTime(now.plusMinutes(30)); order.setCreateTime(now); order.setUpdateTime(now); order.setDelFlag(0); order.setVersion(0); orderMapper.insert(order);
        for (TrOrderItem item : items) { item.setOrderId(order.getId()); item.setDiscountAmount(discount.multiply(item.getPayableAmount()).divide(total.signum() == 0 ? BigDecimal.ONE : total, 2, RoundingMode.HALF_UP)); item.setPayableAmount(item.getPayableAmount().subtract(item.getDiscountAmount())); itemMapper.insert(item); }
        for (Map<String, ?> source : sourceItems) {
            Long cartId = longValue(source.get("id"));
            if (cartId != null && (fromCart || itemObject instanceof List<?>)) {
                cartMapper.delete(new LambdaQueryWrapper<TrCart>().eq(TrCart::getId, cartId).eq(TrCart::getUserId, currentUserId()));
            }
        }
        return orderView(order);
    }

    @Transactional
    public Map<String, Object> freeCourse(Long courseId) {
        require(courseId != null, "课程编号不能为空");
        Map<String, Object> body = new LinkedHashMap<>(); body.put("courseId", courseId); body.put("price", 0); body.put("free", true); body.put("courseName", "免费课程 " + courseId);
        Map<String, Object> result = placeOrder(body);
        TrOrder order = findOrder(longValue(result.get("id"))); order.setOrderStatus(1); order.setPaymentStatus(1); order.setPaidAmount(BigDecimal.ZERO); order.setPaidTime(LocalDateTime.now()); order.setUpdateTime(LocalDateTime.now()); orderMapper.updateById(order); enrollPurchasedCourses(order); return orderView(order);
    }

    @Transactional
    public Map<String, Object> cancelOrder(Long id) {
        TrOrder order = findOrder(id); require(Objects.equals(order.getUserId(), currentUserId()) || SecurityUtils.isAdmin(currentUserId()), "无权取消订单"); require(order.getOrderStatus() == 0, "当前订单不能取消"); order.setOrderStatus(4); order.setUpdateTime(LocalDateTime.now()); orderMapper.updateById(order); return orderView(order);
    }

    @Transactional
    public void deleteOrder(Long id) {
        TrOrder order = findOrder(id); require(Objects.equals(order.getUserId(), currentUserId()) || SecurityUtils.isAdmin(currentUserId()), "无权删除订单"); orderMapper.deleteById(id);
    }

    public List<Map<String, Object>> paymentChannels() {
        return List.of(Map.of("id", "wechat", "name", "微信支付", "type", "wechat"), Map.of("id", "alipay", "name", "支付宝", "type", "alipay"));
    }

    @Transactional
    public Map<String, Object> createPayment(Map<String, ?> body) {
        Long orderId = longValue(body == null ? null : body.get("orderId")); TrOrder order = findOrder(orderId); require(Objects.equals(order.getUserId(), currentUserId()), "无权支付该订单");
        String channel = defaultText(body == null ? null : body.get("channel"), defaultText(body == null ? null : body.get("paymentChannel"), "wechat"));
        TrPaymentOrder payment = paymentMapper.selectOne(new LambdaQueryWrapper<TrPaymentOrder>().eq(TrPaymentOrder::getOrderId, order.getId()).eq(TrPaymentOrder::getPaymentChannel, channel));
        if (payment == null) { payment = new TrPaymentOrder(); payment.setId(newId()); payment.setPaymentNo("PAY" + System.currentTimeMillis()); payment.setOrderId(order.getId()); payment.setPaymentChannel(channel); payment.setAmount(order.getPayableAmount()); payment.setStatus(0); payment.setExpireTime(order.getExpireTime()); payment.setCreateTime(LocalDateTime.now()); payment.setUpdateTime(LocalDateTime.now()); payment.setVersion(0); paymentMapper.insert(payment); }
        order.setPaymentChannel(channel); order.setUpdateTime(LocalDateTime.now()); orderMapper.updateById(order);
        Map<String, Object> result = new LinkedHashMap<>(); result.put("paymentNo", payment.getPaymentNo()); result.put("orderId", order.getId()); result.put("amount", cents(order.getPayableAmount())); result.put("payUrl", "demo://tianji-pay/" + payment.getPaymentNo()); result.put("status", payment.getStatus()); result.put("demo", true); return result;
    }

    /** 本地演示支付回调：不连接真实支付渠道，但完整更新支付单、订单和学习记录。 */
    @Transactional
    public Map<String, Object> simulatePayment(Long orderId) {
        TrOrder order = findOrder(orderId);
        require(Objects.equals(order.getUserId(), currentUserId()), "无权支付该订单");
        LocalDateTime now = LocalDateTime.now();
        TrPaymentOrder payment = paymentMapper.selectOne(new LambdaQueryWrapper<TrPaymentOrder>()
                .eq(TrPaymentOrder::getOrderId, orderId).orderByDesc(TrPaymentOrder::getCreateTime).last("limit 1"));
        if (payment == null) {
            payment = new TrPaymentOrder(); payment.setId(newId()); payment.setPaymentNo("PAY" + System.currentTimeMillis());
            payment.setOrderId(orderId); payment.setPaymentChannel(defaultText(order.getPaymentChannel(), "wechat"));
            payment.setAmount(order.getPayableAmount()); payment.setExpireTime(order.getExpireTime());
            payment.setCreateTime(now); payment.setVersion(0);
        }
        payment.setStatus(1); payment.setThirdPartyNo("DEMO-" + payment.getPaymentNo()); payment.setPaidTime(now); payment.setUpdateTime(now);
        if (paymentMapper.selectById(payment.getId()) == null) paymentMapper.insert(payment); else paymentMapper.updateById(payment);
        order.setPaymentStatus(1); order.setOrderStatus(1); order.setPaidAmount(order.getPayableAmount()); order.setPaidTime(now);
        order.setPaymentChannel(payment.getPaymentChannel()); order.setUpdateTime(now); orderMapper.updateById(order);
        enrollPurchasedCourses(order);
        return orderView(order);
    }

    public Map<String, Object> paymentState(Long orderId) {
        TrOrder order = findOrder(orderId); Map<String, Object> result = new LinkedHashMap<>(); result.put("orderId", order.getId()); result.put("status", order.getPaymentStatus()); result.put("paymentStatus", order.getPaymentStatus()); result.put("expireTime", order.getExpireTime()); result.put("paidTime", order.getPaidTime()); return result;
    }

    @Transactional
    public Map<String, Object> applyRefund(Map<String, ?> body) {
        Long detailId = longValue(body == null ? null : body.get("orderDetailId")); TrOrderItem item = detailId == null ? null : itemMapper.selectById(detailId); require(item != null, "订单明细不存在"); TrOrder order = findOrder(item.getOrderId()); require(Objects.equals(order.getUserId(), currentUserId()), "无权申请退款");
        TrRefundApply old = refundMapper.selectOne(new LambdaQueryWrapper<TrRefundApply>().eq(TrRefundApply::getOrderId, order.getId()).orderByDesc(TrRefundApply::getCreateTime).last("limit 1")); if (old != null) return refundView(old);
        TrRefundApply value = new TrRefundApply(); value.setId(newId()); value.setRefundNo("REF" + System.currentTimeMillis()); value.setOrderId(order.getId()); value.setUserId(currentUserId()); value.setRefundAmount(item.getPayableAmount()); value.setReason(defaultText(body == null ? null : body.get("refundReason"), defaultText(body == null ? null : body.get("questionDesc"), "用户申请退款"))); value.setStatus(0); value.setCreateTime(LocalDateTime.now()); value.setUpdateTime(LocalDateTime.now()); value.setVersion(0); value.setDelFlag(0); refundMapper.insert(value); return refundView(value);
    }

    public Map<String, Object> refund(Long id) {
        TrRefundApply value = refundMapper.selectById(id);
        if (value == null) { TrOrderItem item = itemMapper.selectById(id); if (item != null) value = refundMapper.selectOne(new LambdaQueryWrapper<TrRefundApply>().eq(TrRefundApply::getOrderId, item.getOrderId()).orderByDesc(TrRefundApply::getCreateTime).last("limit 1")); }
        if (value == null) throw new ServiceException("退款记录不存在"); return refundView(value);
    }

    /** 旧管理端订单明细接口按一条课程明细返回一行。 */
    public Map<String, Object> pageLegacyOrderDetails(Map<String, ?> params) {
        Map<String, Object> source = listOrders(params == null ? Map.of() : params, true);
        List<?> orders = (List<?>) source.getOrDefault("list", List.of());
        List<Map<String, Object>> rows = new ArrayList<>();
        String keyword = defaultText(params == null ? null : params.get("keyword"), null);
        for (Object orderObject : orders) {
            if (!(orderObject instanceof Map<?, ?> map)) continue;
            Map<String, Object> order = castMap(map);
            Object detailsObject = order.get("details");
            List<?> details = detailsObject instanceof List<?> list ? list : List.of();
            if (details.isEmpty()) {
                Map<String, Object> row = legacyOrderDetailView(order, Map.<String, Object>of(), keyword);
                if (row != null) rows.add(row);
            }
            else for (Object detailObject : details) {
                Map<String, Object> detail = detailObject instanceof Map<?, ?> detailMap ? castMap(detailMap) : Map.<String, Object>of();
                Map<String, Object> row = legacyOrderDetailView(order, detail, keyword);
                if (row != null) rows.add(row);
            }
        }
        return pageView(rows.size(), rows);
    }

    public Map<String, Object> legacyOrderDetail(Long id) {
        Map<String, Object> order = order(id);
        List<?> details = order.get("details") instanceof List<?> list ? list : List.of();
        Map<String, Object> detail = details.isEmpty() ? Map.<String, Object>of() : castMap((Map<?, ?>) details.get(0));
        return legacyOrderDetailView(order, detail, null);
    }

    public IPage<TrRefundApply> pageRefunds(Integer status, long pageNo, long pageSize) {
        Page<TrRefundApply> page = new Page<>(safePage(pageNo), safeSize(pageSize)); refundMapper.selectPage(page, new LambdaQueryWrapper<TrRefundApply>().eq(status != null, TrRefundApply::getStatus, status).orderByDesc(TrRefundApply::getCreateTime)); return page;
    }

    public Map<String, Object> pageLegacyRefunds(Map<String, ?> params) {
        long pageNo = number(params, "pageNo", number(params, "pageNum", 1));
        long pageSize = number(params, "pageSize", 10);
        Integer status = legacyRefundStatus(params == null ? null : params.get("status"));
        String keyword = defaultText(params == null ? null : params.get("keyword"), null);
        IPage<TrRefundApply> page = pageRefunds(status, pageNo, pageSize);
        List<Map<String, Object>> rows = page.getRecords().stream().map(this::legacyRefundView)
                .filter(row -> !StringUtils.hasText(keyword)
                        || String.valueOf(row.getOrDefault("orderNo", "")).contains(keyword)
                        || String.valueOf(row.getOrDefault("userName", "")).contains(keyword))
                .toList();
        return pageView(rows.size() == page.getRecords().size() ? page.getTotal() : rows.size(), rows);
    }

    @Transactional
    public Map<String, Object> approveRefund(Map<String, ?> body) {
        Long id = longValue(body == null ? null : body.get("id"));
        if (id == null) id = longValue(body == null ? null : body.get("refundId"));
        TrRefundApply value = refundMapper.selectById(id);
        require(value != null, "退款记录不存在");
        Object decision = body == null ? null : body.get("status");
        if (decision == null && body != null) decision = body.get("approval");
        boolean approved = !("reject".equalsIgnoreCase(String.valueOf(decision))
                || "rejected".equalsIgnoreCase(String.valueOf(decision)) || "2".equals(String.valueOf(decision)));
        value.setStatus(approved ? 1 : 2);
        value.setAuditUserId(currentUserId());
        value.setAuditRemark(defaultText(body == null ? null : body.get("reason"),
                defaultText(body == null ? null : body.get("approvalOpinion"), value.getAuditRemark())));
        value.setAuditTime(LocalDateTime.now());
        if (approved) value.setRefundedTime(LocalDateTime.now());
        value.setUpdateTime(LocalDateTime.now());
        refundMapper.updateById(value);
        if (approved) {
            TrOrder order = orderMapper.selectById(value.getOrderId());
            if (order != null) {
                order.setOrderStatus(3); order.setPaymentStatus(3); order.setRefundTime(LocalDateTime.now());
                order.setRefundReason(value.getReason()); order.setUpdateTime(LocalDateTime.now()); orderMapper.updateById(order);
            }
        }
        return legacyRefundView(value);
    }

    public Map<String, Object> nextRefund() {
        TrRefundApply value = refundMapper.selectOne(new LambdaQueryWrapper<TrRefundApply>()
                .eq(TrRefundApply::getStatus, 0).orderByAsc(TrRefundApply::getCreateTime).last("limit 1"));
        return value == null ? Map.of() : legacyRefundView(value);
    }

    public Map<String, Object> legacyRefundViewForApi(Long id) {
        TrRefundApply value = refundMapper.selectById(id);
        if (value == null) throw new ServiceException("退款记录不存在");
        return legacyRefundView(value);
    }

    public Map<String, Object> statistics() {
        List<TrOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<>());
        BigDecimal totalAmount = orders.stream().map(TrOrder::getTotalAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingAmount = orders.stream().filter(item -> Integer.valueOf(0).equals(item.getOrderStatus()))
                .map(item -> defaultValue(item.getPayableAmount(), BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidAmount = orders.stream().filter(item -> Integer.valueOf(1).equals(item.getPaymentStatus()))
                .map(item -> defaultValue(item.getPaidAmount(), BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundAmount = refundMapper.selectList(new LambdaQueryWrapper<TrRefundApply>()).stream()
                .filter(item -> Integer.valueOf(1).equals(item.getStatus()) || Integer.valueOf(3).equals(item.getStatus()))
                .map(item -> defaultValue(item.getRefundAmount(), BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalOrders", orders.size());
        result.put("pendingOrders", orders.stream().filter(item -> Integer.valueOf(0).equals(item.getOrderStatus())).count());
        result.put("paidOrders", orders.stream().filter(item -> Integer.valueOf(1).equals(item.getPaymentStatus())).count());
        result.put("totalAmount", totalAmount); result.put("pendingAmount", pendingAmount);
        result.put("paidAmount", paidAmount); result.put("refundAmount", refundAmount);
        result.put("totalRevenue", paidAmount); result.put("totalCoupons", couponMapper.selectCount(new LambdaQueryWrapper<>()));
        result.put("refunds", refundMapper.selectCount(new LambdaQueryWrapper<>()));
        return result;
    }

    private Map<String, Object> couponView(MktCoupon item) { Map<String, Object> result = new LinkedHashMap<>(); result.put("id", item.getId()); result.put("name", item.getCouponName()); result.put("couponName", item.getCouponName()); result.put("type", item.getCouponType()); result.put("discountType", item.getDiscountType()); result.put("discountValue", cents(item.getDiscountValue())); result.put("thresholdAmount", cents(item.getThresholdAmount())); result.put("maxDiscountAmount", cents(item.getMaxDiscountAmount())); result.put("totalCount", item.getTotalCount()); result.put("receivedCount", item.getReceivedCount()); result.put("usedCount", item.getUsedCount()); result.put("startTime", item.getStartTime()); result.put("endTime", item.getEndTime()); result.put("termEndTime", item.getEndTime()); result.put("status", item.getStatus()); result.put("description", item.getDescription()); result.put("available", item.getTotalCount() == null || item.getTotalCount() == 0 || defaultValue(item.getReceivedCount(), 0) < item.getTotalCount()); result.put("received", false); return result; }

    private Map<String, Object> legacyCouponView(MktCoupon item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId()); result.put("name", item.getCouponName()); result.put("couponName", item.getCouponName());
        String type = item.getDiscountType() != null && item.getDiscountType() == 2 ? "percent"
                : item.getDiscountType() != null && item.getDiscountType() == 4 ? "fixed" : "direct";
        result.put("type", type);
        result.put("discountType", item.getDiscountType());
        result.put("value", "percent".equals(type) ? defaultValue(item.getDiscountValue(), BigDecimal.ZERO).multiply(BigDecimal.TEN).longValue()
                : cents(item.getDiscountValue()));
        result.put("discountValue", result.get("value"));
        result.put("minAmount", cents(item.getThresholdAmount())); result.put("thresholdAmount", cents(item.getThresholdAmount()));
        result.put("maxDiscountAmount", cents(item.getMaxDiscountAmount()));
        result.put("totalCount", item.getTotalCount()); result.put("receivedCount", item.getReceivedCount());
        result.put("usedCount", item.getUsedCount()); result.put("startTime", item.getStartTime()); result.put("endTime", item.getEndTime());
        result.put("termEndTime", item.getEndTime()); result.put("status", item.getStatus()); result.put("description", item.getDescription());
        result.put("available", item.getStatus() != null && item.getStatus() == 1
                && (item.getTotalCount() == null || item.getTotalCount() == 0
                || defaultValue(item.getReceivedCount(), 0) < item.getTotalCount()));
        return result;
    }

    private Map<String, Object> couponCodeView(MktCouponCode item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId()); result.put("couponId", item.getCouponId()); result.put("code", item.getCouponCode());
        result.put("couponCode", item.getCouponCode()); result.put("status", item.getStatus());
        result.put("exchangedUserId", item.getExchangedUserId()); result.put("exchangedTime", item.getExchangedTime());
        result.put("createTime", item.getCreateTime());
        MktCoupon coupon = couponMapper.selectById(item.getCouponId());
        if (coupon != null) result.put("couponName", coupon.getCouponName());
        return result;
    }

    private Map<String, Object> legacyOrderDetailView(Map<String, Object> order, Map<String, Object> detail,
            String keyword) {
        String orderNo = String.valueOf(order.getOrDefault("orderNo", ""));
        String userName = "用户" + order.getOrDefault("userId", "");
        String courseName = String.valueOf(detail.getOrDefault("courseName", detail.getOrDefault("name", "课程")));
        if (StringUtils.hasText(keyword) && !orderNo.contains(keyword) && !userName.contains(keyword)
                && !courseName.contains(keyword)) return null;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", detail.getOrDefault("id", order.get("id"))); result.put("orderId", order.get("id"));
        result.put("orderNo", orderNo); result.put("userId", order.get("userId")); result.put("userName", userName);
        result.put("phone", "-"); result.put("courseId", detail.get("courseId")); result.put("courseName", courseName);
        result.put("courseCover", detail.getOrDefault("cover", "/src/assets/images/courses/vue3.svg"));
        Object amount = detail.getOrDefault("price", order.getOrDefault("totalAmount", 0));
        Object payAmount = detail.getOrDefault("realPayAmount", order.getOrDefault("payableAmount", 0));
        result.put("orderAmount", moneyText(amount)); result.put("payAmount", moneyText(payAmount));
        result.put("payType", order.getOrDefault("paymentChannel", "-")); result.put("status", statusText(order.get("status")));
        result.put("statusCode", order.get("status")); result.put("createTime", order.get("createTime"));
        result.put("payTime", order.get("payTime")); result.put("refundTime", order.get("refundTime"));
        result.put("refundReason", order.get("refundReason"));
        return result;
    }

    private Map<String, Object> legacyRefundView(TrRefundApply item) {
        Map<String, Object> result = refundView(item);
        TrOrder order = orderMapper.selectById(item.getOrderId());
        result.put("orderNo", order == null ? "" : order.getOrderNo()); result.put("userId", item.getUserId());
        result.put("userName", "用户" + defaultValue(item.getUserId(), 0L)); result.put("courseName", "课程");
        result.put("courseCover", "/src/assets/images/courses/vue3.svg");
        result.put("payAmount", order == null ? moneyText(item.getRefundAmount()) : moneyText(order.getPayableAmount().movePointRight(2)));
        result.put("reason", item.getReason()); result.put("status", legacyRefundStatusText(item.getStatus()));
        result.put("applyTime", item.getCreateTime()); result.put("approver", item.getAuditUserId() == null ? null : "管理员");
        result.put("approveTime", item.getAuditTime()); result.put("rejectReason", item.getStatus() != null && item.getStatus() == 2 ? item.getAuditRemark() : null);
        if (order != null) {
            TrOrderItem detail = itemMapper.selectOne(new LambdaQueryWrapper<TrOrderItem>()
                    .eq(TrOrderItem::getOrderId, order.getId()).orderByAsc(TrOrderItem::getCreateTime).last("limit 1"));
            if (detail != null) {
                result.put("courseName", detail.getCourseName()); result.put("courseCover", detail.getCourseCoverUrl());
            }
        }
        return result;
    }

    private String moneyText(Object centsValue) {
        BigDecimal centsValueDecimal;
        try { centsValueDecimal = new BigDecimal(String.valueOf(defaultValue(centsValue, 0))); }
        catch (NumberFormatException ignored) { centsValueDecimal = BigDecimal.ZERO; }
        return centsValueDecimal.movePointLeft(2).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String statusText(Object value) {
        Integer status = value instanceof Number number ? number.intValue() : intValue(value);
        return switch (defaultValue(status, 1)) { case 1 -> "pending"; case 2, 4, 5 -> "paid"; case 3 -> "closed"; case 6 -> "refunded"; default -> "closed"; };
    }

    private String legacyRefundStatusText(Integer value) {
        return switch (defaultValue(value, 0)) { case 0 -> "pending"; case 1, 3 -> "approved"; case 2 -> "rejected"; default -> "pending"; };
    }

    private Integer legacyOrderStatus(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) return null;
        if (value instanceof Number number) return number.intValue();
        return switch (String.valueOf(value).toLowerCase()) {
            case "pending", "unpaid" -> 1; case "paid", "completed" -> 2;
            case "refunded" -> 6; case "closed", "cancelled" -> 3; default -> intValue(value);
        };
    }

    private Integer legacyRefundStatus(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) return null;
        if (value instanceof Number number) return number.intValue();
        return switch (String.valueOf(value).toLowerCase()) { case "pending" -> 0; case "approved" -> 1; case "rejected" -> 2; default -> intValue(value); };
    }

    private BigDecimal decimalValue(Object value, BigDecimal fallback) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) return fallback;
        try { return new BigDecimal(String.valueOf(value)); } catch (NumberFormatException ignored) { return fallback; }
    }

    private LocalDateTime dateTime(Object value, LocalDateTime fallback) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) return fallback;
        if (value instanceof LocalDateTime dateTime) return dateTime;
        String text = String.valueOf(value).trim();
        try { return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME); }
        catch (Exception ignored) {
            try { return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(); }
            catch (Exception ignoredAgain) { return fallback; }
        }
    }

    private int intValue(Object value, int fallback) { Long parsed = longValue(value); return parsed == null ? fallback : parsed.intValue(); }
    private Map<String, Object> userCouponView(MktUserCoupon item) { Map<String, Object> result = couponView(Optional.ofNullable(couponMapper.selectById(item.getCouponId())).orElseGet(MktCoupon::new)); result.put("id", item.getId()); result.put("couponId", item.getCouponId()); result.put("status", item.getStatus() == 0 ? 1 : item.getStatus() == 1 ? 2 : 3); result.put("receivedAt", item.getReceivedAt()); result.put("expireAt", item.getExpireAt()); result.put("usedAt", item.getUsedAt()); result.put("receiveCode", item.getReceiveCode()); return result; }
    private Map<String, Object> cartView(TrCart item) {
        Map<String, Object> snapshot = courseSnapshot(item.getCourseId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId()); result.put("courseId", item.getCourseId()); result.put("quantity", item.getQuantity());
        result.put("selected", item.getSelected()); result.put("checked", Integer.valueOf(1).equals(item.getSelected()));
        result.put("courseName", defaultText(item.getCourseName(), defaultText(snapshot.get("title"), defaultText(snapshot.get("courseName"), "课程 " + item.getCourseId()))));
        result.put("teacherName", defaultText(item.getTeacherName(), defaultText(snapshot.get("teacherName"), "讲师团队")));
        result.put("cover", defaultText(item.getCourseCoverUrl(), defaultText(snapshot.get("cover"), defaultText(snapshot.get("coverUrl"), ""))));
        long price = item.getUnitPrice() == null ? number(snapshot, "price", defaultPriceCents(item.getCourseId())) : cents(item.getUnitPrice()).longValue();
        result.put("price", price); result.put("originalPrice", number(snapshot, "originalPrice", price)); return result;
    }

    private void fillCartSnapshot(TrCart cart, Map<String, Object> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) return;
        cart.setCourseName(defaultText(snapshot.get("title"), defaultText(snapshot.get("courseName"), null)));
        cart.setCourseCoverUrl(defaultText(snapshot.get("cover"), defaultText(snapshot.get("coverUrl"), null)));
        cart.setTeacherName(defaultText(snapshot.get("teacherName"), null));
        Long priceCents = longValue(snapshot.get("price"));
        if (priceCents != null) cart.setUnitPrice(BigDecimal.valueOf(priceCents).movePointLeft(2));
    }

    /** 课程信息只经由教育服务读取，服务不可用时由购物车已保存的快照兜底。 */
    private Map<String, Object> courseSnapshot(Long courseId) {
        if (courseId == null || educationService == null) return Map.of();
        try {
            AjaxResult response = educationService.getCourse(courseId);
            if (response != null && Integer.valueOf(200).equals(response.get("code")) && response.get("data") instanceof Map<?, ?> map) {
                return castMap(map);
            }
        } catch (Exception ignored) {
            // Feign fallback 或网络异常时返回空快照，调用方继续使用本地快照/默认值。
        }
        return Map.of();
    }

    private void enrollPurchasedCourses(TrOrder order) {
        if (order == null || educationService == null) return;
        itemMapper.selectList(new LambdaQueryWrapper<TrOrderItem>().eq(TrOrderItem::getOrderId, order.getId()))
                .stream().map(TrOrderItem::getCourseId).filter(Objects::nonNull).distinct().forEach(courseId -> {
                    try { educationService.enroll(courseId); } catch (Exception ignored) { /* 订单已成功，学习服务稍后可重试 */ }
                });
    }
    private Map<String, Object> orderView(TrOrder item) { Map<String, Object> result = new LinkedHashMap<>(); result.put("id", item.getId()); result.put("orderNo", item.getOrderNo()); result.put("userId", item.getUserId()); result.put("totalAmount", cents(item.getTotalAmount())); result.put("discountAmount", cents(item.getDiscountAmount())); result.put("realAmount", cents(item.getPayableAmount())); result.put("payableAmount", cents(item.getPayableAmount())); result.put("paidAmount", cents(item.getPaidAmount())); result.put("couponId", item.getCouponId()); result.put("status", toOldOrderStatus(item.getOrderStatus())); result.put("statusName", oldStatusName(toOldOrderStatus(item.getOrderStatus()))); result.put("paymentStatus", item.getPaymentStatus()); result.put("paymentChannel", item.getPaymentChannel()); result.put("createTime", item.getCreateTime()); result.put("payTime", item.getPaidTime()); result.put("expireTime", item.getExpireTime()); result.put("refundTime", item.getRefundTime()); result.put("refundReason", item.getRefundReason()); List<Map<String, Object>> details = itemMapper.selectList(new LambdaQueryWrapper<TrOrderItem>().eq(TrOrderItem::getOrderId, item.getId())).stream().map(detail -> { Map<String, Object> row = new LinkedHashMap<>(); row.put("id", detail.getId()); row.put("courseId", detail.getCourseId()); row.put("name", detail.getCourseName()); row.put("courseName", detail.getCourseName()); row.put("cover", detail.getCourseCoverUrl()); row.put("price", cents(detail.getUnitPrice())); row.put("realPayAmount", cents(detail.getPayableAmount())); row.put("canRefund", item.getPaymentStatus() == 1 && item.getOrderStatus() == 1); row.put("refundStatus", null); return row; }).toList(); result.put("details", details); result.put("couponRule", item.getCouponId() == null ? List.of() : List.of("已使用优惠券")); result.put("message", oldStatusName(toOldOrderStatus(item.getOrderStatus()))); result.put("progressNodes", List.of()); return result; }
    private Map<String, Object> refundView(TrRefundApply item) { Map<String, Object> result = new LinkedHashMap<>(); result.put("id", item.getId()); result.put("refundOrderNo", item.getRefundNo()); result.put("orderId", item.getOrderId()); result.put("refundReason", item.getReason()); result.put("refundChannel", "原支付渠道"); result.put("refundAmount", cents(item.getRefundAmount())); result.put("status", item.getStatus()); result.put("remark", item.getStatus() == 1 || item.getStatus() == 3); result.put("approvalOpinion", item.getAuditRemark()); result.put("createTime", item.getCreateTime()); result.put("approveTime", item.getAuditTime()); result.put("refundedTime", item.getRefundedTime()); return result; }
    private TrOrder findOrder(Long id) { TrOrder value = id == null ? null : orderMapper.selectById(id); if (value == null && id != null) value = orderMapper.selectOne(new LambdaQueryWrapper<TrOrder>().eq(TrOrder::getOrderNo, String.valueOf(id))); if (value == null) throw new ServiceException("订单不存在"); return value; }
    private BigDecimal discount(BigDecimal total, Long couponId) { if (couponId == null) return BigDecimal.ZERO; MktCoupon coupon = couponMapper.selectById(couponId); if (coupon == null) return BigDecimal.ZERO; BigDecimal threshold = defaultValue(coupon.getThresholdAmount(), BigDecimal.ZERO); if (total.compareTo(threshold) < 0) return BigDecimal.ZERO; BigDecimal value = defaultValue(coupon.getDiscountValue(), BigDecimal.ZERO); BigDecimal discount; if (coupon.getDiscountType() != null && coupon.getDiscountType() == 2) discount = total.multiply(BigDecimal.ONE.subtract(value.divide(BigDecimal.TEN, 4, RoundingMode.HALF_UP))); else discount = value; if (coupon.getMaxDiscountAmount() != null) discount = discount.min(coupon.getMaxDiscountAmount()); return discount.max(BigDecimal.ZERO).min(total); }
    private int toDbOrderStatus(int old) { return switch (old) { case 1 -> 0; case 2, 4, 5 -> 1; case 3 -> 4; case 6 -> 3; default -> old; }; }
    private int toOldOrderStatus(Integer db) { return db == null ? 1 : switch (db) { case 0 -> 1; case 1 -> 4; case 3 -> 6; case 4 -> 3; default -> 3; }; }
    private String oldStatusName(int value) { return switch (value) { case 1 -> "待支付"; case 2 -> "已支付"; case 3 -> "已关闭"; case 4 -> "已完成"; case 5 -> "已报名"; case 6 -> "已退款"; default -> "未知"; }; }
    private long defaultPriceCents(Long courseId) { return switch (String.valueOf(courseId)) { case "1" -> 19900; case "2" -> 24900; case "3" -> 14900; case "4" -> 29900; case "5" -> 22900; case "6" -> 18900; case "7" -> 26900; case "8" -> 16900; case "9" -> 15900; case "10" -> 34900; default -> 19900; }; }
    private BigDecimal cents(BigDecimal value) { return defaultValue(value, BigDecimal.ZERO).movePointRight(2).setScale(0, RoundingMode.HALF_UP); }
    private Map<String, Object> pageView(long total, List<?> list) { Map<String, Object> result = new LinkedHashMap<>(); result.put("total", total); result.put("list", list); return result; }
    private long number(Map<String, ?> map, String key, long fallback) { Object value = map == null ? null : map.get(key); if (value == null) return fallback; try { return Long.parseLong(String.valueOf(value)); } catch (NumberFormatException ignored) { return fallback; } }
    private Long longValue(Object value) { if (value == null || !StringUtils.hasText(String.valueOf(value))) return null; try { return Long.valueOf(String.valueOf(value)); } catch (NumberFormatException ignored) { return null; } }
    private List<Long> longValues(Object value) {
        if (value instanceof List<?> list) return list.stream().map(this::longValue).filter(Objects::nonNull).toList();
        if (value == null || !StringUtils.hasText(String.valueOf(value))) return List.of();
        return Arrays.stream(String.valueOf(value).split(",|，|、")).map(this::longValue).filter(Objects::nonNull).toList();
    }
    private Integer intValue(Object value) { Long valueLong = longValue(value); return valueLong == null ? null : valueLong.intValue(); }
    private boolean bool(Object value) { return value instanceof Boolean b ? b : "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value)); }
    private String defaultText(Object value, String fallback) { return value == null || !StringUtils.hasText(String.valueOf(value)) ? fallback : String.valueOf(value); }
    private void require(boolean condition, String message) { if (!condition) throw new ServiceException(message); }
    private Long currentUserId() { Long value = SecurityUtils.getUserId(); return value == null || value < 1 ? 1L : value; }
    private long newId() { return IdWorker.getId(); }
    private long safePage(long value) { return value < 1 ? 1 : Math.min(value, 100000); }
    private long safeSize(long value) { return value < 1 ? 10 : Math.min(value, 200); }
    private <T> T defaultValue(T value, T fallback) { return value == null ? fallback : value; }
    @SuppressWarnings("unchecked") private Map<String, Object> castMap(Map<?, ?> value) { Map<String, Object> result = new LinkedHashMap<>(); value.forEach((key, item) -> result.put(String.valueOf(key), item)); return result; }
}
