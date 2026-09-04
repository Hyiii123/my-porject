package com.share.customer.controller;

import com.share.customer.domain.CustomerFaq;
import com.share.common.core.web.domain.AjaxResult;
import com.share.customer.service.CustomerService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 用户端 FAQ 查询接口。 */
@RestController
@RequestMapping("/faq")
public class CustomerFaqController {
    private final CustomerService customerService;

    public CustomerFaqController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/public")
    public AjaxResult publicFaq(@RequestParam(defaultValue = "20") int limit) {
        return AjaxResult.success(customerService.listPublicFaq(limit));
    }
}
