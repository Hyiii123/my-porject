package com.share.trade.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.security.annotation.InnerAuth;
import com.share.trade.domain.TrOrder;
import com.share.trade.service.ITradeOrderService;
import org.springframework.web.bind.annotation.*;

/**
 * 交易微服务内部接口 (供 Share-MQ / 内部微服务调用，受 @InnerAuth 保护)
 */
@RestController
@RequestMapping("/internal")
public class TradeInternalController extends BaseController {

    private final ITradeOrderService orderService;

    public TradeInternalController(ITradeOrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 超时关单内部接口 (具备幂等性防护，避免已支付/已取消订单抛出异常导致 MQ 重复重试与 DLQ 污染)
     */
    @InnerAuth
    @PutMapping("/orders/{id}/cancel")
    public AjaxResult cancelOrder(@PathVariable("id") Long id) {
        if (id == null) {
            return AjaxResult.error("订单ID不能为空");
        }
        TrOrder order;
        try {
            order = orderService.findOrder(id);
        } catch (Exception e) {
            return AjaxResult.success("订单不存在或已删除，无需取消");
        }
        if (order == null) {
            return AjaxResult.success("订单不存在，无需取消");
        }
        if (order.getOrderStatus() != null && order.getOrderStatus() != 0) {
            return AjaxResult.success("订单当前状态已流转为 " + order.getOrderStatus() + "，无需重复取消");
        }
        return success(orderService.cancelOrder(id));
    }

    /**
     * 查询订单内部接口
     */
    @InnerAuth
    @GetMapping("/orders/{id}")
    public AjaxResult getOrder(@PathVariable("id") Long id) {
        if (id == null) {
            return AjaxResult.error("订单ID不能为空");
        }
        try {
            return success(orderService.findOrder(id));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
