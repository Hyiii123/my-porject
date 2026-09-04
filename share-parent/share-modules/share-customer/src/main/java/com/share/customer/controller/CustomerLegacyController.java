package com.share.customer.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.share.common.core.constant.HttpStatus;
import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.customer.domain.CustomerFaq;
import com.share.customer.domain.CustomerKnowledge;
import com.share.customer.domain.CustomerMessage;
import com.share.customer.domain.CustomerSession;
import com.share.customer.domain.dto.AiReplyRecordRequest;
import com.share.customer.domain.dto.CreateSessionRequest;
import com.share.customer.domain.dto.CustomerFaqRequest;
import com.share.customer.domain.dto.CustomerKnowledgeRequest;
import com.share.customer.domain.dto.EvaluationRequest;
import com.share.customer.service.CustomerService;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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

/**
 * 天机旧版客服接口兼容层。
 *
 * <p>旧版 Vue 页面曾使用 /cs/customer-service/*，新页面使用 /customer/*。
 * 两套路径都落到同一个 CustomerService，避免前端迁移时出现功能分叉。</p>
 *
 * <p>按当前需求不提供转人工和人工回复接口；未映射的旧动作会由统一 404 处理。</p>
 */
@RestController
@RequestMapping("/customer-service")
public class CustomerLegacyController extends BaseController {
    private final CustomerService customerService;

    public CustomerLegacyController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/faqs")
    public AjaxResult faqs(@RequestParam(defaultValue = "20") int limit) {
        return success(customerService.listPublicFaq(limit));
    }

    @RequiresLogin
    @PostMapping("/ask")
    public AjaxResult ask(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> source = body == null ? Map.of() : body;
        Long sessionId = longValue(source.get("sessionId"));
        String question = firstText(source, "content", "question", "message");
        CustomerSession session = sessionId == null
                ? customerService.createSession(new CreateSessionRequest())
                : customerService.getMySession(sessionId);
        return success(customerService.sendMessage(session.getId(), question));
    }

    /** 兼容旧页面直连第三方 AI 后回写会话记录的流程。 */
    @RequiresLogin
    @PostMapping("/ai-reply")
    public AjaxResult recordAiReply(@RequestBody @Valid AiReplyRecordRequest request,
            @RequestParam(required = false) Long sessionId) {
        Long effectiveSessionId = sessionId == null ? request.getSessionId() : sessionId;
        return success(customerService.recordExternalReply(effectiveSessionId, request));
    }

    @RequiresLogin
    @GetMapping("/sessions/{id}")
    public AjaxResult session(@PathVariable Long id) {
        return success(customerService.getMySession(id));
    }

    @RequiresLogin
    @PostMapping("/sessions/{id}/evaluate")
    public AjaxResult evaluate(@PathVariable Long id, @Valid @RequestBody EvaluationRequest request) {
        return success(customerService.saveEvaluation(id, request));
    }

    @RequiresPermissions("customer:knowledge:list")
    @GetMapping("/knowledge/page")
    public TableDataInfo knowledgePage(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category, @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNum, @RequestParam(defaultValue = "10") long pageSize) {
        return page(customerService.listKnowledge(keyword, category, status, pageNum, pageSize));
    }

    @RequiresPermissions("customer:knowledge:add")
    @PostMapping("/knowledge")
    public AjaxResult addKnowledge(@Valid @RequestBody CustomerKnowledgeRequest request) {
        return success(customerService.saveKnowledge(request));
    }

    @RequiresPermissions("customer:knowledge:edit")
    @PutMapping({"/knowledge", "/knowledge/{id}"})
    public AjaxResult editKnowledge(@PathVariable(required = false) Long id,
            @Valid @RequestBody CustomerKnowledgeRequest request) {
        if (id != null) {
            request.setId(id);
        }
        return success(customerService.saveKnowledge(request));
    }

    @RequiresPermissions("customer:knowledge:remove")
    @DeleteMapping("/knowledge/{ids}")
    public AjaxResult removeKnowledge(@PathVariable Long[] ids) {
        customerService.removeKnowledge(Arrays.asList(ids));
        return success();
    }

    @RequiresPermissions("customer:faq:list")
    @GetMapping("/faqs/page")
    public TableDataInfo faqPage(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category, @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") long pageNum, @RequestParam(defaultValue = "10") long pageSize) {
        return page(customerService.listFaq(keyword, category, enabled, pageNum, pageSize));
    }

    @RequiresPermissions("customer:faq:add")
    @PostMapping("/faqs")
    public AjaxResult addFaq(@Valid @RequestBody CustomerFaqRequest request) {
        return success(customerService.saveFaq(request));
    }

    @RequiresPermissions("customer:faq:edit")
    @PutMapping({"/faqs", "/faqs/{id}"})
    public AjaxResult editFaq(@PathVariable(required = false) Long id,
            @Valid @RequestBody CustomerFaqRequest request) {
        if (id != null) {
            request.setId(id);
        }
        return success(customerService.saveFaq(request));
    }

    @RequiresPermissions("customer:faq:remove")
    @DeleteMapping("/faqs/{ids}")
    public AjaxResult removeFaq(@PathVariable Long[] ids) {
        customerService.removeFaq(Arrays.asList(ids));
        return success();
    }

    @RequiresPermissions("customer:session:list")
    @GetMapping("/sessions/page")
    public TableDataInfo sessionPage(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status, @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return page(customerService.listAllSessions(keyword, status, pageNum, pageSize));
    }

    @RequiresPermissions("customer:session:query")
    @GetMapping("/sessions/{id}/messages")
    public AjaxResult sessionMessages(@PathVariable Long id) {
        List<CustomerMessage> messages = customerService.listAdminMessages(id);
        return success(messages);
    }

    @RequiresPermissions("customer:statistics:view")
    @GetMapping("/stats")
    public AjaxResult stats() {
        return success(customerService.statistics());
    }

    private TableDataInfo page(IPage<?> data) {
        TableDataInfo result = new TableDataInfo();
        result.setCode(HttpStatus.SUCCESS);
        result.setMsg("查询成功");
        result.setRows(data.getRecords());
        result.setTotal(data.getTotal());
        return result;
    }

    private String firstText(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                return String.valueOf(value).trim();
            }
        }
        return "";
    }

    private Long longValue(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
