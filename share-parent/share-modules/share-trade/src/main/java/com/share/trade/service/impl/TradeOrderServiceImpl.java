package com.share.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.redis.service.RedisService;
import com.share.education.api.RemoteEducationService;
import com.share.trade.domain.*;
import com.share.trade.mapper.*;
import com.share.trade.service.ITradeCouponService;
import com.share.trade.service.ITradeOrderService;
import com.share.trade.service.support.TradeUtils;
import com.alibaba.fastjson2.JSON;
import com.share.trade.mq.RocketMQTopicConstants;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.share.trade.service.support.TradeUtils.*;

/**
 * 购物车与订单领域服务实现类
 */
@Service
@Primary
public class TradeOrderServiceImpl implements ITradeOrderService {

    private static final Logger log = LoggerFactory.getLogger(TradeOrderServiceImpl.class);

    private final TrCartMapper cartMapper;
    private final TrOrderMapper orderMapper;
    private final TrOrderItemMapper itemMapper;
    private final TrRefundApplyMapper refundMapper;
    private final MktCouponMapper couponMapper;
    private final MktUserCouponMapper userCouponMapper;
    private final RemoteEducationService educationService;
    private final RedisService redisService;
    private final Environment environment;
    private final ITradeCouponService couponService;

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    public TradeOrderServiceImpl(TrCartMapper cartMapper,
                                 TrOrderMapper orderMapper,
                                 TrOrderItemMapper itemMapper,
                                 TrRefundApplyMapper refundMapper,
                                 MktCouponMapper couponMapper,
                                 MktUserCouponMapper userCouponMapper,
                                 RemoteEducationService educationService,
                                 RedisService redisService,
                                 Environment environment,
                                 @Qualifier("tradeCouponServiceImpl") ITradeCouponService couponService) {
        this.cartMapper = cartMapper;
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.refundMapper = refundMapper;
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
        this.educationService = educationService;
        this.redisService = redisService;
        this.environment = environment;
        this.couponService = couponService;
    }

    @Override
    public List<Map<String, Object>> carts() {
        return cartMapper.selectList(new LambdaQueryWrapper<TrCart>()
                .eq(TrCart::getUserId, currentUserId())
                .orderByDesc(TrCart::getCreateTime))
                .stream().map(this::cartView).toList();
    }

    @Override
    @Transactional
    public Map<String, Object> addCart(Map<String, ?> body) {
        Long courseId = longValue(body == null ? null : body.get("courseId"));
        require(courseId != null, "课程编号不能为空");
        Map<String, Object> snapshot = courseSnapshot(courseId);
        require(snapshot != null && !snapshot.isEmpty(), "课程不存在");
        if (snapshot.containsKey("status")) {
            require(Objects.equals(snapshot.get("status"), 1) || Objects.equals(snapshot.get("status"), "1"), "该课程已下架或暂未开放购买");
        }
        TrCart value = cartMapper.selectOne(new LambdaQueryWrapper<TrCart>()
                .eq(TrCart::getUserId, currentUserId())
                .eq(TrCart::getCourseId, courseId));
        LocalDateTime now = LocalDateTime.now();
        if (value == null) {
            value = new TrCart();
            value.setId(newId());
            value.setUserId(currentUserId());
            value.setCourseId(courseId);
            value.setQuantity(1);
            value.setCreateTime(now);
            value.setUpdateTime(now);
            fillCartSnapshot(value, snapshot);
            cartMapper.insert(value);
        } else {
            // 在线课程为虚拟数字商品，单用户限购 1 份，不可重复累加数量
            value.setQuantity(1);
            value.setUpdateTime(now);
            fillCartSnapshot(value, snapshot);
            cartMapper.updateById(value);
        }
        return cartView(value);
    }

    @Override
    @Transactional
    public void removeCarts(String ids) {
        List<Long> cartIds = longValues(ids);
        if (!cartIds.isEmpty()) {
            cartMapper.delete(new LambdaQueryWrapper<TrCart>()
                    .eq(TrCart::getUserId, currentUserId())
                    .in(TrCart::getId, cartIds));
        }
    }

    @Override
    public Map<String, Object> listOrders(Map<String, ?> params, boolean admin) {
        long pageNo = number(params, "pageNo", number(params, "pageNum", 1));
        long pageSize = number(params, "pageSize", 10);
        String orderNo = defaultText(params == null ? null : params.get("orderNo"), null);
        String keyword = defaultText(params == null ? null : params.get("keyword"), null);
        Integer status = intValue(params == null ? null : params.get("status"));
        Long userId = admin ? longValue(params == null ? null : params.get("userId")) : currentUserId();

        // 高性能优化：当无关键词深层扫描时，直接走数据库原生 LIMIT 分页，杜绝全表拉取与 N+1 循环
        if (!StringUtils.hasText(keyword)) {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<TrOrder> page =
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(safePage(pageNo), safeSize(pageSize));
            LambdaQueryWrapper<TrOrder> q = new LambdaQueryWrapper<TrOrder>()
                    .eq(userId != null, TrOrder::getUserId, userId)
                    .eq(StringUtils.hasText(orderNo), TrOrder::getOrderNo, orderNo)
                    .orderByDesc(TrOrder::getCreateTime);
            if (status != null) {
                Integer newStatus = status == 5 ? 4 : status;
                q.eq(TrOrder::getOrderStatus, newStatus);
            }
            orderMapper.selectPage(page, q);
            for (TrOrder order : page.getRecords()) {
                checkAndExpireOrder(order);
            }
            List<Map<String, Object>> pagedRows = page.getRecords().stream().map(this::orderView).toList();
            return pageView(page.getTotal(), pagedRows);
        }

        List<TrOrder> rows = orderMapper.selectList(new LambdaQueryWrapper<TrOrder>()
                .eq(userId != null, TrOrder::getUserId, userId)
                .eq(StringUtils.hasText(orderNo), TrOrder::getOrderNo, orderNo)
                .orderByDesc(TrOrder::getCreateTime));

        List<TrOrder> filtered = new ArrayList<>();
        for (TrOrder order : rows) {
            checkAndExpireOrder(order);
            if (status != null && !Objects.equals(toOldOrderStatus(order.getOrderStatus()), status)) {
                continue;
            }
            if (StringUtils.hasText(keyword) && !order.getOrderNo().contains(keyword)) {
                List<TrOrderItem> details = itemMapper.selectList(new LambdaQueryWrapper<TrOrderItem>()
                        .eq(TrOrderItem::getOrderId, order.getId()));
                boolean matched = details.stream().anyMatch(item -> item.getCourseName() != null && item.getCourseName().contains(keyword));
                if (!matched) continue;
            }
            filtered.add(order);
        }

        int fromIndex = (int) Math.min((safePage(pageNo) - 1) * safeSize(pageSize), filtered.size());
        int toIndex = (int) Math.min(fromIndex + safeSize(pageSize), filtered.size());
        List<Map<String, Object>> pagedRows = filtered.subList(fromIndex, toIndex).stream().map(this::orderView).toList();
        return pageView(filtered.size(), pagedRows);
    }

    @Override
    public String createOrderToken() {
        Long userId = currentUserId();
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = "trade:order:token:" + (userId != null ? userId : 0L) + ":" + token;
        redisService.setCacheObject(key, "1", 30L, TimeUnit.MINUTES);
        return token;
    }

    @Override
    public Map<String, Object> order(Long id) {
        return orderView(findOrder(id));
    }

    @Override
    public Map<String, Object> prePlaceOrder(Map<String, ?> params) {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> available = new ArrayList<>();

        List<MktUserCoupon> myCoupons = userCouponMapper.selectList(new LambdaQueryWrapper<MktUserCoupon>()
                .eq(MktUserCoupon::getUserId, currentUserId())
                .eq(MktUserCoupon::getStatus, 0)
                .and(w -> w.isNull(MktUserCoupon::getExpireAt).or().ge(MktUserCoupon::getExpireAt, now))
                .orderByDesc(MktUserCoupon::getReceivedAt));
        for (MktUserCoupon uc : myCoupons) {
            MktCoupon c = couponMapper.selectById(uc.getCouponId());
            if (c != null && Integer.valueOf(1).equals(c.getStatus())) {
                Map<String, Object> cv = couponService.couponView(c);
                cv.put("id", uc.getId());
                cv.put("userCouponId", uc.getId());
                cv.put("couponId", c.getId());
                available.add(cv);
            }
        }
        if (available.isEmpty()) {
            available.addAll(couponService.collectableCoupons());
        }
        result.put("availableCoupons", available);
        result.put("coupons", available);

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
            directItem.put("id", null);
            directItem.put("courseId", directCourseId);
            directItem.put("quantity", 1);
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

    @Override
    @Transactional
    public Map<String, Object> placeOrder(Map<String, ?> body) {
        // 幂等防重 Token 校验 (防止网络抖动或连击产生重复订单)
        Object orderTokenObj = body == null ? null : body.get("orderToken");
        if (orderTokenObj != null && StringUtils.hasText(String.valueOf(orderTokenObj))) {
            Long currentUid = currentUserId();
            String key = "trade:order:token:" + (currentUid != null ? currentUid : 0L) + ":" + String.valueOf(orderTokenObj).trim();
            boolean deleted = Boolean.TRUE.equals(redisService.deleteObject(key));
            if (!deleted) {
                throw new ServiceException("订单正在提交或令牌已失效，请勿重复提交");
            }
        }

        List<Map<String, ?>> sourceItems = new ArrayList<>();
        boolean fromCart = false;
        Object itemObject = body == null ? null : body.get("items");
        if (itemObject instanceof List<?> list) {
            for (Object item : list) if (item instanceof Map<?, ?> map) sourceItems.add(castMap(map));
        }
        if (sourceItems.isEmpty()) {
            Long courseId = longValue(body == null ? null : body.get("courseId"));
            if (courseId != null) {
                Map<String, Object> one = new LinkedHashMap<>();
                one.put("courseId", courseId);
                one.putAll(body);
                sourceItems.add(one);
            }
            List<Long> cIds = longValues(body == null ? null : body.get("courseIds"));
            for (Long cId : cIds) {
                if (!Objects.equals(cId, courseId)) {
                    Map<String, Object> one = new LinkedHashMap<>();
                    one.put("courseId", cId);
                    sourceItems.add(one);
                }
            }
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
            Long courseId = longValue(source.get("courseId"));
            require(courseId != null, "订单课程编号不能为空");
            Map<String, Object> snapshot = courseSnapshot(courseId);
            require(snapshot != null && !snapshot.isEmpty(), "课程不存在");
            // 安全防御：严格禁止客户端伪造 isInternalSeckill 实施 0 元购绕过 (BUG-FIX)
            boolean isCourseFree = bool(snapshot.get("free")) || bool(snapshot.get("isFree"))
                    || (snapshot.containsKey("price") && number(snapshot, "price", -1) == 0);
            long cents = isCourseFree ? 0 : number(snapshot, "price", defaultPriceCents(courseId));
            BigDecimal amount = BigDecimal.valueOf(cents).movePointLeft(2);
            String courseName = isCourseFree ? defaultText(snapshot.get("title"), defaultText(snapshot.get("courseName"), "免费课程 " + courseId))
                    : defaultText(snapshot.get("title"), defaultText(snapshot.get("courseName"), "课程 " + courseId));
            String cover = defaultText(snapshot.get("cover"), defaultText(snapshot.get("coverUrl"), defaultText(source.get("cover"), null)));
            TrOrderItem item = new TrOrderItem();
            item.setId(newId());
            item.setCourseId(courseId);
            item.setCourseName(courseName);
            item.setCourseCoverUrl(cover);
            item.setUnitPrice(amount);
            // 在线虚拟课程每门购买数量固定为 1
            item.setQuantity(1);
            item.setDiscountAmount(BigDecimal.ZERO);
            item.setPayableAmount(amount);
            item.setCreateTime(LocalDateTime.now());
            items.add(item);
            total = total.add(item.getPayableAmount());
        }

        Long couponParam = longValue(body == null ? null : body.get("couponId"));
        MktUserCoupon targetUserCoupon = null;
        MktCoupon coupon = null;
        LocalDateTime now = LocalDateTime.now();
        if (couponParam != null) {
            targetUserCoupon = userCouponMapper.selectOne(new LambdaQueryWrapper<MktUserCoupon>()
                    .eq(MktUserCoupon::getId, couponParam)
                    .eq(MktUserCoupon::getUserId, currentUserId())
                    .eq(MktUserCoupon::getStatus, 0));
            if (targetUserCoupon == null) {
                targetUserCoupon = userCouponMapper.selectOne(new LambdaQueryWrapper<MktUserCoupon>()
                        .eq(MktUserCoupon::getCouponId, couponParam)
                        .eq(MktUserCoupon::getUserId, currentUserId())
                        .eq(MktUserCoupon::getStatus, 0)
                        .orderByDesc(MktUserCoupon::getReceivedAt)
                        .last("limit 1"));
            }
            if (targetUserCoupon == null) {
                throw new ServiceException("未找到您名下可用的有效优惠券，或该优惠券已被使用");
            }
            coupon = couponMapper.selectById(targetUserCoupon.getCouponId());
            require(coupon != null && Integer.valueOf(1).equals(coupon.getStatus()), "该优惠券已下架或失效");
        }

        BigDecimal discountAmount = couponService.discount(total, coupon);
        BigDecimal payable = total.subtract(discountAmount).max(BigDecimal.ZERO);

        TrOrder order = new TrOrder();
        order.setId(newId());
        order.setOrderNo(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(now) + String.format("%04d", (int) (Math.random() * 10000)));
        order.setUserId(currentUserId());
        order.setTotalAmount(total);
        order.setDiscountAmount(discountAmount);
        order.setPayableAmount(payable);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setCouponId(coupon == null ? null : coupon.getId());
        order.setPaymentChannel("mock");
        order.setOrderStatus(0);
        order.setPaymentStatus(0);
        order.setExpireTime(now.plusMinutes(15));
        order.setCreateTime(now);
        order.setUpdateTime(now);
        order.setDelFlag(0);
        order.setVersion(0);
        orderMapper.insert(order);

        for (TrOrderItem item : items) {
            item.setOrderId(order.getId());
            itemMapper.insert(item);
        }

        if (fromCart) {
            List<Long> cartIds = sourceItems.stream().map(item -> longValue(item.get("id"))).filter(Objects::nonNull).toList();
            if (!cartIds.isEmpty()) {
                cartMapper.delete(new LambdaQueryWrapper<TrCart>()
                        .eq(TrCart::getUserId, currentUserId())
                        .in(TrCart::getId, cartIds));
            }
        }

        if (coupon != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (targetUserCoupon != null) {
                targetUserCoupon.setStatus(1);
                targetUserCoupon.setUsedOrderId(order.getId());
                targetUserCoupon.setUsedAt(now);
                targetUserCoupon.setUpdateTime(now);
                userCouponMapper.updateById(targetUserCoupon);
            } else {
                MktUserCoupon autoCreated = new MktUserCoupon();
                autoCreated.setId(newId());
                autoCreated.setUserId(currentUserId());
                autoCreated.setCouponId(coupon.getId());
                autoCreated.setSourceType("direct_order");
                autoCreated.setStatus(1);
                autoCreated.setUsedOrderId(order.getId());
                autoCreated.setReceivedAt(now);
                autoCreated.setUsedAt(now);
                autoCreated.setExpireAt(coupon.getEndTime());
                autoCreated.setCreateTime(now);
                autoCreated.setUpdateTime(now);
                autoCreated.setDelFlag(0);
                autoCreated.setVersion(0);
                userCouponMapper.insert(autoCreated);
            }
            couponMapper.update(null, new LambdaUpdateWrapper<MktCoupon>()
                    .setSql("used_count = COALESCE(used_count, 0) + 1")
                    .eq(MktCoupon::getId, coupon.getId()));
        }

        // 发送 15 分钟超时关单延时消息至 RocketMQ
        sendOrderTimeoutDelayMessage(order.getId(), order.getUserId(), body);

        return orderView(order);
    }

    private void sendOrderTimeoutDelayMessage(Long orderId, Long userId, Map<String, ?> body) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQTemplate not available, skipping delay message for orderId={}", orderId);
            return;
        }
        try {
            boolean quickTimeout = body != null && bool(body.get("quickTimeout"));
            // 关单延时级别：默认15对应自定义15分钟；单元与联调测试传入 quickTimeout=true 时使用级别2 (5秒)
            int cancelDelayLevel = quickTimeout ? 2 : 15;
            // 催付延时级别：默认14对应自定义10分钟（提前5分钟催付）；测试模式传入 quickTimeout=true 时使用级别1 (1秒)
            int remindDelayLevel = quickTimeout ? 1 : 14;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("orderId", orderId);
            payload.put("userId", userId);
            payload.put("delayLevel", cancelDelayLevel);
            payload.put("createTime", System.currentTimeMillis());

            // 1. 发送 15 分钟超时关单消息
            rocketMQTemplate.syncSend(
                RocketMQTopicConstants.TRADE_ORDER_TIMEOUT_TOPIC,
                MessageBuilder.withPayload(JSON.toJSONString(payload)).build(),
                3000,
                cancelDelayLevel
            );
            log.info("【RocketMQ】成功发送订单超时延时关单消息, orderId={}, delayLevel={}", orderId, cancelDelayLevel);

            // 2. 发送超时前 5 分钟催付提醒延时消息
            Map<String, Object> remindPayload = new LinkedHashMap<>(payload);
            remindPayload.put("remindDelayLevel", remindDelayLevel);
            rocketMQTemplate.syncSend(
                RocketMQTopicConstants.TRADE_ORDER_EXPIRING_REMINDER_TOPIC,
                MessageBuilder.withPayload(JSON.toJSONString(remindPayload)).build(),
                3000,
                remindDelayLevel
            );
            log.info("【RocketMQ】成功发送订单超时前催付提醒延时消息, orderId={}, delayLevel={}", orderId, remindDelayLevel);
        } catch (Exception e) {
            log.error("【RocketMQ】发送订单延时消息失败, orderId={}", orderId, e);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> freeCourse(Long courseId) {
        require(courseId != null, "课程编号不能为空");
        Map<String, Object> direct = new LinkedHashMap<>();
        direct.put("courseId", courseId);
        direct.put("isInternalSeckill", true);
        direct.put("price", 0);
        Map<String, Object> result = placeOrder(Map.of("items", List.of(direct)));
        Long orderId = longValue(result.get("id"));
        TrOrder order = orderMapper.selectById(orderId);
        if (order != null) {
            order.setOrderStatus(1);
            order.setPaymentStatus(1);
            order.setPaidAmount(BigDecimal.ZERO);
            order.setPaidTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
            enrollPurchasedCourses(order);
            return orderView(order);
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> seckillCourse(Long courseId) {
        require(courseId != null, "秒杀课程编号不能为空");
        Long userId = currentUserId();
        String userKey = "trade:seckill:course:users:" + courseId;
        String stockKey = "trade:seckill:course:stock:" + courseId;

        if (Boolean.TRUE.equals(redisService.sIsMember(userKey, String.valueOf(userId)))) {
            TrOrder existing = orderMapper.selectOne(new LambdaQueryWrapper<TrOrder>()
                    .eq(TrOrder::getUserId, userId)
                    .in(TrOrder::getOrderStatus, Arrays.asList(0, 1))
                    .orderByDesc(TrOrder::getCreateTime).last("limit 1"));
            if (existing != null) return orderView(existing);
        }

        if (redisService.getCacheObject(stockKey) == null) {
            redisService.setCacheObjectIfAbsent(stockKey, 50, 24L, TimeUnit.HOURS);
        }
        long remaining = redisService.decrement(stockKey);
        if (remaining < 0) {
            redisService.decrement(stockKey, -1);
            throw new ServiceException("秒杀名额已抢光，下次早点来哦！");
        }

        try {
            Map<String, Object> direct = new LinkedHashMap<>();
            direct.put("courseId", courseId);
            direct.put("isInternalSeckill", true);
            direct.put("courseName", "⚡限时秒杀·智问精品课 #" + courseId);
            direct.put("price", 990);
            direct.put("originalPrice", defaultPriceCents(courseId));

            Map<String, Object> result = placeOrder(Map.of("items", List.of(direct)));
            redisService.sAdd(userKey, String.valueOf(userId));
            return result;
        } catch (Exception e) {
            redisService.decrement(stockKey, -1);
            throw e;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> cancelOrder(Long id) {
        TrOrder order = findOrder(id);
        require(order.getOrderStatus() == 0, "当前订单状态不可取消");
        order.setOrderStatus(4);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        restoreOrderCoupon(order);
        return orderView(order);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        TrOrder order = findOrder(id);
        require(order.getOrderStatus() == 4 || order.getOrderStatus() == 3, "只能删除已关闭或已退款的订单");
        order.setDelFlag(1);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
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
            } else {
                for (Object detailObject : details) {
                    Map<String, Object> detail = detailObject instanceof Map<?, ?> detailMap ? castMap(detailMap) : Map.<String, Object>of();
                    Map<String, Object> row = legacyOrderDetailView(order, detail, keyword);
                    if (row != null) rows.add(row);
                }
            }
        }
        return pageView(rows.size(), rows);
    }

    @Override
    public Map<String, Object> legacyOrderDetail(Long id) {
        Map<String, Object> order = order(id);
        List<?> details = order.get("details") instanceof List<?> list ? list : List.of();
        Map<String, Object> detail = details.isEmpty() ? Map.<String, Object>of() : castMap((Map<?, ?>) details.get(0));
        return legacyOrderDetailView(order, detail, null);
    }

    @Override
    public TrOrder findOrder(Long id) {
        require(id != null, "订单编号不能为空");
        TrOrder value = orderMapper.selectById(id);
        require(value != null && (value.getDelFlag() == null || value.getDelFlag() == 0), "订单不存在");
        return value;
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, Integer status) {
        TrOrder order = findOrder(orderId);
        order.setOrderStatus(status);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    public void enrollPurchasedCourses(TrOrder order) {
        if (order == null || educationService == null) return;
        Long userId = order.getUserId();
        itemMapper.selectList(new LambdaQueryWrapper<TrOrderItem>().eq(TrOrderItem::getOrderId, order.getId()))
                .stream().map(TrOrderItem::getCourseId).filter(Objects::nonNull).distinct().forEach(courseId -> {
                    try { educationService.enroll(courseId, userId, SecurityConstants.INNER); } catch (Exception ignored) {}
                });
    }

    @Override
    public void revokePurchasedCourses(TrOrder order) {
        if (order == null || educationService == null) return;
        Long userId = order.getUserId();
        itemMapper.selectList(new LambdaQueryWrapper<TrOrderItem>().eq(TrOrderItem::getOrderId, order.getId()))
                .stream().map(TrOrderItem::getCourseId).filter(Objects::nonNull).distinct().forEach(courseId -> {
                    try { educationService.revokeEnrollment(courseId, userId, SecurityConstants.INNER); } catch (Exception ignored) {}
                });
    }

    @Override
    public Map<String, Object> courseSnapshot(Long courseId) {
        if (courseId == null || educationService == null) return Map.of();
        try {
            AjaxResult response = educationService.getCourse(courseId);
            if (response != null && Integer.valueOf(200).equals(response.get("code")) && response.get("data") instanceof Map<?, ?> map) {
                return castMap(map);
            }
        } catch (Exception ignored) {}
        return Map.of();
    }

    private void fillCartSnapshot(TrCart cart, Map<String, Object> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) return;
        cart.setCourseName(defaultText(snapshot.get("title"), defaultText(snapshot.get("courseName"), null)));
        cart.setCourseCoverUrl(defaultText(snapshot.get("cover"), defaultText(snapshot.get("coverUrl"), null)));
        cart.setTeacherName(defaultText(snapshot.get("teacherName"), null));
        Long priceCents = longValue(snapshot.get("price"));
        if (priceCents != null) cart.setUnitPrice(BigDecimal.valueOf(priceCents).movePointLeft(2));
    }

    private void checkAndExpireOrder(TrOrder order) {
        if (order == null || order.getOrderStatus() != 0 || order.getExpireTime() == null) return;
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(order.getExpireTime())) {
            order.setOrderStatus(4);
            order.setUpdateTime(now);
            orderMapper.updateById(order);
            restoreOrderCoupon(order);
        }
    }

    private void restoreOrderCoupon(TrOrder order) {
        if (order == null || order.getId() == null) return;
        List<MktUserCoupon> usedCoupons = userCouponMapper.selectList(new LambdaQueryWrapper<MktUserCoupon>()
                .eq(MktUserCoupon::getUsedOrderId, order.getId()));
        LocalDateTime now = LocalDateTime.now();
        for (MktUserCoupon uc : usedCoupons) {
            if (uc.getStatus() != null && uc.getStatus() == 1) {
                if ("direct_order".equals(uc.getSourceType())) {
                    uc.setStatus(2);
                } else {
                    int restoredStatus = (uc.getExpireAt() != null && now.isAfter(uc.getExpireAt())) ? 2 : 0;
                    uc.setStatus(restoredStatus);
                }
                uc.setUsedAt(null);
                uc.setUsedOrderId(null);
                uc.setUpdateTime(now);
                userCouponMapper.updateById(uc);
            }
        }
    }

    @Override
    public Map<String, Object> orderView(TrOrder item) {
        checkAndExpireOrder(item);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("orderNo", item.getOrderNo());
        result.put("userId", item.getUserId());
        result.put("totalAmount", cents(item.getTotalAmount()));
        result.put("discountAmount", cents(item.getDiscountAmount()));
        result.put("realAmount", cents(item.getPayableAmount()));
        result.put("payableAmount", cents(item.getPayableAmount()));
        result.put("paidAmount", cents(item.getPaidAmount()));
        result.put("couponId", item.getCouponId());
        result.put("status", toOldOrderStatus(item.getOrderStatus()));
        result.put("orderStatus", item.getOrderStatus());
        result.put("statusName", oldStatusName(toOldOrderStatus(item.getOrderStatus())));
        result.put("paymentStatus", item.getPaymentStatus());
        result.put("paymentChannel", item.getPaymentChannel());
        result.put("createTime", item.getCreateTime());
        result.put("payTime", item.getPaidTime());
        result.put("expireTime", item.getExpireTime());
        result.put("refundTime", item.getRefundTime());
        result.put("refundReason", item.getRefundReason());

        TrRefundApply refund = refundMapper.selectOne(new LambdaQueryWrapper<TrRefundApply>()
                .eq(TrRefundApply::getOrderId, item.getId())
                .orderByDesc(TrRefundApply::getCreateTime).last("limit 1"));
        boolean hasActiveOrApprovedRefund = (refund != null && refund.getStatus() != 2);
        Integer refundStatusCode = null;
        if (refund != null) {
            refundStatusCode = refund.getStatus() == 0 ? 1 : refund.getStatus() == 1 ? 5 : 4;
        }
        final Integer finalRefundStatus = refundStatusCode;

        List<Map<String, Object>> details = itemMapper.selectList(new LambdaQueryWrapper<TrOrderItem>()
                .eq(TrOrderItem::getOrderId, item.getId())).stream().map(detail -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", detail.getId());
            row.put("courseId", detail.getCourseId());
            row.put("name", detail.getCourseName());
            row.put("courseName", detail.getCourseName());
            row.put("cover", detail.getCourseCoverUrl());
            row.put("coverUrl", detail.getCourseCoverUrl());
            row.put("courseCoverUrl", detail.getCourseCoverUrl());
            row.put("price", cents(detail.getUnitPrice()));
            row.put("realPayAmount", cents(detail.getPayableAmount()));
            row.put("canRefund", !hasActiveOrApprovedRefund && item.getPaymentStatus() == 1 && item.getOrderStatus() == 1);
            row.put("refundStatus", finalRefundStatus);
            return row;
        }).toList();
        result.put("details", details);

        String couponName = "已使用优惠券";
        if (item.getCouponId() != null) {
            MktCoupon coupon = couponMapper.selectById(item.getCouponId());
            if (coupon != null && StringUtils.hasText(coupon.getCouponName())) {
                BigDecimal discVal = item.getDiscountAmount() != null ? item.getDiscountAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                couponName = coupon.getCouponName() + " (优惠¥" + discVal.toPlainString() + ")";
            }
        }
        result.put("couponRule", item.getCouponId() == null ? List.of() : List.of(couponName));
        result.put("couponDesc", item.getCouponId() == null ? "" : couponName);
        result.put("message", oldStatusName(toOldOrderStatus(item.getOrderStatus())));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> progressNodes = new ArrayList<>();
        if (item.getCreateTime() != null) {
            progressNodes.add(Map.of("id", 1, "name", "提交订单", "time", item.getCreateTime().format(dtf)));
        }
        if (item.getPaidTime() != null || (item.getPaymentStatus() != null && item.getPaymentStatus() == 1)) {
            LocalDateTime pTime = item.getPaidTime() != null ? item.getPaidTime() : item.getUpdateTime();
            progressNodes.add(Map.of("id", 2, "name", "完成支付", "time", pTime != null ? pTime.format(dtf) : ""));
        }
        if (item.getOrderStatus() != null && item.getOrderStatus() == 4) {
            LocalDateTime cTime = item.getUpdateTime() != null ? item.getUpdateTime() : item.getExpireTime();
            progressNodes.add(Map.of("id", 3, "name", "订单关闭", "time", cTime != null ? cTime.format(dtf) : ""));
        }
        if (refund != null) {
            LocalDateTime rTime = refund.getRefundedTime() != null ? refund.getRefundedTime() : (refund.getAuditTime() != null ? refund.getAuditTime() : refund.getCreateTime());
            String rName = refund.getStatus() == 1 ? "退款成功" : refund.getStatus() == 2 ? "退款已驳回" : "退款申请中";
            progressNodes.add(Map.of("id", 4, "name", rName, "time", rTime != null ? rTime.format(dtf) : ""));
        }
        result.put("progressNodes", progressNodes);
        return result;
    }

    private Map<String, Object> cartView(TrCart item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("courseId", item.getCourseId());
        result.put("name", item.getCourseName());
        result.put("courseName", item.getCourseName());
        result.put("cover", item.getCourseCoverUrl());
        result.put("coverUrl", item.getCourseCoverUrl());
        result.put("courseCoverUrl", item.getCourseCoverUrl());
        result.put("teacherName", item.getTeacherName());
        result.put("price", cents(item.getUnitPrice()));
        result.put("originalPrice", cents(item.getUnitPrice()));
        result.put("quantity", item.getQuantity());
        return result;
    }

    private Map<String, Object> legacyOrderDetailView(Map<String, Object> order, Map<String, Object> detail, String keyword) {
        String courseName = defaultText(detail.get("name"), defaultText(detail.get("courseName"), ""));
        String orderNo = defaultText(order.get("orderNo"), "");
        if (StringUtils.hasText(keyword) && !orderNo.contains(keyword) && !courseName.contains(keyword)) {
            return null;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", detail.get("id") != null ? detail.get("id") : order.get("id"));
        row.put("orderId", order.get("id"));
        row.put("orderNo", order.get("orderNo"));
        row.put("userId", order.get("userId"));
        row.put("courseId", detail.get("courseId"));
        row.put("name", courseName);
        row.put("courseName", courseName);
        row.put("cover", detail.get("cover"));
        row.put("coverUrl", detail.get("coverUrl"));
        row.put("courseCoverUrl", detail.get("courseCoverUrl"));
        row.put("price", detail.get("price") != null ? detail.get("price") : order.get("totalAmount"));
        row.put("realPayAmount", detail.get("realPayAmount") != null ? detail.get("realPayAmount") : order.get("realAmount"));
        row.put("payType", 1);
        row.put("status", order.get("status"));
        row.put("statusName", order.get("statusName"));
        row.put("createTime", order.get("createTime"));
        row.put("payTime", order.get("payTime"));
        row.put("refundStatus", detail.get("refundStatus"));
        row.put("refundStatusName", refundStatusText(detail.get("refundStatus")));
        row.put("refundAmount", order.get("realAmount"));
        return row;
    }

    private String refundStatusText(Object value) {
        Integer code = intValue(value);
        if (code == null) return "";
        return switch (code) {
            case 1 -> "申请中";
            case 2 -> "退款中";
            case 3 -> "退款失败";
            case 4 -> "退款已驳回";
            case 5 -> "退款成功";
            default -> "";
        };
    }

    private int toOldOrderStatus(Integer db) {
        return db == null ? 1 : switch (db) {
            case 0 -> 1;
            case 1 -> 4;
            case 3 -> 6;
            case 4 -> 3;
            default -> 3;
        };
    }

    private String oldStatusName(int value) {
        return switch (value) {
            case 1 -> "待支付";
            case 2 -> "已支付";
            case 3 -> "已关闭";
            case 4 -> "已完成";
            case 5 -> "已报名";
            case 6 -> "已退款";
            default -> "未知";
        };
    }

    private long defaultPriceCents(Long courseId) {
        return switch (String.valueOf(courseId)) {
            case "1" -> 19900;
            case "2" -> 24900;
            case "3" -> 14900;
            case "4" -> 29900;
            case "5" -> 22900;
            case "6" -> 18900;
            case "7" -> 26900;
            case "8" -> 16900;
            case "9" -> 15900;
            case "10" -> 34900;
            default -> 19900;
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((k, v) -> result.put(String.valueOf(k), v));
        return result;
    }
}

