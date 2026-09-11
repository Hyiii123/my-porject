package com.share.customer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.share.customer.domain.interview.InterviewCodeSubmission;
import com.share.customer.domain.interview.InterviewReport;
import com.share.customer.domain.interview.InterviewSession;
import com.share.customer.domain.interview.InterviewTurn;
import com.share.customer.domain.interview.dto.StartInterviewRequest;
import com.share.customer.domain.interview.dto.SubmitAnswerRequest;
import com.share.customer.domain.interview.dto.SubmitCodeRequest;

/**
 * 全真沉浸式 AI 模拟面试与职涯评测服务接口。
 */
public interface IInterviewService {

    /**
     * 开启全新面试场次并生成第一轮开篇考题。
     */
    InterviewSession startSession(StartInterviewRequest request);

    /**
     * 提交本轮回答，触发 Qdrant 影子检定与三级剥洋葱深度追问。
     */
    InterviewTurn submitAnswer(SubmitAnswerRequest request);

    /**
     * 提交算法代码，触发沙箱语法与边界评测、时空复杂度推演及架构异味重构。
     */
    InterviewCodeSubmission submitCode(SubmitCodeRequest request);

    /**
     * 终局裁定：生成多维能力诊断大屏报告（阿里P6/P7职级对标、六维雷达、STAR话术重塑）。
     */
    InterviewReport finishSession(Long sessionId);

    /**
     * 获取面试场次完整详情（包含所有轮次、代码提交与终局报告）。
     */
    InterviewSession getSessionDetail(Long sessionId);

    /**
     * 分页查询当前用户的历史面试记录。
     */
    IPage<InterviewSession> listMySessions(long pageNum, long pageSize);

    /**
     * 中途主动终止面试场次。
     */
    void terminateSession(Long sessionId);
}
