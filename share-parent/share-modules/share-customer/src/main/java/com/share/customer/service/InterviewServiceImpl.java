package com.share.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.share.common.core.exception.ServiceException;
import com.share.common.security.utils.SecurityUtils;
import com.share.customer.domain.interview.InterviewCodeSubmission;
import com.share.customer.domain.interview.InterviewReport;
import com.share.customer.domain.interview.InterviewSession;
import com.share.customer.domain.interview.InterviewTurn;
import com.share.customer.domain.interview.UserResume;
import com.share.customer.domain.interview.dto.StartInterviewRequest;
import com.share.customer.domain.interview.dto.SubmitAnswerRequest;
import com.share.customer.domain.interview.dto.SubmitCodeRequest;
import com.share.customer.mapper.InterviewCodeMapper;
import com.share.customer.mapper.InterviewReportMapper;
import com.share.customer.mapper.InterviewSessionMapper;
import com.share.customer.mapper.InterviewTurnMapper;
import com.share.customer.service.support.InterviewEvaluator;
import com.share.customer.service.support.InterviewQuestionEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 全真沉浸式 AI 模拟面试与职涯评测核心业务实现
 */
@Slf4j
@Service
public class InterviewServiceImpl implements IInterviewService {

    private final InterviewSessionMapper sessionMapper;
    private final InterviewTurnMapper turnMapper;
    private final InterviewCodeMapper codeMapper;
    private final InterviewReportMapper reportMapper;
    private final IUserResumeService userResumeService;
    private final InterviewQuestionEngine questionEngine;
    private final InterviewEvaluator evaluator;

    public InterviewServiceImpl(
            InterviewSessionMapper sessionMapper,
            InterviewTurnMapper turnMapper,
            InterviewCodeMapper codeMapper,
            InterviewReportMapper reportMapper,
            IUserResumeService userResumeService,
            InterviewQuestionEngine questionEngine,
            InterviewEvaluator evaluator) {
        this.sessionMapper = sessionMapper;
        this.turnMapper = turnMapper;
        this.codeMapper = codeMapper;
        this.reportMapper = reportMapper;
        this.userResumeService = userResumeService;
        this.questionEngine = questionEngine;
        this.evaluator = evaluator;
    }

    @Override
    @Transactional
    public InterviewSession startSession(StartInterviewRequest request) {
        Long userId = currentUserId();
        String userName = currentUserName();
        LocalDateTime now = LocalDateTime.now();

        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setUserName(userName);
        session.setTargetJob(InterviewQuestionEngine.safeTruncate(StringUtils.hasText(request.getTargetJob()) ? request.getTargetJob().trim() : "Java高级开发工程师", 64));
        session.setCompanyTarget(InterviewQuestionEngine.safeTruncate(StringUtils.hasText(request.getCompanyTarget()) ? request.getCompanyTarget().trim() : "大厂通用", 64));
        session.setInterviewerStyle(InterviewQuestionEngine.safeTruncate(StringUtils.hasText(request.getInterviewerStyle()) ? request.getInterviewerStyle().trim() : "p7_architect", 64));
        session.setStatus(1); // 进行中
        session.setCurrentTurn(1);
        int totalTurns = request.getTotalTurns() != null && request.getTotalTurns() >= 5 ? Math.min(request.getTotalTurns(), 30) : 20;
        session.setTotalTurns(totalTurns);
        session.setDurationSeconds(0);
        session.setCreateTime(now);
        session.setUpdateTime(now);

        // 简历深度联动：若开启简历定制，则读取个人中心简历并注入面试快照
        if (request.getEnableResumeCustomization() != Boolean.FALSE) {
            UserResume resume = userResumeService.getResumeEntity(request.getResumeId(), userId);
            if (resume != null) {
                session.setResumeId(resume.getId());
                StringBuilder summary = new StringBuilder();
                summary.append("候选人目标=").append(resume.getTargetJob());
                if (resume.getMatchScore() != null && resume.getMatchScore() > 0) {
                    summary.append("，对标分=").append(resume.getMatchScore()).append("分(").append(resume.getMatchLevel()).append(")");
                }
                if (StringUtils.hasText(resume.getTechTags())) {
                    summary.append("，核心技能=").append(resume.getTechTags());
                }
                if (StringUtils.hasText(resume.getProjectHighlights())) {
                    summary.append("，重点项目=").append(resume.getProjectHighlights());
                }
                if (StringUtils.hasText(resume.getResumeGaps())) {
                    summary.append("，需重点追问的薄弱点=").append(resume.getResumeGaps());
                }
                session.setResumeSummary(summary.toString());
            }
        }

        sessionMapper.insert(session);

        // 生成第 1 轮开篇题目：严格为【环节一·自我介绍】
        String question = questionEngine.generateSelfIntroQuestion(session);

        InterviewTurn turn1 = new InterviewTurn();
        turn1.setSessionId(session.getId());
        turn1.setTurnNum(1);
        turn1.setDimension(InterviewQuestionEngine.safeTruncate("【环节一·自我介绍】职业背景与综合素质", 64));
        turn1.setQuestion(InterviewQuestionEngine.safeTruncate(question, 1000));
        turn1.setStandardReference(questionEngine.generateSelfIntroStandardRef(session));
        turn1.setDepthLevel(1);
        turn1.setStage(1);
        turn1.setStageName("环节一：自我介绍");
        turn1.setCreateTime(now);
        turnMapper.insert(turn1);

        session.setTurns(List.of(turn1));
        return session;
    }

    @Override
    public InterviewTurn submitAnswer(SubmitAnswerRequest request) {
        if (request.getSessionId() == null || request.getTurnId() == null) {
            throw new ServiceException("请求参数不完整");
        }
        InterviewSession session = sessionMapper.selectById(request.getSessionId());
        if (session == null) {
            throw new ServiceException("面试场次不存在");
        }
        if (session.getStatus() != 1) {
            throw new ServiceException("该面试场次已结束或已终止");
        }
        assertOwner(session);

        if (isSessionStagnant(session, 2)) {
            deleteSessionCascade(Collections.singletonList(session.getId()));
            throw new ServiceException("该面试场次停滞超过2小时已超时，记录已自动清理");
        }

        InterviewTurn currentTurn = turnMapper.selectById(request.getTurnId());
        if (currentTurn == null || !currentTurn.getSessionId().equals(session.getId())) {
            throw new ServiceException("问答轮次不存在或不匹配");
        }
        if (StringUtils.hasText(currentTurn.getUserAnswer())) {
            throw new ServiceException("本轮问题已作答，请勿重复提交");
        }

        String rawAnswer = request.getUserAnswer();
        String answer = StringUtils.hasText(rawAnswer) ? rawAnswer.trim() : "";
        if (!StringUtils.hasText(answer)) {
            throw new ServiceException("作答内容不能为空");
        }

        LocalDateTime now = LocalDateTime.now();

        // 幂等防重并发护栏：乐观锁条件更新 (BUG-3)
        int updated = turnMapper.update(null, new LambdaUpdateWrapper<InterviewTurn>()
                .set(InterviewTurn::getUserAnswer, answer)
                .set(InterviewTurn::getAnswerTime, now)
                .eq(InterviewTurn::getId, currentTurn.getId())
                .isNull(InterviewTurn::getUserAnswer));
        if (updated == 0) {
            throw new ServiceException("本轮问题已在处理或已提交，请勿重复操作");
        }
        currentTurn.setUserAnswer(answer);
        currentTurn.setAnswerTime(now);

        // 1. Qdrant 影子语义检定 (BUG-22)
        InterviewEvaluator.SemanticHit hit = evaluator.searchSemanticKnowledge(currentTurn.getQuestion() + " " + answer);
        if (hit != null) {
            if (currentTurn.getMatchedKnowledgeId() == null) {
                currentTurn.setMatchedKnowledgeId(hit.id());
            }
            if (!StringUtils.hasText(currentTurn.getStandardReference())) {
                currentTurn.setStandardReference(hit.answer());
            }
        }

        // 2. AI 面试官多维评分与点评
        String standardKnowledge = StringUtils.hasText(currentTurn.getStandardReference())
                ? currentTurn.getStandardReference()
                : (hit != null ? hit.answer() : null);
        InterviewEvaluator.TurnEvaluation evaluation = evaluator.evaluateTurnAnswer(session, currentTurn, standardKnowledge);
        currentTurn.setAiFeedback(evaluation.feedback());
        currentTurn.setTurnScore(evaluation.score());
        if (!StringUtils.hasText(currentTurn.getStandardReference()) && StringUtils.hasText(evaluation.standardRef())) {
            currentTurn.setStandardReference(evaluation.standardRef());
        }
        turnMapper.updateById(currentTurn);

        // 3. 推进或生成下一题
        advanceTurnOrFinish(session, currentTurn, evaluation.score(), evaluation.feedback());

        int total = session.getTotalTurns() != null ? session.getTotalTurns() : 20;
        int st = InterviewQuestionEngine.resolveStage(currentTurn.getTurnNum() != null ? currentTurn.getTurnNum() : 1, total);
        currentTurn.setStage(st);
        currentTurn.setStageName(InterviewQuestionEngine.resolveStageName(st));

        return currentTurn;
    }

    @Override
    @Transactional
    public InterviewCodeSubmission submitCode(SubmitCodeRequest request) {
        if (request.getSessionId() == null) {
            throw new ServiceException("场次ID不能为空");
        }
        InterviewSession session = sessionMapper.selectById(request.getSessionId());
        if (session == null) {
            throw new ServiceException("面试场次不存在");
        }
        if (session.getStatus() != 1) {
            throw new ServiceException("该面试场次已结束或已终止");
        }
        assertOwner(session);

        if (isSessionStagnant(session, 2)) {
            deleteSessionCascade(Collections.singletonList(session.getId()));
            throw new ServiceException("该面试场次停滞超过2小时已超时，记录已自动清理");
        }

        InterviewCodeSubmission submission = new InterviewCodeSubmission();
        submission.setSessionId(session.getId());
        submission.setTurnId(request.getTurnId());
        submission.setProblemTitle(InterviewQuestionEngine.safeTruncate(StringUtils.hasText(request.getProblemTitle()) ? request.getProblemTitle().trim() : "算法代码手撕", 128));
        submission.setLanguage(StringUtils.hasText(request.getLanguage()) ? request.getLanguage().trim().toLowerCase() : "java");
        submission.setUserCode(request.getUserCode() != null ? request.getUserCode() : "");
        submission.setCreateTime(LocalDateTime.now());

        // 执行 AI 沙箱与架构异味审计
        evaluator.auditCodeSubmission(submission);
        codeMapper.insert(submission);

        // 回填当前轮次作答内容与沙箱实测成绩，推进轮次 (BUG-1, BUG-2)
        InterviewTurn turn = request.getTurnId() != null ? turnMapper.selectById(request.getTurnId()) : null;
        if (turn == null && session.getCurrentTurn() != null) {
            turn = turnMapper.selectOne(new LambdaQueryWrapper<InterviewTurn>()
                    .eq(InterviewTurn::getSessionId, session.getId())
                    .eq(InterviewTurn::getTurnNum, session.getCurrentTurn()));
        }
        if (turn != null) {
            turn.setUserAnswer(request.getUserCode());
            int total = submission.getTotalTestCases() != null ? submission.getTotalTestCases() : 10;
            int passed = submission.getPassedTestCases() != null ? submission.getPassedTestCases() : 0;
            int codeScore = total > 0 ? (int) Math.round((double) passed / total * 100) : 80;
            turn.setTurnScore(codeScore);
            turn.setAiFeedback("【沙箱状态】： " + submission.getExecutionStatus() + "；【复杂度】：时间 " 
                    + submission.getTimeComplexity() + "，空间 " + submission.getSpaceComplexity() 
                    + "；【通过用例】： " + passed + "/" + total 
                    + "；【代码异味建议】： " + submission.getCodeSmells());
            turn.setAnswerTime(LocalDateTime.now());
            turnMapper.updateById(turn);

            advanceTurnOrFinish(session, turn, codeScore, turn.getAiFeedback());
        } else {
            int totalTurns = session.getTotalTurns() != null ? session.getTotalTurns() : 20;
            if (session.getCurrentTurn() != null && session.getCurrentTurn() >= totalTurns) {
                finishSession(session.getId());
            }
        }

        return submission;
    }

    @Override
    @Transactional
    public InterviewReport finishSession(Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new ServiceException("面试场次不存在");
        }
        assertOwner(session);

        InterviewReport existReport = reportMapper.selectOne(new LambdaQueryWrapper<InterviewReport>()
                .eq(InterviewReport::getSessionId, session.getId()));
        if (existReport != null) {
            return existReport;
        }

        LocalDateTime now = LocalDateTime.now();
        session.setStatus(2); // 已完成
        if (session.getCreateTime() != null) {
            session.setDurationSeconds((int) Math.max(0, Duration.between(session.getCreateTime(), now).getSeconds()));
        }
        session.setUpdateTime(now);

        List<InterviewTurn> turns = turnMapper.selectList(new LambdaQueryWrapper<InterviewTurn>()
                .eq(InterviewTurn::getSessionId, session.getId())
                .orderByAsc(InterviewTurn::getTurnNum));
        List<InterviewCodeSubmission> codes = codeMapper.selectList(new LambdaQueryWrapper<InterviewCodeSubmission>()
                .eq(InterviewCodeSubmission::getSessionId, session.getId()));

        if (codes != null && !codes.isEmpty()) {
            for (InterviewCodeSubmission c : codes) {
                for (InterviewTurn t : turns) {
                    if ((t.getTurnNum() != null && t.getTurnNum().equals(session.getTotalTurns()))
                            || (t.getDimension() != null && t.getDimension().contains("算法"))) {
                        if (!StringUtils.hasText(t.getUserAnswer())) {
                            t.setUserAnswer(c.getUserCode());
                            int total = c.getTotalTestCases() != null ? c.getTotalTestCases() : 10;
                            int passed = c.getPassedTestCases() != null ? c.getPassedTestCases() : 0;
                            int codeScore = total > 0 ? (int) Math.round((double) passed / total * 100) : 80;
                            t.setTurnScore(codeScore);
                            t.setAiFeedback("【沙箱状态】： " + c.getExecutionStatus() + "；【复杂度】：时间 " 
                                    + c.getTimeComplexity() + "，空间 " + c.getSpaceComplexity() 
                                    + "；【通过用例】： " + passed + "/" + total);
                            t.setAnswerTime(now);
                            turnMapper.updateById(t);
                        }
                        break;
                    }
                }
            }
        }

        int answeredCount = 0;
        double sum = 0;
        for (InterviewTurn t : turns) {
            if (StringUtils.hasText(t.getUserAnswer()) && t.getTurnScore() != null) {
                sum += t.getTurnScore();
                answeredCount++;
            }
        }

        int finalScore;
        if (answeredCount == 0) {
            finalScore = 0;
        } else {
            int plannedTurns = Math.max(1, session.getTotalTurns() != null ? session.getTotalTurns() : 20);
            double avgAnswered = sum / answeredCount;
            if (answeredCount < plannedTurns) {
                finalScore = (int) Math.round(avgAnswered * ((double) answeredCount / plannedTurns));
            } else {
                finalScore = (int) Math.round(avgAnswered);
            }
        }
        session.setScore(finalScore);
        sessionMapper.updateById(session);

        InterviewReport report = evaluator.generateFinalReport(session, turns, codes, finalScore);
        report.setSessionId(session.getId());
        report.setCreateTime(now);
        reportMapper.insert(report);

        return report;
    }

    @Override
    public InterviewSession getSessionDetail(Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new ServiceException("面试场次不存在");
        }
        assertOwner(session);

        if (isSessionStagnant(session, 2)) {
            deleteSessionCascade(Collections.singletonList(session.getId()));
            throw new ServiceException("该面试场次停滞超过2小时已超时，记录已自动清理");
        }

        List<InterviewTurn> turns = turnMapper.selectList(new LambdaQueryWrapper<InterviewTurn>()
                .eq(InterviewTurn::getSessionId, session.getId())
                .orderByAsc(InterviewTurn::getTurnNum));
        if (turns != null) {
            int total = session.getTotalTurns() != null ? session.getTotalTurns() : 20;
            for (InterviewTurn t : turns) {
                int st = InterviewQuestionEngine.resolveStage(t.getTurnNum() != null ? t.getTurnNum() : 1, total);
                t.setStage(st);
                t.setStageName(InterviewQuestionEngine.resolveStageName(st));
            }
        }
        session.setTurns(turns);

        List<InterviewCodeSubmission> codes = codeMapper.selectList(new LambdaQueryWrapper<InterviewCodeSubmission>()
                .eq(InterviewCodeSubmission::getSessionId, session.getId())
                .orderByDesc(InterviewCodeSubmission::getCreateTime));
        session.setCodeSubmissions(codes);

        InterviewReport report = reportMapper.selectOne(new LambdaQueryWrapper<InterviewReport>()
                .eq(InterviewReport::getSessionId, session.getId()));
        session.setReport(report);

        return session;
    }

    @Override
    public IPage<InterviewSession> listMySessions(long pageNum, long pageSize) {
        Long userId = currentUserId();
        try {
            cleanStagnantSessions(2);
        } catch (Exception e) {
            log.warn("查询列表前置清理超时场次异常: {}", e.getMessage());
        }

        Page<InterviewSession> page = new Page<>(pageNum < 1 ? 1 : pageNum, pageSize < 1 ? 10 : Math.min(pageSize, 50));
        LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getUserId, userId)
                .orderByDesc(InterviewSession::getCreateTime);
        IPage<InterviewSession> result = sessionMapper.selectPage(page, wrapper);
        List<InterviewSession> records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            List<Long> sessionIds = records.stream().map(InterviewSession::getId).filter(Objects::nonNull).toList();
            if (!sessionIds.isEmpty()) {
                List<InterviewReport> reports = reportMapper.selectList(new LambdaQueryWrapper<InterviewReport>()
                        .in(InterviewReport::getSessionId, sessionIds));
                Map<Long, InterviewReport> reportMap = reports.stream()
                        .collect(Collectors.toMap(InterviewReport::getSessionId, Function.identity(), (a, b) -> a));
                for (InterviewSession s : records) {
                    s.setReport(reportMap.get(s.getId()));
                }
            }
        }
        return result;
    }

    @Override
    @Transactional
    public void terminateSession(Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new ServiceException("面试场次不存在");
        }
        assertOwner(session);
        LocalDateTime now = LocalDateTime.now();
        session.setStatus(3);
        if (session.getCreateTime() != null) {
            session.setDurationSeconds((int) Math.max(0, Duration.between(session.getCreateTime(), now).getSeconds()));
        }
        session.setUpdateTime(now);
        sessionMapper.updateById(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId) {
        if (sessionId == null) {
            return;
        }
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return;
        }
        assertOwner(session);
        deleteSessionCascade(Collections.singletonList(sessionId));
        log.info("【AI模拟面试治理】学员主动删除面试场次成功: id={}", sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanStagnantSessions(int expireHours) {
        int hours = expireHours > 0 ? expireHours : 2;
        LocalDateTime threshold = LocalDateTime.now().minusHours(hours);
        List<InterviewSession> stagnantSessions = sessionMapper.selectList(new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getStatus, 1)
                .and(w -> w.lt(InterviewSession::getUpdateTime, threshold)
                        .or(ow -> ow.isNull(InterviewSession::getUpdateTime).lt(InterviewSession::getCreateTime, threshold))));
        if (stagnantSessions == null || stagnantSessions.isEmpty()) {
            return 0;
        }
        List<Long> expiredIds = stagnantSessions.stream().map(InterviewSession::getId).filter(Objects::nonNull).toList();
        if (!expiredIds.isEmpty()) {
            deleteSessionCascade(expiredIds);
            log.info("【AI模拟面试超时治理】成功自动清理 {} 个停滞超过 {} 小时的进行中场次: {}", expiredIds.size(), hours, expiredIds);
        }
        return expiredIds.size();
    }

    @Override
    public String getSocraticHint(Long sessionId, Long turnId, int hintLevel, String currentCode) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new ServiceException("面试场次不存在");
        }
        assertOwner(session);

        if (session.getStatus() != null && session.getStatus() != 1) {
            throw new ServiceException("当前面试场次已结束，无法继续获取考官提示");
        }

        InterviewTurn turn = null;
        if (turnId != null) {
            turn = turnMapper.selectById(turnId);
            if (turn != null && !turn.getSessionId().equals(session.getId())) {
                throw new ServiceException("问答轮次不匹配当前面试场次");
            }
        }
        if (turn == null && session.getCurrentTurn() != null) {
            turn = turnMapper.selectOne(new LambdaQueryWrapper<InterviewTurn>()
                    .eq(InterviewTurn::getSessionId, session.getId())
                    .eq(InterviewTurn::getTurnNum, session.getCurrentTurn()));
        }

        String question = (turn != null && StringUtils.hasText(turn.getQuestion())) ? turn.getQuestion() : "通用算法与架构设计考题";
        return evaluator.generateSocraticHint(question, currentCode, hintLevel);
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduleCleanStagnantSessions() {
        try {
            int cleaned = cleanStagnantSessions(2);
            if (cleaned > 0) {
                log.info("【AI模拟面试每日巡检】凌晨任务已自动清理 {} 个停滞超过2小时的过期场次", cleaned);
            }
        } catch (Exception e) {
            log.warn("【AI模拟面试每日巡检】清理超时场次异常: {}", e.getMessage());
        }
    }

    private void advanceTurnOrFinish(InterviewSession session, InterviewTurn currentTurn, Integer lastScore, String lastFeedback) {
        int currentTurnNum = (currentTurn != null && currentTurn.getTurnNum() != null) ? currentTurn.getTurnNum() : 1;
        int nextTurnNum = currentTurnNum + 1;
        int totalTurns = (session.getTotalTurns() != null && session.getTotalTurns() >= 5) ? session.getTotalTurns() : 20;
        if (nextTurnNum > totalTurns) {
            finishSession(session.getId());
            return;
        }

        int nextStage = InterviewQuestionEngine.resolveStage(nextTurnNum, totalTurns);
        InterviewQuestionEngine.JobTrack track = questionEngine.detectJobTrack(session.getTargetJob());
        String nextDimension;
        String nextQuestion;
        String nextStandardReference = null;
        Long matchedKnowledgeId = null;
        int nextDepth = 1;

        int stage2End = totalTurns <= 10 ? Math.max(2, totalTurns / 2) : 11;

        if (nextStage == 2) {
            int fundamentalIndex = nextTurnNum - 1;
            List<InterviewTurn> existTurns = turnMapper.selectList(new LambdaQueryWrapper<InterviewTurn>()
                    .eq(InterviewTurn::getSessionId, session.getId()));
            InterviewQuestionEngine.XiaolinQuestionResult xq = questionEngine.resolveXiaolinFundamentalQuestion(
                    session, track, fundamentalIndex, existTurns, lastScore);
            nextDimension = xq.dimension();
            nextQuestion = xq.question();
            nextStandardReference = xq.standardReference();
            matchedKnowledgeId = xq.knowledgeId();
            nextDepth = xq.depthLevel();
        } else {
            int projectIndex = Math.max(1, nextTurnNum - stage2End);
            if (nextTurnNum == totalTurns) {
                nextDimension = "【环节三·终局挑战】算法设计与工程手撕";
                nextDepth = 3;
                nextQuestion = questionEngine.resolveCodingProblem(track);
                nextStandardReference = questionEngine.resolveCodingStandardRef(track);
            } else {
                nextDepth = questionEngine.resolveDrillDepth(projectIndex, lastScore);
                InterviewQuestionEngine.ProjectDrillResult pdr = questionEngine.generateProjectDrillQuestion(
                        session, track, projectIndex, nextDepth, currentTurn, lastScore, lastFeedback);
                nextDimension = "【环节三·项目深挖】" + pdr.dimension();
                nextQuestion = pdr.question();
                nextStandardReference = pdr.standardReference();
            }
        }

        InterviewTurn nextTurn = new InterviewTurn();
        nextTurn.setSessionId(session.getId());
        nextTurn.setTurnNum(nextTurnNum);
        nextTurn.setDimension(InterviewQuestionEngine.safeTruncate(nextDimension, 64));
        nextTurn.setQuestion(InterviewQuestionEngine.safeTruncate(nextQuestion, 1000));
        nextTurn.setStandardReference(nextStandardReference);
        nextTurn.setMatchedKnowledgeId(matchedKnowledgeId);
        nextTurn.setDepthLevel(nextDepth);
        nextTurn.setStage(nextStage);
        nextTurn.setStageName(InterviewQuestionEngine.resolveStageName(nextStage));
        nextTurn.setCreateTime(LocalDateTime.now());
        turnMapper.insert(nextTurn);

        session.setCurrentTurn(nextTurnNum);
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    private boolean isSessionStagnant(InterviewSession session, int hours) {
        if (session == null || session.getStatus() == null || session.getStatus() != 1) {
            return false;
        }
        LocalDateTime lastActivity = session.getUpdateTime() != null ? session.getUpdateTime() : session.getCreateTime();
        return lastActivity != null && lastActivity.plusHours(hours).isBefore(LocalDateTime.now());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSessionCascade(List<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }
        turnMapper.delete(new LambdaQueryWrapper<InterviewTurn>().in(InterviewTurn::getSessionId, sessionIds));
        codeMapper.delete(new LambdaQueryWrapper<InterviewCodeSubmission>().in(InterviewCodeSubmission::getSessionId, sessionIds));
        reportMapper.delete(new LambdaQueryWrapper<InterviewReport>().in(InterviewReport::getSessionId, sessionIds));
        sessionMapper.deleteBatchIds(sessionIds);
    }

    private void assertOwner(InterviewSession session) {
        Long current = currentUserId();
        if (session == null || session.getUserId() == null || !current.equals(session.getUserId())) {
            throw new ServiceException("无权访问该面试场次");
        }
    }

    private Long currentUserId() {
        Long id = SecurityUtils.getUserId();
        if (id == null || id <= 0) {
            throw new ServiceException("请先登录后再进行操作");
        }
        return id;
    }

    private String currentUserName() {
        String name = SecurityUtils.getUsername();
        return StringUtils.hasText(name) ? name : "学员";
    }
}

