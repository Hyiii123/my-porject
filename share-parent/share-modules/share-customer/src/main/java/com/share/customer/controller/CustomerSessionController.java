package com.share.customer.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.share.common.core.constant.HttpStatus;
import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.security.annotation.RequiresLogin;
import com.share.customer.domain.CustomerMessage;
import com.share.customer.domain.CustomerSession;
import com.share.customer.domain.dto.CreateSessionRequest;
import com.share.customer.domain.dto.EvaluationRequest;
import com.share.customer.domain.dto.AiReplyRecordRequest;
import com.share.customer.domain.dto.SendMessageRequest;
import com.share.customer.service.CustomerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 用户端 AI 客服会话接口。 */
@RestController
@RequestMapping("/session")
public class CustomerSessionController extends BaseController {
    private final CustomerService customerService;

    public CustomerSessionController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @RequiresLogin
    @PostMapping
    public AjaxResult create(@Valid @RequestBody(required = false) CreateSessionRequest request) {
        return success(customerService.createSession(request == null ? new CreateSessionRequest() : request));
    }

    @RequiresLogin
    @GetMapping("/my")
    public TableDataInfo mySessions(@RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return page(customerService.listMySessions(pageNum, pageSize));
    }

    @RequiresLogin
    @GetMapping("/{sessionId}")
    public AjaxResult getInfo(@PathVariable Long sessionId) {
        return success(customerService.getMySession(sessionId));
    }

    @RequiresLogin
    @GetMapping("/{sessionId}/messages")
    public AjaxResult messages(@PathVariable Long sessionId) {
        List<CustomerMessage> messages = customerService.listMyMessages(sessionId);
        return success(messages);
    }

    @RequiresLogin
    @PostMapping("/{sessionId}/messages")
    public AjaxResult sendMessage(@PathVariable Long sessionId, @Valid @RequestBody SendMessageRequest request) {
        return success(customerService.sendMessage(sessionId, request.getContent(),
                request.getApiKey(), request.getModel()));
    }

    @RequiresLogin
    @PostMapping("/{sessionId}/messages/record")
    public AjaxResult recordExternalReply(@PathVariable Long sessionId,
            @Valid @RequestBody AiReplyRecordRequest request) {
        return success(customerService.recordExternalReply(sessionId, request));
    }

    @RequiresLogin
    @PostMapping("/{sessionId}/evaluation")
    public AjaxResult evaluate(@PathVariable Long sessionId, @Valid @RequestBody EvaluationRequest request) {
        return success(customerService.saveEvaluation(sessionId, request));
    }

    private TableDataInfo page(IPage<CustomerSession> page) {
        TableDataInfo result = new TableDataInfo();
        result.setCode(HttpStatus.SUCCESS);
        result.setMsg("查询成功");
        result.setRows(page.getRecords());
        result.setTotal(page.getTotal());
        return result;
    }
}
