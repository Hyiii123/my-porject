package com.share.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.share.common.core.exception.ServiceException;
import com.share.common.security.utils.SecurityUtils;
import com.share.trade.domain.*;
import com.share.trade.mapper.*;
import com.share.trade.service.ITradeCouponService;
import com.share.trade.service.ITradeOrderService;
import com.share.trade.service.ITradePaymentService;
import com.share.trade.service.support.TradeUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.share.trade.service.support.TradeUtils.*;

/**
 * 支付与退款领域服务实现类
 */
@Service
@Primary
public class TradePaymentServiceImpl implements ITradePaymentService {

    private final TrOrderMapper orderMapper;
    private final TrOrderItemMapper itemMapper;
    private final TrPaymentOrderMapper paymentMapper;
    private final TrRefundApplyMapper refundMapper;
    private final MktCouponMapper couponMapper;
    private final ITradeOrderService orderService;
    private final ITradeCouponService couponService;
    private final Environment environment;

    public TradePaymentServiceImpl(TrOrderMapper orderMapper,
                                   TrOrderItemMapper itemMapper,
                                   TrPaymentOrderMapper paymentMapper,
                                   TrRefundApplyMapper refundMapper,
                                   MktCouponMapper couponMapper,
                                   @Qualifier("tradeOrderServiceImpl") ITradeOrderService orderService,
                                   @Qualifier("tradeCouponServiceImpl") ITradeCouponService couponService,
                                   Environment environment) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.paymentMapper = paymentMapper;
        this.refundMapper = refundMapper;
        this.couponMapper = couponMapper;
        this.orderService = orderService;
        this.couponService = couponService;
        this.environment = environment;
    }

    @Override
    public List<Map<String, Object>> paymentChannels() {
        return List.of(
                Map.of("id", "wechat", "name", "微信支付", "type", "wechat"),
                Map.of("id", "alipay", "name", "支付宝", "type", "alipay")
        );
    }

    @Override
    @Transactional
    public Map<String, Object> createPayment(Map<String, ?> body) {
        Long orderId = longValue(body == null ? null : body.get("orderId"));
        TrOrder order = orderService.findOrder(orderId);
        require(Objects.equals(order.getUserId(), currentUserId()), "无权支付该订单");
        require(order.getOrderStatus() == 0, "订单当前状态不可发起支付"
                + (order.getOrderStatus() == 4 ? "（订单已超时关闭）" : order.getOrderStatus() == 1 ? "（订单已支付）" : ""));

        String channel = defaultText(body == null ? null : body.get("channel"),
                defaultText(body == null ? null : body.get("paymentChannel"), "wechat"));

        TrPaymentOrder payment = paymentMapper.selectOne(new LambdaQueryWrapper<TrPaymentOrder>()
                .eq(TrPaymentOrder::getOrderId, order.getId())
                .eq(TrPaymentOrder::getPaymentChannel, channel));
        if (payment == null) {
            payment = new TrPaymentOrder();
            payment.setId(newId());
            payment.setPaymentNo("PAY" + System.currentTimeMillis());
            payment.setOrderId(order.getId());
            payment.setPaymentChannel(channel);
            payment.setAmount(order.getPayableAmount());
            payment.setStatus(0);
            payment.setExpireTime(order.getExpireTime());
            payment.setCreateTime(LocalDateTime.now());
            payment.setUpdateTime(LocalDateTime.now());
            payment.setVersion(0);
            paymentMapper.insert(payment);
        }

        order.setPaymentChannel(channel);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentNo", payment.getPaymentNo());
        result.put("orderId", order.getId());
        result.put("amount", cents(order.getPayableAmount()));
        result.put("payUrl", "demo://zhiwen-pay/" + payment.getPaymentNo());
        result.put("status", payment.getStatus());
        result.put("demo", true);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> simulatePayment(Long orderId) {
        if (environment != null && environment.acceptsProfiles(Profiles.of("prod", "production"))) {
            if (!SecurityUtils.isAdmin(currentUserId())) {
                throw new ServiceException("生产环境严禁使用模拟支付通道，请使用正式渠道完成支付");
            }
        }
        TrOrder order = orderService.findOrder(orderId);
        require(Objects.equals(order.getUserId(), currentUserId()) || SecurityUtils.isAdmin(currentUserId()), "无权支付该订单");
        require(order.getOrderStatus() == 0, "订单当前状态不可支付"
                + (order.getOrderStatus() == 4 ? "（订单已超时关闭）" : order.getOrderStatus() == 1 ? "（订单已支付）" : ""));

        LocalDateTime now = LocalDateTime.now();
        TrPaymentOrder payment = paymentMapper.selectOne(new LambdaQueryWrapper<TrPaymentOrder>()
                .eq(TrPaymentOrder::getOrderId, orderId).orderByDesc(TrPaymentOrder::getCreateTime).last("limit 1"));
        if (payment == null) {
            payment = new TrPaymentOrder();
            payment.setId(newId());
            payment.setPaymentNo("PAY" + System.currentTimeMillis());
            payment.setOrderId(orderId);
            payment.setPaymentChannel(defaultText(order.getPaymentChannel(), "wechat"));
            payment.setAmount(order.getPayableAmount());
            payment.setExpireTime(order.getExpireTime());
            payment.setCreateTime(now);
            payment.setVersion(0);
        }
        payment.setStatus(1);
        payment.setThirdPartyNo("DEMO-" + payment.getPaymentNo());
        payment.setPaidTime(now);
        payment.setUpdateTime(now);
        if (paymentMapper.selectById(payment.getId()) == null) paymentMapper.insert(payment);
        else paymentMapper.updateById(payment);

        boolean updated = orderMapper.update(null, new LambdaUpdateWrapper<TrOrder>()
                .set(TrOrder::getPaymentStatus, 1)
                .set(TrOrder::getOrderStatus, 1)
                .set(TrOrder::getPaidAmount, order.getPayableAmount())
                .set(TrOrder::getPaidTime, now)
                .set(TrOrder::getPaymentChannel, payment.getPaymentChannel())
                .set(TrOrder::getUpdateTime, now)
                .eq(TrOrder::getId, order.getId())
                .eq(TrOrder::getOrderStatus, 0)) > 0;
        if (!updated) {
            throw new ServiceException("订单已被处理或支付状态已变更，请勿重复支付");
        }
        order.setPaymentStatus(1);
        order.setOrderStatus(1);
        order.setPaidAmount(order.getPayableAmount());
        order.setPaidTime(now);
        order.setPaymentChannel(payment.getPaymentChannel());
        order.setUpdateTime(now);

        orderService.enrollPurchasedCourses(order);
        return orderService.orderView(order);
    }

    @Override
    public Map<String, Object> paymentState(Long orderId) {
        TrOrder order = orderService.findOrder(orderId);
        require(Objects.equals(order.getUserId(), currentUserId()) || SecurityUtils.isAdmin(currentUserId()), "无权查看该订单");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", order.getId());
        result.put("status", order.getPaymentStatus());
        result.put("paymentStatus", order.getPaymentStatus());
        result.put("expireTime", order.getExpireTime());
        result.put("paidTime", order.getPaidTime());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> applyRefund(Map<String, ?> body) {
        Long detailId = longValue(body == null ? null : body.get("orderDetailId"));
        TrOrderItem item = detailId == null ? null : itemMapper.selectById(detailId);
        require(item != null, "订单明细不存在");
        TrOrder order = orderService.findOrder(item.getOrderId());
        require(Objects.equals(order.getUserId(), currentUserId()), "无权申请退款");
        require(Integer.valueOf(1).equals(order.getPaymentStatus()) && Integer.valueOf(1).equals(order.getOrderStatus()), "当前订单不可申请退款");

        TrRefundApply old = refundMapper.selectOne(new LambdaQueryWrapper<TrRefundApply>()
                .eq(TrRefundApply::getOrderId, order.getId())
                .orderByDesc(TrRefundApply::getCreateTime).last("limit 1"));
        if (old != null) {
            if (old.getStatus() == 0) {
                return refundView(old);
            }
            if (old.getStatus() == 1) {
                throw new ServiceException("该订单已成功退款，无法重复申请");
            }
        }

        TrRefundApply value = new TrRefundApply();
        value.setId(newId());
        value.setRefundNo("REF" + System.currentTimeMillis());
        value.setOrderId(order.getId());
        value.setUserId(currentUserId());
        value.setRefundAmount(item.getPayableAmount());
        value.setReason(defaultText(body == null ? null : body.get("refundReason"),
                defaultText(body == null ? null : body.get("questionDesc"), "用户申请退款")));
        value.setStatus(0);
        value.setCreateTime(LocalDateTime.now());
        value.setUpdateTime(LocalDateTime.now());
        value.setVersion(0);
        value.setDelFlag(0);
        refundMapper.insert(value);
        return refundView(value);
    }

    @Override
    public Map<String, Object> refund(Long id) {
        TrRefundApply value = refundMapper.selectById(id);
        if (value == null) {
            TrOrderItem item = itemMapper.selectById(id);
            if (item != null) {
                value = refundMapper.selectOne(new LambdaQueryWrapper<TrRefundApply>()
                        .eq(TrRefundApply::getOrderId, item.getOrderId())
                        .orderByDesc(TrRefundApply::getCreateTime).last("limit 1"));
            }
        }
        if (value == null) throw new ServiceException("退款记录不存在");
        TrOrder order = orderService.findOrder(value.getOrderId());
        require(Objects.equals(order.getUserId(), currentUserId()) || SecurityUtils.isAdmin(currentUserId()), "无权查看该退款记录");
        return refundView(value);
    }

    @Override
    public IPage<TrRefundApply> pageRefunds(Integer status, long pageNo, long pageSize) {
        Page<TrRefundApply> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        refundMapper.selectPage(page, new LambdaQueryWrapper<TrRefundApply>()
                .eq(status != null, TrRefundApply::getStatus, status)
                .orderByDesc(TrRefundApply::getCreateTime));
        return page;
    }

    @Override
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

    @Override
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
                order.setOrderStatus(3);
                order.setPaymentStatus(3);
                order.setRefundTime(LocalDateTime.now());
                order.setRefundReason(value.getReason());
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.updateById(order);
                couponService.restoreUserCoupon(order.getCouponId());
                orderService.revokePurchasedCourses(order);
            }
        }
        return legacyRefundView(value);
    }

    @Override
    public Map<String, Object> nextRefund() {
        TrRefundApply value = refundMapper.selectOne(new LambdaQueryWrapper<TrRefundApply>()
                .eq(TrRefundApply::getStatus, 0).orderByAsc(TrRefundApply::getCreateTime).last("limit 1"));
        if (value == null) return Map.of();
        return legacyRefundView(value);
    }

    @Override
    public Map<String, Object> legacyRefundViewForApi(Long id) {
        TrRefundApply value = refundMapper.selectById(id);
        if (value == null) throw new ServiceException("退款记录不存在");
        return legacyRefundView(value);
    }

    @Override
    public Map<String, Object> statistics() {
        Map<String, Object> stats = orderMapper.selectOrderStats();
        if (stats == null) stats = Map.of();
        BigDecimal totalRefundAmount = refundMapper.selectTotalRefundAmount();
        if (totalRefundAmount == null) totalRefundAmount = BigDecimal.ZERO;

        BigDecimal totalAmount = stats.get("totalAmount") != null ? new BigDecimal(stats.get("totalAmount").toString()) : BigDecimal.ZERO;
        BigDecimal pendingAmount = stats.get("pendingAmount") != null ? new BigDecimal(stats.get("pendingAmount").toString()) : BigDecimal.ZERO;
        BigDecimal paidAmount = stats.get("paidAmount") != null ? new BigDecimal(stats.get("paidAmount").toString()) : BigDecimal.ZERO;
        long totalOrders = stats.get("totalOrders") != null ? Long.parseLong(stats.get("totalOrders").toString()) : 0L;
        long pendingOrders = stats.get("pendingOrders") != null ? Long.parseLong(stats.get("pendingOrders").toString()) : 0L;
        long paidOrders = stats.get("paidOrders") != null ? Long.parseLong(stats.get("paidOrders").toString()) : 0L;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalOrders", totalOrders);
        result.put("pendingOrders", pendingOrders);
        result.put("paidOrders", paidOrders);
        result.put("totalAmount", totalAmount);
        result.put("pendingAmount", pendingAmount);
        result.put("paidAmount", paidAmount);
        result.put("refundAmount", totalRefundAmount);
        result.put("totalRevenue", paidAmount);
        result.put("totalCoupons", couponMapper.selectCount(new LambdaQueryWrapper<>()));
        result.put("refunds", refundMapper.selectCount(new LambdaQueryWrapper<>()));
        return result;
    }

    private Map<String, Object> refundView(TrRefundApply item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("refundOrderNo", item.getRefundNo());
        result.put("orderId", item.getOrderId());
        result.put("refundReason", item.getReason());
        result.put("refundChannel", "原支付渠道");
        result.put("refundAmount", cents(item.getRefundAmount()));
        result.put("status", item.getStatus());
        result.put("statusText", item.getStatus() == 0 ? "审核中" : item.getStatus() == 1 ? "已同意退款" : "已拒绝退款");
        result.put("remark", item.getStatus() == 0 ? null : (item.getStatus() == 1 || item.getStatus() == 3));
        result.put("approvalOpinion", item.getAuditRemark());
        result.put("createTime", item.getCreateTime());
        result.put("approveTime", item.getAuditTime());
        result.put("refundedTime", item.getRefundedTime());
        TrOrder order = orderMapper.selectById(item.getOrderId());
        if (order != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            result.put("payChannel", order.getPaymentChannel() != null
                    ? ("wechat".equals(order.getPaymentChannel()) ? "微信支付" : "alipay".equals(order.getPaymentChannel()) ? "支付宝" : order.getPaymentChannel())
                    : "在线支付");
            result.put("orderTime", order.getCreateTime() != null ? order.getCreateTime().format(fmt) : "--");
            result.put("paySuccessTime", order.getPaidTime() != null ? order.getPaidTime().format(fmt) : "--");
        } else {
            result.put("payChannel", "在线支付");
            result.put("orderTime", "--");
            result.put("paySuccessTime", "--");
        }
        return result;
    }

    private Map<String, Object> legacyRefundView(TrRefundApply item) {
        Map<String, Object> result = refundView(item);
        TrOrder order = orderMapper.selectById(item.getOrderId());
        result.put("orderNo", order == null ? "" : order.getOrderNo());
        result.put("userId", item.getUserId());
        result.put("userName", "用户" + defaultValue(item.getUserId(), 0L));
        result.put("courseName", "课程");
        result.put("courseCover", "/src/assets/images/courses/vue3.svg");
        result.put("payAmount", order == null ? moneyText(item.getRefundAmount()) : moneyText(order.getPayableAmount().movePointRight(2)));
        result.put("reason", item.getReason());
        result.put("status", legacyRefundStatusText(item.getStatus()));
        result.put("applyTime", item.getCreateTime());
        result.put("approver", item.getAuditUserId() == null ? null : "管理员");
        result.put("approveTime", item.getAuditTime());
        result.put("rejectReason", item.getStatus() != null && item.getStatus() == 2 ? item.getAuditRemark() : null);
        if (order != null) {
            TrOrderItem detail = itemMapper.selectOne(new LambdaQueryWrapper<TrOrderItem>()
                    .eq(TrOrderItem::getOrderId, order.getId()).orderByAsc(TrOrderItem::getCreateTime).last("limit 1"));
            if (detail != null) {
                result.put("courseName", detail.getCourseName());
                result.put("courseCover", detail.getCourseCoverUrl());
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

    private String legacyRefundStatusText(Integer value) {
        return switch (defaultValue(value, 0)) {
            case 0 -> "pending";
            case 1, 3 -> "approved";
            case 2 -> "rejected";
            default -> "pending";
        };
    }

    private Integer legacyRefundStatus(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) return null;
        if (value instanceof Number number) return number.intValue();
        return switch (String.valueOf(value).toLowerCase()) {
            case "pending" -> 0;
            case "approved" -> 1;
            case "rejected" -> 2;
            default -> intValue(value);
        };
    }
}
