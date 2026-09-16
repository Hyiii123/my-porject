package com.share.trade.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.security.annotation.InnerAuth;
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
     * 超时关单内部接口
     */
    @InnerAuth
    @PutMapping("/orders/{id}/cancel")
    public AjaxResult cancelOrder(@PathVariable("id") Long id) {
        return success(orderService.cancelOrder(id));
    }

    /**
     * 查询订单内部接口
     */
    @InnerAuth
    @GetMapping("/orders/{id}")
    public AjaxResult getOrder(@PathVariable("id") Long id) {
        return success(orderService.findOrder(id));
    }
}
