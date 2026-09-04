package com.share.customer.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.share.common.core.constant.HttpStatus;
import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.log.annotation.Log;
import com.share.common.log.enums.BusinessType;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.customer.domain.CustomerFaq;
import com.share.customer.domain.CustomerKnowledge;
import com.share.customer.domain.CustomerMessage;
import com.share.customer.domain.CustomerSession;
import com.share.customer.domain.dto.AiConfigRequest;
import com.share.customer.domain.dto.AiTestRequest;
import com.share.customer.domain.dto.CustomerFaqRequest;
import com.share.customer.domain.dto.CustomerKnowledgeRequest;
import com.share.customer.domain.dto.SendMessageRequest;
import com.share.customer.service.CustomerService;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 客服管理端接口。 */
@RestController
@RequestMapping("/admin")
public class CustomerAdminController extends BaseController {
    private final CustomerService customerService;

    public CustomerAdminController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @RequiresPermissions("customer:knowledge:list")
    @GetMapping("/knowledge/list")
    public TableDataInfo knowledgeList(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category, @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNum, @RequestParam(defaultValue = "10") long pageSize) {
        return page(customerService.listKnowledge(keyword, category, status, pageNum, pageSize));
    }

    @RequiresPermissions("customer:knowledge:query")
    @GetMapping("/knowledge/{id}")
    public AjaxResult knowledgeInfo(@PathVariable Long id) {
        return success(customerService.getKnowledge(id));
    }

    @RequiresPermissions("customer:knowledge:add")
    @Log(title = "客服知识库", businessType = BusinessType.INSERT)
    @PostMapping("/knowledge")
    public AjaxResult addKnowledge(@Valid @RequestBody CustomerKnowledgeRequest request) {
        return success(customerService.saveKnowledge(request));
    }

    @RequiresPermissions("customer:knowledge:edit")
    @Log(title = "客服知识库", businessType = BusinessType.UPDATE)
    @PutMapping("/knowledge")
    public AjaxResult editKnowledge(@Valid @RequestBody CustomerKnowledgeRequest request) {
        return success(customerService.saveKnowledge(request));
    }

    @RequiresPermissions("customer:knowledge:remove")
    @Log(title = "客服知识库", businessType = BusinessType.DELETE)
    @DeleteMapping("/knowledge/{ids}")
    public AjaxResult removeKnowledge(@PathVariable Long[] ids) {
        customerService.removeKnowledge(Arrays.asList(ids));
        return success();
    }

    @RequiresPermissions("customer:faq:list")
    @GetMapping("/faq/list")
    public TableDataInfo faqList(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category, @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") long pageNum, @RequestParam(defaultValue = "10") long pageSize) {
        return page(customerService.listFaq(keyword, category, enabled, pageNum, pageSize));
    }

    @RequiresPermissions("customer:faq:query")
    @GetMapping("/faq/{id}")
    public AjaxResult faqInfo(@PathVariable Long id) {
        return success(customerService.getFaq(id));
    }

    @RequiresPermissions("customer:faq:add")
    @Log(title = "客服常见问题", businessType = BusinessType.INSERT)
    @PostMapping("/faq")
    public AjaxResult addFaq(@Valid @RequestBody CustomerFaqRequest request) {
        return success(customerService.saveFaq(request));
    }

    @RequiresPermissions("customer:faq:edit")
    @Log(title = "客服常见问题", businessType = BusinessType.UPDATE)
    @PutMapping("/faq")
    public AjaxResult editFaq(@Valid @RequestBody CustomerFaqRequest request) {
        return success(customerService.saveFaq(request));
    }

    @RequiresPermissions("customer:faq:remove")
    @Log(title = "客服常见问题", businessType = BusinessType.DELETE)
    @DeleteMapping("/faq/{ids}")
    public AjaxResult removeFaq(@PathVariable Long[] ids) {
        customerService.removeFaq(Arrays.asList(ids));
        return success();
    }

    @RequiresPermissions("customer:session:list")
    @GetMapping("/sessions/list")
    public TableDataInfo sessionList(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status, @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return page(customerService.listAllSessions(keyword, status, pageNum, pageSize));
    }

    @RequiresPermissions("customer:session:query")
    @GetMapping("/sessions/{sessionId}")
    public AjaxResult sessionInfo(@PathVariable Long sessionId) {
        return success(customerService.getAdminSession(sessionId));
    }

    @RequiresPermissions("customer:session:query")
    @GetMapping("/sessions/{sessionId}/messages")
    public AjaxResult sessionMessages(@PathVariable Long sessionId) {
        List<CustomerMessage> messages = customerService.listAdminMessages(sessionId);
        return success(messages);
    }

    @RequiresPermissions("customer:session:close")
    @Log(title = "客服会话", businessType = BusinessType.UPDATE)
    @PostMapping("/sessions/{sessionId}/close")
    public AjaxResult closeSession(@PathVariable Long sessionId) {
        return success(customerService.closeSession(sessionId));
    }

    @RequiresPermissions("customer:statistics:view")
    @GetMapping("/statistics/overview")
    public AjaxResult statistics() {
        return success(customerService.statistics());
    }

    @RequiresPermissions("customer:ai:query")
    @GetMapping("/ai/config")
    public AjaxResult aiConfig() {
        return success(customerService.getAiConfigView());
    }

    @RequiresPermissions("customer:ai:edit")
    // API Key 只允许进入 Redis，不能被若依操作日志序列化到 sys_oper_log。
    @Log(title = "第三方 AI 配置", businessType = BusinessType.UPDATE,
            excludeParamNames = {"apiKey"})
    @PutMapping("/ai/config")
    public AjaxResult saveAiConfig(@Valid @RequestBody AiConfigRequest request) {
        return success(customerService.saveAiConfig(request));
    }

    @RequiresPermissions("customer:ai:test")
    @PostMapping("/ai/test")
    public AjaxResult testAi(@Valid @RequestBody AiTestRequest request) {
        return success(customerService.testAi(request.getMessage()));
    }

    private TableDataInfo page(IPage<?> page) {
        TableDataInfo result = new TableDataInfo();
        result.setCode(HttpStatus.SUCCESS);
        result.setMsg("查询成功");
        result.setRows(page.getRecords());
        result.setTotal(page.getTotal());
        return result;
    }
}
