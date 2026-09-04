package com.share.trade.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.share.common.core.constant.HttpStatus;
import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.trade.domain.MktCoupon;
import com.share.trade.service.TradeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 交易管理端接口。 */
@RestController
@RequestMapping("/admin")
public class TradeAdminController extends BaseController {
    private final TradeService tradeService;

    public TradeAdminController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @RequiresPermissions("trade:coupon:list")
    @GetMapping("/coupons/list")
    public TableDataInfo coupons(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status, @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize) {
        return page(tradeService.pageCoupons(keyword, status, pageNo, pageSize));
    }

    @RequiresPermissions("trade:coupon:add")
    @PostMapping("/coupons")
    public AjaxResult add(@RequestBody MktCoupon value) {
        return success(tradeService.saveCoupon(value));
    }

    @RequiresPermissions("trade:coupon:edit")
    @PutMapping("/coupons")
    public AjaxResult edit(@RequestBody MktCoupon value) {
        return success(tradeService.saveCoupon(value));
    }

    @RequiresPermissions("trade:order:list")
    @GetMapping("/orders/list")
    public TableDataInfo orders(@RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNo, @RequestParam(defaultValue = "10") long pageSize) {
        java.util.Map<String, Object> params = new java.util.HashMap<>(); params.put("status", status); params.put("pageNo", pageNo); params.put("pageSize", pageSize);
        java.util.Map<String, Object> value = tradeService.listOrders(params, true); TableDataInfo result = new TableDataInfo(); result.setCode(HttpStatus.SUCCESS); result.setMsg("查询成功"); result.setTotal(((Number) value.get("total")).longValue()); result.setRows((java.util.List<?>) value.get("list")); return result;
    }

    @RequiresPermissions("trade:refund:list")
    @GetMapping("/refunds/list")
    public TableDataInfo refunds(@RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNo, @RequestParam(defaultValue = "10") long pageSize) {
        return page(tradeService.pageRefunds(status, pageNo, pageSize));
    }

    @RequiresPermissions("trade:statistics:view")
    @GetMapping("/statistics/overview")
    public AjaxResult statistics() {
        return success(tradeService.statistics());
    }

    private TableDataInfo page(IPage<?> data) { TableDataInfo result = new TableDataInfo(); result.setCode(HttpStatus.SUCCESS); result.setMsg("查询成功"); result.setRows(data.getRecords()); result.setTotal(data.getTotal()); return result; }
}
