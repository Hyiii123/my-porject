package com.share.customer.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.security.annotation.RequiresLogin;
import com.share.customer.domain.interview.InterviewCodeSubmission;
import com.share.customer.domain.interview.InterviewReport;
import com.share.customer.domain.interview.InterviewSession;
import com.share.customer.domain.interview.InterviewTurn;
import com.share.customer.domain.interview.dto.ResumeAnalysisRequest;
import com.share.customer.domain.interview.dto.ResumeSaveRequest;
import com.share.customer.domain.interview.dto.StartInterviewRequest;
import com.share.customer.domain.interview.dto.SubmitAnswerRequest;
import com.share.customer.domain.interview.dto.SubmitCodeRequest;
import com.share.customer.domain.interview.vo.ResumeAnalysisVO;
import com.share.customer.service.IInterviewService;
import com.share.customer.service.IUserResumeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 全真沉浸式 AI 模拟面试与职涯评测控制器。
 */
@RestController
@RequestMapping("/interview")
public class InterviewController extends BaseController {

    private final IInterviewService interviewService;
    private final IUserResumeService resumeService;

    public InterviewController(IInterviewService interviewService, IUserResumeService resumeService) {
        this.interviewService = interviewService;
        this.resumeService = resumeService;
    }

    /**
     * 开启全新面试场次并生成第一轮开题。
     */
    @RequiresLogin
    @PostMapping("/start")
    public AjaxResult start(@Valid @RequestBody(required = false) StartInterviewRequest request) {
        InterviewSession session = interviewService.startSession(request != null ? request : new StartInterviewRequest());
        return success(session);
    }

    /**
     * 提交本轮答题（触发 Qdrant 影子检定与三级剥洋葱深度追问）。
     */
    @RequiresLogin
    @PostMapping("/answer")
    public AjaxResult submitAnswer(@Valid @RequestBody SubmitAnswerRequest request) {
        InterviewTurn turn = interviewService.submitAnswer(request);
        return success(turn);
    }

    /**
     * 提交代码手撕（触发沙箱语法与边界评测、时空复杂度推演与异味重构）。
     */
    @RequiresLogin
    @PostMapping("/code")
    public AjaxResult submitCode(@Valid @RequestBody SubmitCodeRequest request) {
        InterviewCodeSubmission code = interviewService.submitCode(request);
        return success(code);
    }

    /**
     * 终局交卷 / 裁定（生成多维能力诊断大屏报告）。
     */
    @RequiresLogin
    @PostMapping("/finish/{sessionId}")
    public AjaxResult finish(@PathVariable Long sessionId) {
        InterviewReport report = interviewService.finishSession(sessionId);
        return success(report);
    }

    /**
     * 获取面试场次完整详情（包含历史问答、代码提交与报告）。
     */
    @RequiresLogin
    @GetMapping("/{sessionId}")
    public AjaxResult detail(@PathVariable Long sessionId) {
        InterviewSession session = interviewService.getSessionDetail(sessionId);
        return success(session);
    }

    /**
     * 分页获取当前用户历史面试场次。
     */
    @RequiresLogin
    @GetMapping("/my")
    public TableDataInfo myList(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        IPage<InterviewSession> page = interviewService.listMySessions(pageNum, pageSize);
        return page(page);
    }

    /**
     * 中途终止面试场次。
     */
    @RequiresLogin
    @PostMapping("/terminate/{sessionId}")
    public AjaxResult terminate(@PathVariable Long sessionId) {
        interviewService.terminateSession(sessionId);
        return success();
    }

    /**
     * 获取当前登录用户的个人简历与最新 AI 诊断报告。
     */
    @RequiresLogin
    @GetMapping("/resume/my")
    public AjaxResult getMyResume() {
        ResumeAnalysisVO vo = resumeService.getMyResume();
        return success(vo);
    }

    /**
     * 保存/更新个人简历文本内容。
     */
    @RequiresLogin
    @PostMapping("/resume/save")
    public AjaxResult saveResume(@Valid @RequestBody ResumeSaveRequest request) {
        ResumeAnalysisVO vo = resumeService.saveResume(request);
        return success(vo);
    }

    /**
     * 触发 AI 简历深度对标与诊断分析。
     */
    @RequiresLogin
    @PostMapping("/resume/analyze")
    public AjaxResult analyzeResume(@Valid @RequestBody ResumeAnalysisRequest request) {
        ResumeAnalysisVO vo = resumeService.analyzeResume(request);
        return success(vo);
    }

    /**
     * 上传简历附件提取文本。
     */
    @RequiresLogin
    @PostMapping("/resume/upload")
    public AjaxResult uploadResume(@RequestParam("file") MultipartFile file) {
        String content = resumeService.extractResumeText(file);
        Map<String, String> data = new HashMap<>();
        data.put("fileName", file.getOriginalFilename());
        data.put("content", content);
        return success(data);
    }

    /**
     * 清空个人中心简历。
     */
    @RequiresLogin
    @DeleteMapping("/resume/clear")
    public AjaxResult clearResume() {
        resumeService.clearResume();
        return success();
    }

    private TableDataInfo page(IPage<InterviewSession> page) {
        TableDataInfo result = new TableDataInfo();
        result.setCode(com.share.common.core.constant.HttpStatus.SUCCESS);
        result.setMsg("查询成功");
        result.setRows(page.getRecords());
        result.setTotal(page.getTotal());
        return result;
    }
}
