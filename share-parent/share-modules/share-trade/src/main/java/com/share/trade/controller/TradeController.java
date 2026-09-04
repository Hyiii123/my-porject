package com.share.trade.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.trade.service.TradeService;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 用户端优惠券、购物车、订单、支付和退款兼容接口。 */
@RestController
public class TradeController extends BaseController {
    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @GetMapping({"/coupons/list", "/coupons/page"})
    public AjaxResult coupons(@RequestParam Map<String, Object> params) {
        if (params != null && (params.containsKey("pageNo") || params.containsKey("pageSize")
                || params.containsKey("keyword") || params.containsKey("type"))) {
            return success(tradeService.pageLegacyCoupons(params));
        }
        return success(tradeService.collectableCoupons());
    }

    @GetMapping("/coupons/{id}")
    public AjaxResult coupon(@PathVariable Long id) {
        return success(tradeService.coupon(id));
    }

    /** 旧管理端优惠券新增入口，继续沿用 /prs/coupons 路径。 */
    @RequiresPermissions("trade:coupon:add")
    @PostMapping({"/coupons", "/coupon"})
    public AjaxResult addCoupon(@RequestBody(required = false) Map<String, Object> body) {
        return success(tradeService.saveLegacyCoupon(body == null ? Map.of() : body));
    }

    @PutMapping({"/coupons/{id}", "/coupon/{id}"})
    @RequiresPermissions("trade:coupon:edit")
    public AjaxResult updateCoupon(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> value = body == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(body);
        value.put("id", id);
        return success(tradeService.saveLegacyCoupon(value));
    }

    @DeleteMapping({"/coupons/{id}", "/coupon/{id}"})
    @RequiresPermissions("trade:coupon:remove")
    public AjaxResult deleteCoupon(@PathVariable Long id) {
        tradeService.deleteCoupon(id);
        return success();
    }

    @PutMapping("/coupons/{id}/pause")
    @RequiresPermissions("trade:coupon:edit")
    public AjaxResult pauseCoupon(@PathVariable Long id) {
        return success(tradeService.setCouponStatus(id, 0));
    }

    @PutMapping("/coupons/{id}/issue")
    @RequiresPermissions("trade:coupon:edit")
    public AjaxResult issueCoupon(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return success(tradeService.setCouponStatus(id, 1));
    }

    @GetMapping("/codes/page")
    @RequiresPermissions("trade:coupon:list")
    public AjaxResult couponCodes(@RequestParam Map<String, Object> params) {
        return success(tradeService.pageCouponCodes(params));
    }

    @RequiresLogin
    @GetMapping("/user-coupons/page")
    public AjaxResult userCoupons(@RequestParam Map<String, Object> params) {
        return success(tradeService.userCoupons(params));
    }

    @RequiresLogin
    @PostMapping("/user-coupons/{id}/receive")
    public AjaxResult receive(@PathVariable Long id) {
        return success(tradeService.receiveCoupon(id));
    }

    @RequiresLogin
    @PostMapping("/user-coupons/{code}/exchange")
    public AjaxResult exchange(@PathVariable String code) {
        return success(tradeService.exchangeCoupon(code));
    }

    @RequiresLogin
    @GetMapping("/carts")
    public AjaxResult carts() {
        return success(tradeService.carts());
    }

    @RequiresLogin
    @PostMapping("/carts")
    public AjaxResult addCart(@RequestBody Map<String, Object> body) {
        return success(tradeService.addCart(body));
    }

    @RequiresLogin
    @DeleteMapping("/carts")
    public AjaxResult removeCarts(@RequestParam(required = false) String ids) {
        tradeService.removeCarts(ids);
        return success();
    }

    @RequiresLogin
    @GetMapping("/orders/page")
    public AjaxResult orders(@RequestParam Map<String, Object> params) {
        return success(tradeService.listOrders(params, false));
    }

    @RequiresLogin
    @GetMapping("/orders/{id}")
    public AjaxResult order(@PathVariable Long id) {
        return success(tradeService.order(id));
    }

    @RequiresLogin
    @GetMapping("/orders/prePlaceOrder")
    public AjaxResult prePlaceOrder(@RequestParam Map<String, Object> params) {
        return success(tradeService.prePlaceOrder(params));
    }

    @RequiresLogin
    @PostMapping("/orders/placeOrder")
    public AjaxResult placeOrder(@RequestBody Map<String, Object> body) {
        return success(tradeService.placeOrder(body));
    }

    @RequiresLogin
    @PostMapping("/orders/freeCourse/{courseId}")
    public AjaxResult freeCourse(@PathVariable Long courseId) {
        return success(tradeService.freeCourse(courseId));
    }

    @RequiresLogin
    @PutMapping("/orders/{id}/cancel")
    public AjaxResult cancel(@PathVariable Long id) {
        return success(tradeService.cancelOrder(id));
    }

    @RequiresLogin
    @DeleteMapping("/orders/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        tradeService.deleteOrder(id);
        return success();
    }

    @GetMapping("/pay/channels")
    public AjaxResult paymentChannels() {
        return success(tradeService.paymentChannels());
    }

    @RequiresLogin
    @PostMapping("/pay/order")
    public AjaxResult createPayment(@RequestBody Map<String, Object> body) {
        return success(tradeService.createPayment(body));
    }

    /** 仅供本地 Demo 验收使用，生产环境应替换为第三方支付回调。 */
    @RequiresLogin
    @PostMapping("/pay/order/{orderId}/demo-success")
    public AjaxResult simulatePayment(@PathVariable Long orderId) {
        return success(tradeService.simulatePayment(orderId));
    }

    @RequiresLogin
    @GetMapping("/orders/{orderId}/status")
    public AjaxResult paymentState(@PathVariable Long orderId) {
        return success(tradeService.paymentState(orderId));
    }

    @RequiresLogin
    @PostMapping("/refund-apply")
    public AjaxResult applyRefund(@RequestBody Map<String, Object> body) {
        return success(tradeService.applyRefund(body));
    }

    @RequiresLogin
    @GetMapping("/refund-apply/detail/{id}")
    public AjaxResult refund(@PathVariable Long id) {
        return success(tradeService.refund(id));
    }

    @GetMapping("/order-details/page")
    @RequiresPermissions("trade:order:list")
    public AjaxResult orderDetails(@RequestParam Map<String, Object> params) {
        return success(tradeService.pageLegacyOrderDetails(params));
    }

    @GetMapping("/order-details/{id}")
    @RequiresPermissions("trade:order:list")
    public AjaxResult orderDetail(@PathVariable Long id) {
        return success(tradeService.legacyOrderDetail(id));
    }

    @GetMapping("/refund-apply/page")
    @RequiresPermissions("trade:refund:list")
    public AjaxResult refundPage(@RequestParam Map<String, Object> params) {
        return success(tradeService.pageLegacyRefunds(params));
    }

    @GetMapping("/refund-apply/{id}")
    @RequiresPermissions("trade:refund:list")
    public AjaxResult refundLegacy(@PathVariable Long id) {
        return success(tradeService.legacyRefundViewForApi(id));
    }

    @PutMapping("/refund-apply/approval")
    @RequiresPermissions("trade:refund:edit")
    public AjaxResult refundApproval(@RequestBody Map<String, Object> body) {
        return success(tradeService.approveRefund(body));
    }

    @GetMapping("/refund-apply/next")
    @RequiresPermissions("trade:refund:list")
    public AjaxResult nextRefund() {
        return success(tradeService.nextRefund());
    }
}
