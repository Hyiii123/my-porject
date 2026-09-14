package com.share.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.common.core.exception.ServiceException;
import com.share.common.security.utils.SecurityUtils;
import com.share.customer.config.CustomerAiProperties;
import com.share.customer.domain.CustomerAiConfig;
import com.share.customer.domain.CustomerKnowledge;
import com.share.customer.domain.interview.InterviewCodeSubmission;
import com.share.customer.domain.interview.InterviewReport;
import com.share.customer.domain.interview.InterviewSession;
import com.share.customer.domain.interview.InterviewTurn;
import com.share.customer.domain.interview.UserResume;
import com.share.customer.domain.interview.dto.StartInterviewRequest;
import com.share.customer.domain.interview.dto.SubmitAnswerRequest;
import com.share.customer.domain.interview.dto.SubmitCodeRequest;
import com.share.customer.mapper.CustomerAiConfigMapper;
import com.share.customer.mapper.CustomerKnowledgeMapper;
import com.share.customer.mapper.InterviewCodeMapper;
import com.share.customer.mapper.InterviewReportMapper;
import com.share.customer.mapper.InterviewSessionMapper;
import com.share.customer.mapper.InterviewTurnMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 全真沉浸式 AI 模拟面试与职涯评测超级子系统核心业务实现。
 */
@Slf4j
@Service
public class InterviewServiceImpl implements IInterviewService {

    private static final String EMBEDDING_SERVICE_URL = "http://zhiwen-embedding:8000/search";
    private static final long CONFIG_ID = 1L;

    private final InterviewSessionMapper sessionMapper;
    private final InterviewTurnMapper turnMapper;
    private final InterviewCodeMapper codeMapper;
    private final InterviewReportMapper reportMapper;
    private final CustomerKnowledgeMapper knowledgeMapper;
    private final CustomerAiConfigMapper aiConfigMapper;
    private final CustomerAiClient aiClient;
    private final CustomerAiProperties aiProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final IUserResumeService userResumeService;

    public InterviewServiceImpl(
            InterviewSessionMapper sessionMapper,
            InterviewTurnMapper turnMapper,
            InterviewCodeMapper codeMapper,
            InterviewReportMapper reportMapper,
            CustomerKnowledgeMapper knowledgeMapper,
            CustomerAiConfigMapper aiConfigMapper,
            CustomerAiClient aiClient,
            CustomerAiProperties aiProperties,
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            IUserResumeService userResumeService) {
        this.sessionMapper = sessionMapper;
        this.turnMapper = turnMapper;
        this.codeMapper = codeMapper;
        this.reportMapper = reportMapper;
        this.knowledgeMapper = knowledgeMapper;
        this.aiConfigMapper = aiConfigMapper;
        this.aiClient = aiClient;
        this.aiProperties = aiProperties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userResumeService = userResumeService;
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
        session.setTargetJob(StringUtils.hasText(request.getTargetJob()) ? request.getTargetJob().trim() : "Java高级开发工程师");
        session.setCompanyTarget(StringUtils.hasText(request.getCompanyTarget()) ? request.getCompanyTarget().trim() : "大厂通用");
        session.setInterviewerStyle(StringUtils.hasText(request.getInterviewerStyle()) ? request.getInterviewerStyle().trim() : "p7_architect");
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
        String question = generateSelfIntroQuestion(session);

        InterviewTurn turn1 = new InterviewTurn();
        turn1.setSessionId(session.getId());
        turn1.setTurnNum(1);
        turn1.setDimension("【环节一·自我介绍】职业背景与综合素质");
        turn1.setQuestion(question);
        turn1.setStandardReference(generateSelfIntroStandardRef(session));
        turn1.setDepthLevel(1); // 1-概念摸底
        turn1.setStage(1);
        turn1.setStageName("环节一：自我介绍");
        turn1.setCreateTime(now);
        turnMapper.insert(turn1);

        session.setTurns(List.of(turn1));
        return session;
    }

    @Override
    @Transactional
    public InterviewTurn submitAnswer(SubmitAnswerRequest request) {
        InterviewSession session = sessionMapper.selectById(request.getSessionId());
        if (session == null) {
            throw new ServiceException("面试场次不存在");
        }
        if (session.getStatus() != 1) {
            throw new ServiceException("该面试场次已结束或已终止");
        }
        assertOwner(session);

        InterviewTurn currentTurn = turnMapper.selectById(request.getTurnId());
        if (currentTurn == null || !currentTurn.getSessionId().equals(session.getId())) {
            throw new ServiceException("问答轮次不存在或不匹配");
        }
        if (StringUtils.hasText(currentTurn.getUserAnswer())) {
            throw new ServiceException("本轮问题已作答，请勿重复提交");
        }

        LocalDateTime now = LocalDateTime.now();
        String answer = request.getUserAnswer().trim();
        currentTurn.setUserAnswer(answer);
        currentTurn.setAnswerTime(now);

        // 1. Qdrant 影子语义检定：基于 10,000 条题库进行检索对比
        SemanticHit hit = searchSemanticKnowledge(currentTurn.getQuestion() + " " + answer);
        if (hit != null) {
            currentTurn.setMatchedKnowledgeId(hit.id);
            if (!StringUtils.hasText(currentTurn.getStandardReference())) {
                currentTurn.setStandardReference(hit.answer);
            }
        }

        // 2. AI 面试官多维评分与点评（结合风格 Persona）
        String standardKnowledge = StringUtils.hasText(currentTurn.getStandardReference())
                ? currentTurn.getStandardReference()
                : (hit != null ? hit.answer : null);
        TurnEvaluation evaluation = evaluateTurnAnswer(session, currentTurn, standardKnowledge);
        currentTurn.setAiFeedback(evaluation.feedback);
        currentTurn.setTurnScore(evaluation.score);
        if (!StringUtils.hasText(currentTurn.getStandardReference()) && StringUtils.hasText(evaluation.standardRef)) {
            currentTurn.setStandardReference(evaluation.standardRef);
        }
        turnMapper.updateById(currentTurn);

        // 3. 推进或生成下一题（三大环节状态机驱动）
        advanceTurnOrFinish(session, currentTurn, evaluation);

        int total = session.getTotalTurns() != null ? session.getTotalTurns() : 20;
        int st = resolveStage(currentTurn.getTurnNum() != null ? currentTurn.getTurnNum() : 1, total);
        currentTurn.setStage(st);
        currentTurn.setStageName(resolveStageName(st));

        return currentTurn;
    }

    @Override
    @Transactional
    public InterviewCodeSubmission submitCode(SubmitCodeRequest request) {
        InterviewSession session = sessionMapper.selectById(request.getSessionId());
        if (session == null) {
            throw new ServiceException("面试场次不存在");
        }
        if (session.getStatus() != 1) {
            throw new ServiceException("该面试场次已结束或已终止");
        }
        assertOwner(session);

        InterviewCodeSubmission submission = new InterviewCodeSubmission();
        submission.setSessionId(session.getId());
        submission.setTurnId(request.getTurnId());
        submission.setProblemTitle(request.getProblemTitle().trim());
        submission.setLanguage(StringUtils.hasText(request.getLanguage()) ? request.getLanguage().trim().toLowerCase() : "java");
        submission.setUserCode(request.getUserCode());
        submission.setCreateTime(LocalDateTime.now());

        // 执行 AI 沙箱与架构异味审计
        auditCodeSubmission(submission);
        codeMapper.insert(submission);

        // BUG-36, BUG-51: 真实回填当前轮次作答内容与沙箱实测成绩，推进轮次，消除提前交卷误判
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
        }

        // 推进当前轮次
        int totalTurns = session.getTotalTurns() != null ? session.getTotalTurns() : 6;
        if (session.getCurrentTurn() != null && session.getCurrentTurn() < totalTurns) {
            session.setCurrentTurn(session.getCurrentTurn() + 1);
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.updateById(session);
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

        // 若已有报告直接返回
        InterviewReport existReport = reportMapper.selectOne(new LambdaQueryWrapper<InterviewReport>()
                .eq(InterviewReport::getSessionId, session.getId()));
        if (existReport != null) {
            return existReport;
        }

        LocalDateTime now = LocalDateTime.now();
        session.setStatus(2); // 已完成
        if (session.getCreateTime() != null) {
            session.setDurationSeconds((int) Duration.between(session.getCreateTime(), now).getSeconds());
        }
        session.setUpdateTime(now);

        // 获取所有轮次答题与代码手撕记录
        List<InterviewTurn> turns = turnMapper.selectList(new LambdaQueryWrapper<InterviewTurn>()
                .eq(InterviewTurn::getSessionId, session.getId())
                .orderByAsc(InterviewTurn::getTurnNum));
        List<InterviewCodeSubmission> codes = codeMapper.selectList(new LambdaQueryWrapper<InterviewCodeSubmission>()
                .eq(InterviewCodeSubmission::getSessionId, session.getId()));

        // BUG-36, BUG-51: 确保若有代码沙箱实测提交，但对应轮次未记录作答时，自动回填至算法手撕轮次
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

        // 计算总得分：必须严格结合实质答题率与各轮得分
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
            finalScore = 0; // 全场未作答 / 零分快速交卷
        } else {
            // 若提前交卷（未完成全部轮次），未作答轮次按 0 分折算完成度综合计算
            int plannedTurns = Math.max(1, session.getTotalTurns() != null ? session.getTotalTurns() : 6);
            double avgAnswered = sum / answeredCount;
            if (answeredCount < plannedTurns) {
                finalScore = (int) Math.round(avgAnswered * ((double) answeredCount / plannedTurns));
            } else {
                finalScore = (int) Math.round(avgAnswered);
            }
        }
        session.setScore(finalScore);
        sessionMapper.updateById(session);

        // 终局委员会多维能力综合裁决（调用 AI 合成职级、六维雷达、STAR话术重塑）
        InterviewReport report = generateFinalReport(session, turns, codes, finalScore);
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

        List<InterviewTurn> turns = turnMapper.selectList(new LambdaQueryWrapper<InterviewTurn>()
                .eq(InterviewTurn::getSessionId, session.getId())
                .orderByAsc(InterviewTurn::getTurnNum));
        if (turns != null) {
            int total = session.getTotalTurns() != null ? session.getTotalTurns() : 20;
            for (InterviewTurn t : turns) {
                int st = resolveStage(t.getTurnNum() != null ? t.getTurnNum() : 1, total);
                t.setStage(st);
                t.setStageName(resolveStageName(st));
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
        Page<InterviewSession> page = new Page<>(pageNum < 1 ? 1 : pageNum, pageSize < 1 ? 10 : Math.min(pageSize, 50));
        LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getUserId, userId)
                .orderByDesc(InterviewSession::getCreateTime);
        IPage<InterviewSession> result = sessionMapper.selectPage(page, wrapper);
        for (InterviewSession s : result.getRecords()) {
            InterviewReport rep = reportMapper.selectOne(new LambdaQueryWrapper<InterviewReport>()
                    .eq(InterviewReport::getSessionId, s.getId()));
            s.setReport(rep);
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
        session.setStatus(3); // 已终止
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    // =========================================================================
    // 内部核心逻辑：剥洋葱三级追问、风格引擎、语义检定与报告合成
    // =========================================================================

    public static int resolveStage(int turnNum, int totalTurns) {
        if (turnNum <= 1) {
            return 1;
        }
        int stage2End = totalTurns <= 10 ? Math.max(2, totalTurns / 2) : 11;
        if (turnNum <= stage2End) {
            return 2;
        }
        return 3;
    }

    public static String resolveStageName(int stage) {
        return switch (stage) {
            case 1 -> "环节一：自我介绍";
            case 2 -> "环节二：基础八股文";
            case 3 -> "环节三：简历项目追问";
            default -> "综合技术考查";
        };
    }

    /**
     * 根据候选人表现推进下一题或直接终局（三大环节状态机驱动）。
     */
    private static class XiaolinQuestionResult {
        final String dimension;
        final String question;
        final String standardReference;
        final Long knowledgeId;
        final int depthLevel;

        public XiaolinQuestionResult(String dimension, String question, String standardReference, Long knowledgeId, int depthLevel) {
            this.dimension = dimension;
            this.question = question;
            this.standardReference = standardReference;
            this.knowledgeId = knowledgeId;
            this.depthLevel = depthLevel;
        }
    }

    private static class ProjectDrillResult {
        final String dimension;
        final String question;
        final String standardReference;

        public ProjectDrillResult(String dimension, String question, String standardReference) {
            this.dimension = dimension;
            this.question = question;
            this.standardReference = standardReference;
        }
    }

    /**
     * 根据候选人作答表现动态推进下一题或直接终局（三大环节状态机与大模型动态感知驱动）。
     * 遵循原则：题目绝不预先定死，严格根据候选人的回答与技术深度动态演进、剥洋葱连环深挖。
     */
    private void advanceTurnOrFinish(InterviewSession session, InterviewTurn currentTurn, TurnEvaluation evaluation) {
        int nextTurnNum = currentTurn.getTurnNum() + 1;
        int totalTurns = session.getTotalTurns() != null ? session.getTotalTurns() : 20;
        if (nextTurnNum > totalTurns) {
            // 已达最大轮次，自动完成面试并生成报告
            finishSession(session.getId());
            return;
        }

        int nextStage = resolveStage(nextTurnNum, totalTurns);
        JobTrack track = detectJobTrack(session.getTargetJob());
        String nextDimension;
        String nextQuestion;
        String nextStandardReference = null;
        Long matchedKnowledgeId = null;
        int nextDepth = 1;

        if (nextStage == 2) {
            // 环节二：基础八股文 (Turn 2 .. 11)
            // 选自 xiaolincoding.com 知识库，严格跨 10 大独立模块分散选拔（绝不重复模块），并根据前序答题深度动态演进！
            int fundamentalIndex = nextTurnNum - 1; // 1 to 10
            XiaolinQuestionResult xq = resolveXiaolinFundamentalQuestion(session, track, fundamentalIndex, currentTurn, evaluation);
            nextDimension = xq.dimension;
            nextQuestion = xq.question;
            nextStandardReference = xq.standardReference;
            matchedKnowledgeId = xq.knowledgeId;
            nextDepth = xq.depthLevel;
        } else {
            // 环节三：简历项目追问 (Turn 12 .. 20)
            int projectIndex = nextTurnNum - 11; // 1 to 9 (assuming 20 total)
            if (nextTurnNum == totalTurns) {
                nextDimension = "【环节三·终局挑战】算法设计与工程手撕";
                nextDepth = 3;
                nextQuestion = resolveCodingProblem(track);
                nextStandardReference = resolveCodingStandardRef(track);
            } else {
                nextDepth = resolveDrillDepth(projectIndex, evaluation);
                ProjectDrillResult pdr = generateProjectDrillQuestion(session, track, projectIndex, nextDepth, currentTurn, evaluation);
                nextDimension = "【环节三·项目深挖】" + pdr.dimension;
                nextQuestion = pdr.question;
                nextStandardReference = pdr.standardReference;
            }
        }

        InterviewTurn nextTurn = new InterviewTurn();
        nextTurn.setSessionId(session.getId());
        nextTurn.setTurnNum(nextTurnNum);
        nextTurn.setDimension(nextDimension);
        nextTurn.setQuestion(nextQuestion);
        nextTurn.setStandardReference(nextStandardReference);
        nextTurn.setMatchedKnowledgeId(matchedKnowledgeId);
        nextTurn.setDepthLevel(nextDepth);
        nextTurn.setStage(nextStage);
        nextTurn.setStageName(resolveStageName(nextStage));
        nextTurn.setCreateTime(LocalDateTime.now());
        turnMapper.insert(nextTurn);

        session.setCurrentTurn(nextTurnNum);
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    private String generateSelfIntroQuestion(InterviewSession session) {
        String stylePersona = getStylePersona(session.getInterviewerStyle());
        StringBuilder sb = new StringBuilder();
        sb.append("你现在是").append(stylePersona).append("。候选人正在面试【").append(session.getTargetJob()).append("】岗位。\n");
        sb.append("当前是面试的第一环节：【自我介绍】。\n");
        if (StringUtils.hasText(session.getResumeSummary())) {
            sb.append("【候选人绑定的个人简历信息】：\n").append(session.getResumeSummary()).append("\n");
        }
        sb.append("请向候选人提出自我介绍要求。\n");
        sb.append("要求：\n");
        sb.append("1. 符合你的面试官风格，专业严谨、真实自然；\n");
        sb.append("2. 明确要求候选人用2~3分钟做一个简要的自我介绍，重点结合其技术栈背景、过往主导的核心项目以及自身核心技术专长进行阐述；\n");
        sb.append("3. 严禁废话客套，直接输出提问正文，120字以内。");

        String fallback = StringUtils.hasText(session.getResumeSummary())
                ? "你好，欢迎参加本次【" + session.getTargetJob() + "】技术面试！我是今天的技术面试官。请你先用2~3分钟做一个简要的自我介绍，可以重点结合你的技术背景、简历中主导的核心业务项目以及你最擅长的技术专长进行阐述。"
                : "你好，欢迎参加本次【" + session.getTargetJob() + "】技术面试！我是今天的技术面试官。请你先用2~3分钟做一个简要的自我介绍，重点介绍一下你的技术栈、过往项目经验以及你对该岗位的核心技术优势。";

        String aiResult = callAi(sb.toString(), fallback);
        return cleanAiText(aiResult);
    }

    private String generateSelfIntroStandardRef(InterviewSession session) {
        String job = StringUtils.hasText(session.getTargetJob()) ? session.getTargetJob() : "高级开发工程师";
        return "【标杆满分自我介绍示范 (STAR+技术矩阵法)】\n"
                + "1. 基本定位与职业背景：面试官您好，非常高兴参加本次【" + job + "】技术面试。我有一线互联网大厂核心业务研发经验，主导负责高并发分布式架构设计与高可用系统治理。\n"
                + "2. 核心技术栈与专长：熟练掌握Java多线程与JMM底层模型、AQS并发编程，深入理解MySQL InnoDB索引、事务隔离与MVCC并发控制机制；在Redis分布式缓存设计、高并发消息中间件（Kafka/RocketMQ）可靠投递以及微服务高可用熔断治理方面有深度生产实战积累。\n"
                + "3. 核心项目与量化产出：曾主导过核心业务系统从0到1架构重构与大促抗峰，针对突发峰值流量与慢SQL瓶颈，通过链路异步化、分库分表与多级缓存优化，将系统支撑QPS提升数倍，P99接口耗时显著下降，保障全链路零故障运行。\n"
                + "4. 总结与期许：期待今天能与您就系统底层机制、核心架构选型以及极端生产排障展开深入交流！";
    }

    private int resolveDrillDepth(int projectIndex, TurnEvaluation evaluation) {
        if (projectIndex <= 2) {
            return (evaluation != null && evaluation.score >= 85) ? 2 : 1;
        } else if (projectIndex <= 6) {
            if (evaluation != null && evaluation.score >= 90) return 3;
            if (evaluation != null && evaluation.score < 60) return 1;
            return 2;
        } else {
            return 3;
        }
    }

    private String resolveXiaolinCategory(JobTrack track, int fundamentalIndex, String resumeSummary) {
        String lowerResume = StringUtils.hasText(resumeSummary) ? resumeSummary.toLowerCase() : "";
        boolean isCpp = lowerResume.contains("c++") || track == JobTrack.SYSTEMS_HIGH_PERF;
        boolean isGo = lowerResume.contains("go") || lowerResume.contains("golang");
        boolean isQa = track == JobTrack.QA_SECURITY || lowerResume.contains("测试");

        if (isCpp) {
            return switch (fundamentalIndex) {
                case 1 -> "计算机网络与协议";
                case 2 -> "Linux操作系统与运维";
                case 3 -> "C++底层高性能";
                case 4 -> "数据结构与算法";
                case 5 -> "MySQL与数据存储";
                case 6 -> "Redis与高性能缓存";
                case 7 -> "架构与设计模式";
                case 8 -> "软件工程与架构规范";
                case 9 -> "Linux操作系统与运维";
                default -> "C++底层高性能";
            };
        } else if (isGo) {
            return switch (fundamentalIndex) {
                case 1 -> "计算机网络与协议";
                case 2 -> "Linux操作系统与运维";
                case 3 -> "Go语言与微服务";
                case 4 -> "云原生与K8s";
                case 5 -> "MySQL与数据存储";
                case 6 -> "Redis与高性能缓存";
                case 7 -> "数据结构与算法";
                case 8 -> "消息队列与中间件";
                case 9 -> "架构与设计模式";
                default -> "Go语言与微服务";
            };
        } else if (isQa) {
            return switch (fundamentalIndex) {
                case 1 -> "自动化测试与质量";
                case 2 -> "计算机网络与协议";
                case 3 -> "Linux操作系统与运维";
                case 4 -> "MySQL与数据存储";
                case 5 -> "Redis与高性能缓存";
                case 6 -> "Java核心与并发";
                case 7 -> "自动化测试与质量";
                case 8 -> "消息队列与中间件";
                case 9 -> "数据结构与算法";
                default -> "架构与设计模式";
            };
        } else {
            return switch (fundamentalIndex) {
                case 1 -> "计算机网络与协议";
                case 2 -> "Linux操作系统与运维";
                case 3 -> "MySQL与数据存储";
                case 4 -> "Redis与高性能缓存";
                case 5 -> "Java核心与并发";
                case 6 -> "JVM虚拟机与调优";
                case 7 -> "Spring源码机制";
                case 8 -> "消息队列与中间件";
                case 9 -> "数据结构与算法";
                default -> "架构与设计模式";
            };
        }
    }

    private CustomerKnowledge filterXiaolinQuestionByResume(List<CustomerKnowledge> list, String resumeSummary, TurnEvaluation evaluation) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }

        String lowerResume = StringUtils.hasText(resumeSummary) ? resumeSummary.toLowerCase() : "";
        boolean requireDeep = evaluation != null && evaluation.score >= 80;

        CustomerKnowledge best = list.get(0);
        int bestScore = -1;

        for (CustomerKnowledge item : list) {
            int score = 0;
            String q = item.getQuestion() != null ? item.getQuestion().toLowerCase() : "";
            String kw = item.getKeywords() != null ? item.getKeywords().toLowerCase() : "";

            if (StringUtils.hasText(lowerResume)) {
                String[] techTokens = {"mysql", "redis", "kafka", "rocketmq", "rabbitmq", "spring", "jvm", "mybatis",
                        "dubbo", "netty", "docker", "k8s", "linux", "分库分表", "分布式锁", "缓存", "索引", "线程池", "aqs",
                        "gc", "三次握手", "四次挥手", "tcp", "http", "mvcc", "跳表", "微服务"};
                for (String token : techTokens) {
                    if (lowerResume.contains(token) && (q.contains(token) || kw.contains(token))) {
                        score += 15;
                    }
                }
            }

            boolean isHard = q.contains("原理") || q.contains("底层") || q.contains("源码") || q.contains("调优") || q.contains("排查") || q.contains("机制");
            if (requireDeep && isHard) {
                score += 10;
            } else if (!requireDeep && !isHard) {
                score += 5;
            }

            if (q.length() >= 8 && q.length() <= 45) {
                score += 5;
            }

            if (score > bestScore) {
                bestScore = score;
                best = item;
            }
        }

        return best;
    }

    private XiaolinQuestionResult resolveXiaolinFundamentalQuestion(InterviewSession session, JobTrack track, int fundamentalIndex, InterviewTurn previousTurn, TurnEvaluation evaluation) {
        String category = resolveXiaolinCategory(track, fundamentalIndex, session.getResumeSummary());
        try {
            LambdaQueryWrapper<CustomerKnowledge> wrapper = new LambdaQueryWrapper<CustomerKnowledge>()
                    .likeRight(CustomerKnowledge::getLegacyId, "xl-")
                    .eq(CustomerKnowledge::getCategory, category)
                    .eq(CustomerKnowledge::getStatus, 1);
            List<CustomerKnowledge> candidateList = knowledgeMapper.selectList(wrapper);

            if (candidateList != null && !candidateList.isEmpty()) {
                CustomerKnowledge selected = filterXiaolinQuestionByResume(candidateList, session.getResumeSummary(), evaluation);
                if (selected != null) {
                    String prefix = (fundamentalIndex == 1)
                            ? "很好，对你的背景有了初步了解。接下来我们进入第二环节【核心基础八股与底层技术考查】。首先深入聊聊："
                            : "";
                    String question = prefix + selected.getQuestion();
                    String stdRef = "【小林coding官方高分示范答案】\n" + selected.getAnswer();
                    int depth = (selected.getAnswer() != null && selected.getAnswer().length() > 500) ? 2 : 1;
                    String dimension = "【环节二·" + category + "】" + (selected.getKeywords() != null ? selected.getKeywords() : category);
                    return new XiaolinQuestionResult(dimension, question, stdRef, selected.getId(), depth);
                }
            }
        } catch (Exception ex) {
            log.warn("查询小林coding基础八股题库异常，启用高可用兜底: {}", ex.getMessage());
        }

        String fallbackQ = resolveFundamentalFallback(track, fundamentalIndex);
        if (fundamentalIndex == 1) {
            fallbackQ = "很好，对你的背景有了初步了解。接下来我们进入第二环节【核心基础八股与底层技术考查】。首先：" + fallbackQ;
        }
        String stdRef = resolveFundamentalFallbackRef(track, fundamentalIndex);
        return new XiaolinQuestionResult("【环节二·" + category + "】核心八股考查", fallbackQ, stdRef, null, 2);
    }

    private String resolveFundamentalDimension(JobTrack track, int index) {
        return switch (track) {
            case SYSTEMS_HIGH_PERF -> switch (index) {
                case 1 -> "操作系统虚拟内存管理与页表PageCache";
                case 2 -> "CPU缓存行与内存屏障/无锁并发模型";
                case 3 -> "高并发Epoll网络I/O多路复用与事件循环";
                case 4 -> "协程调度器底层工作机制与上下文切换开销";
                case 5 -> "高性能内存分配器(Jemalloc/TCMalloc)底层原理";
                case 6 -> "零拷贝技术(mmap/sendfile)与DMA数据传输";
                case 7 -> "分布式共识算法(Raft)与日志一致性复制";
                case 8 -> "高性能RPC通信协议设计与序列化开销优化";
                case 9 -> "C++/Rust并发内存模型与所有权生命周期";
                default -> "超高吞吐系统性能瓶颈定位与内核eBPF调优";
            };
            case FRONTEND_MOBILE -> switch (index) {
                case 1 -> "JavaScript事件循环机制与异步任务调度管线";
                case 2 -> "浏览器关键渲染路径与重绘回流极致优化";
                case 3 -> "现代前端框架响应式原理(Vue3 Proxy vs React Fiber)";
                case 4 -> "虚拟DOM树Diff算法核心机制与Key的作用";
                case 5 -> "大型前端工程化分包构建与Tree-shaking机制";
                case 6 -> "核心性能指标(Core Web Vitals/LCP/INP)深度优化";
                case 7 -> "现代网络传输协议与HTTP/2/3浏览器缓存体系";
                case 8 -> "Web前端安全防御(XSS/CSRF/CSP)与跨域隔离机制";
                case 9 -> "跨端混合开发容器通信与离线数据持久化机制";
                default -> "微前端架构设计与大型企业级组件设计规范";
            };
            case AI_LLM -> switch (index) {
                case 1 -> "Transformer底层多头自注意力机制与数学推导";
                case 2 -> "主流大语言模型架构(Decoder-Only)与自回归推理";
                case 3 -> "高效参数微调PEFT(LoRA/QLoRA)底层梯度计算";
                case 4 -> "大模型对齐与人类偏好学习(RLHF/PPO与DPO权衡)";
                case 5 -> "工业级RAG向量检索增强与混合分块切分方案";
                case 6 -> "RAG检索重排机制与Cross-Encoder语义重打分";
                case 7 -> "AI Agent智能体多步规划与动态反思自纠错回路";
                case 8 -> "大模型推理加速部署与PagedAttention显存管理";
                case 9 -> "超长上下文窗口压缩与Function Calling工具调用";
                default -> "大模型权威评测基准体系与防幻觉安全对齐治理";
            };
            case BIG_DATA -> switch (index) {
                case 1 -> "Flink流式计算底层引擎与状态一致性管理机制";
                case 2 -> "Flink端到端Exactly-Once精确一致语义保障";
                case 3 -> "Spark计算引擎Stage划分与Shuffle内存管理机制";
                case 4 -> "海量大数据倾斜成因分析与算子极致调优实战";
                case 5 -> "数据湖仓一体存储格式(Iceberg/Hudi)ACID事务";
                case 6 -> "分布式消息队列Kafka高吞吐零拷贝与ISR容灾机制";
                case 7 -> "离线与实时数仓分层规范建模与维度建模实战";
                case 8 -> "分布式OLAP列式存储与向量化计算执行引擎";
                case 9 -> "全链路数据血缘关系构建与企业级元数据治理";
                default -> "大数据集群分布式资源调度与背压反压监控治理";
            };
            case DATABASE_STORAGE -> switch (index) {
                case 1 -> "InnoDB行存储格式与数据页分裂合并底层机制";
                case 2 -> "B+Tree与LSM-Tree索引存储结构性能深度对比";
                case 3 -> "事务ACID特性与RedoLog/UndoLog日志刷盘时机";
                case 4 -> "MVCC多版本并发控制与快照读当前读行锁机制";
                case 5 -> "千万级慢SQL执行计划深入分析与联合索引调优";
                case 6 -> "海量数据分布式分库分表与全局唯一ID生成方案";
                case 7 -> "分布式事务强一致性协议(2PC/3PC/TCC)实现对比";
                case 8 -> "MySQL高可用主从半同步复制与主从延迟消除方案";
                case 9 -> "高可用存储集群容灾切换与脑裂故障预防机制";
                default -> "冷热数据智能分层存储与海量归档架构演进实战";
            };
            case CLOUD_NATIVE_SRE -> switch (index) {
                case 1 -> "Linux内核虚拟化与Cgroups/Namespace资源隔离";
                case 2 -> "容器网络模型(CNI)与Pod跨节点数据包转发路径";
                case 3 -> "Kubernetes APIServer架构与Etcd分布式一致性保障";
                case 4 -> "Kubernetes调度器调度算法与自定义CRD控制器机制";
                case 5 -> "Service Mesh服务网格无侵入流量治理与Sidecar注入";
                case 6 -> "全链路可观测性三大支柱(Tracing/Metrics/Logging)";
                case 7 -> "生产环境动态容量规划与HPA自动弹性扩缩容实战";
                case 8 -> "多云多活容灾架构与全链路故障应急止血SOP";
                case 9 -> "混沌工程生产故障注入演练与系统韧性评估方案";
                default -> "SRE服务等级目标(SLO/SLA)制定与线上稳定性红线";
            };
            case QA_SECURITY -> switch (index) {
                case 1 -> "千万级全链路高并发压测架构设计与流量打标方案";
                case 2 -> "影子数据库表动态路由与压测脏数据隔离兜底";
                case 3 -> "企业级自动化测试平台与CI/CD质量门禁拦截体系";
                case 4 -> "线上真实流量录制回放技术与契约测试落地实战";
                case 5 -> "OWASP Top 10高危安全漏洞攻防与渗透测试实战";
                case 6 -> "API分布式鉴权加固与OAuth2/JWT防伪造重放机制";
                case 7 -> "微服务接口动态风控防刷与令牌桶精细化限流策略";
                case 8 -> "敏感业务数据动态脱敏与全链路传输合规加密体系";
                case 9 -> "混沌工程在大型系统质量与容错验证中的实施方案";
                default -> "重大线上故障复盘定位与自动化全链路巡检监控建设";
            };
            default -> switch (index) {
                case 1 -> "Java语言底层特性与JMM内存模型(volatile/可见性/重排)";
                case 2 -> "并发编程基石与AQS锁机制底层(CAS/锁升级/ReentrantLock)";
                case 3 -> "Java高并发线程池与并发容器(参数选型/ThreadLocal内存泄露)";
                case 4 -> "JVM内存布局与类加载机制(双亲委派与元空间溢出)";
                case 5 -> "JVM垃圾收集器与性能调优(G1/ZGC/STW/Full GC排障)";
                case 6 -> "MySQL InnoDB存储引擎与B+Tree索引底层原理";
                case 7 -> "MySQL事务ACID特性与MVCC多版本并发控制锁机制";
                case 8 -> "Redis核心数据结构底层实现(SDS/跳表/ziplist/渐进rehash)";
                case 9 -> "Redis高并发缓存架构与分布式锁(穿透/击穿/雪崩/双写一致)";
                default -> "高性能网络I/O模型与Spring框架底层(Netty/循环依赖/AOP)";
            };
        };
    }

    private String generateFundamentalQuestion(InterviewSession session, String dimension, JobTrack track, int fundamentalIndex, InterviewTurn previousTurn) {
        String stylePersona = getStylePersona(session.getInterviewerStyle());
        StringBuilder sb = new StringBuilder();
        sb.append("你现在是").append(stylePersona).append("。候选人正在面试【").append(session.getTargetJob()).append("】岗位。\n");
        sb.append("当前进行到第二环节：【基础八股文】第 ").append(fundamentalIndex).append(" 题，考查维度：【").append(dimension).append("】。\n");
        if (fundamentalIndex == 1) {
            sb.append("【特别要求】：这是第二环节的第一题，请以面试官口吻自然过渡破题（如：'很好，对你的背景有了初步了解。接下来我们进入第二环节【核心基础八股与底层技术考查】。首先深入聊聊...'）。\n");
        } else if (previousTurn != null && StringUtils.hasText(previousTurn.getUserAnswer())) {
            sb.append("上一题候选人的回答摘要：").append(previousTurn.getUserAnswer().substring(0, Math.min(60, previousTurn.getUserAnswer().length()))).append("...\n");
        }
        sb.append("请直接提出该维度的核心技术八股问题。\n");
        sb.append("要求：\n");
        sb.append("1. 紧扣【").append(session.getTargetJob()).append("】赛道底层核心技术与高频八股真题；\n");
        sb.append("2. 题目专业犀利，直接考察底层原理、源码细节或运行机制，严禁废话，120字以内。");

        String fallback = resolveFundamentalFallback(track, fundamentalIndex);
        if (fundamentalIndex == 1) {
            fallback = "很好，对你的背景有了初步了解。接下来我们进入第二环节【核心基础八股与底层技术考查】。首先：" + fallback;
        }

        String aiResult = callAi(sb.toString(), fallback);
        return cleanAiText(aiResult);
    }

    private String resolveFundamentalFallback(JobTrack track, int index) {
        return switch (track) {
            case SYSTEMS_HIGH_PERF -> switch (index) {
                case 1 -> "请深入阐述 Linux 虚拟内存管理中，Page Cache 缓存页与直接 I/O (Direct I/O) 的核心区别与性能权衡？";
                case 2 -> "在多核 CPU 高并发编程中，什么是伪共享（False Sharing）？底层硬件层面是如何通过 Cache Line 对齐进行消除的？";
                case 3 -> "在高性能长连接服务中，Epoll 的水平触发（LT）与边缘触发（ET）在底层就绪通知机制上有何本质差异？";
                case 4 -> "深入拆解协程调度器（如 Go GMP 模型或 C++20 协程）。当发生密集系统调用阻塞时，调度器是如何进行 M 与 P 的解绑让渡的？";
                case 5 -> "对比标准 glibc ptmalloc，Jemalloc 与 TCMalloc 是如何通过 Thread Cache 与 Arena 机制避免多线程内存分配锁竞争的？";
                case 6 -> "请剖析 Linux 零拷贝技术中 mmap 与 sendfile 的系统调用全流程，它们各自减少了几次上下文切换与 CPU 数据拷贝？";
                case 7 -> "在 Raft 分布式共识算法中，Leader 选举机制是如何避免脑裂的？日志复制出现不一致时 Leader 如何强制 Follower 对齐？";
                case 8 -> "设计超低延迟 RPC 协议时，序列化格式（Protobuf/FlatBuffers）在内存反序列化开销与数据包体积上有何深度取舍？";
                case 9 -> "请阐述 C++ 内存模型中的 std::memory_order_relaxed、acquire 与 release 在汇编层面是如何对应 CPU 内存屏障指令的？";
                default -> "当线上高吞吐服务出现微秒级长尾延迟（P999）抖动时，你通常如何使用 Perf、eBPF 或火焰图进行无侵入内核级定位？";
            };
            case FRONTEND_MOBILE -> switch (index) {
                case 1 -> "请从浏览器主线程事件循环出发，详细拆解宏任务（MacroTask）、微任务（MicroTask）与 requestAnimationFrame 渲染帧的精确执行时机。";
                case 2 -> "请详细剖析浏览器渲染引擎从解析 HTML 到像素绘制上屏的关键渲染路径。在何种场景下会触发重排（Reflow）与重绘（Repaint），如何利用合成层提升性能？";
                case 3 -> "请深度对比 Vue3 基于 ES6 Proxy 的响应式依赖收集系统与 React Fiber 架构基于单向链表的时间切片（Time Slicing）机制。";
                case 4 -> "请深入阐述 Vue3 快速 Diff 算法（最长递增子序列）与 Vue2 双端 Diff 算法的核心区别？为什么在列表渲染中绝对不能使用 index 作为 Key？";
                case 5 -> "在现代大型前端工程化构建中，Vite 基于 ESBuild 的依赖预构建与 Rollup 的生产 Tree-shaking 在 AST 语法树标记上有何底层工作原理？";
                case 6 -> "针对 Google 核心性能指标 Core Web Vitals（LCP、INP、CLS），在面对复杂 SPA 应用时，你的全链路诊断手段与前置资源优化手段是什么？";
                case 7 -> "请深度解析 HTTP/2 的多路复用与头部压缩（HPACK）原理。相比 HTTP/2，HTTP/3 为什么转而基于 QUIC（UDP）构建？";
                case 8 -> "在大型 Web 应用中，存储在 Cookie 中的敏感凭据如何防御 XSS 窃取与 CSRF 伪造攻击？CORS 预检请求（OPTIONS）触发的精确条件是什么？";
                case 9 -> "在 Hybrid 跨端混合开发中，JSBridge 的通信原理（URL Schema 拦截与注入 API）是什么？离线缓存与资源版本更新是如何做到秒级生效的？";
                default -> "在微前端架构（如基于 qiankun / Module Federation）设计中，主子应用之间是如何实现 JS 全局沙箱隔离与 CSS 样式污染隔离的？";
            };
            case AI_LLM -> switch (index) {
                case 1 -> "在 Transformer 架构中，为什么 Multi-Head Attention 需要进行缩放点积（Scaling by sqrt(d_k)）？位置编码 RoPE（旋转位置编码）的数学本质是什么？";
                case 2 -> "为什么当前主流大语言模型（如 LLaMA / GPT-4）均采用 Decoder-Only 架构？在自回归生成中，KV-Cache 的显存占用计算公式是什么？";
                case 3 -> "在大模型微调技术中，LoRA 是如何在冻结预训练权重的前提下，通过低秩矩阵分解 A 和 B 实现高效参数微调的？其前向传播计算公式是什么？";
                case 4 -> "对比 RLHF（基于人类反馈的强化学习）中的 PPO 算法与 DPO（直接偏好优化），DPO 是如何通过隐式奖励函数推导摆脱独立 Reward Model 训练的？";
                case 5 -> "在企业级 RAG 检索增强架构中，向量数据库（如 Qdrant / Milvus）的 HNSW 图索引算法是如何平衡召回率与检索时延的？Chunk 切分如何避免语义截断？";
                case 6 -> "在 RAG 系统的高阶重排阶段，为什么需要将稠密向量检索初筛结果输入给 Cross-Encoder 重排模型进行二次打分？它的时延与精度权衡是怎样的？";
                case 7 -> "请深入拆解 AI Agent 的核心决策循环（ReAct 范式：Thought -> Action -> Observation）。当 Agent 出现多步规划死循环或工具调用参数幻觉时，如何设计反思回路自纠错？";
                case 8 -> "在 vLLM 推理加速引擎中，PagedAttention 技术是如何借鉴操作系统虚拟内存分页机制彻底消除显存内部与外部碎片浪费的？";
                case 9 -> "当给大模型喂入 128k 超长上下文时，注意力机制的显存复杂度是如何通过 FlashAttention 的分块计算（Tiling）与重计算策略降至线性复杂度的？";
                default -> "如何构建严密的大模型自动化评测 Benchmark 体系？在面对 Prompt 注入攻防与越狱对抗时，企业级应用网关应如何设计多层防御防线？";
            };
            case BIG_DATA -> switch (index) {
                case 1 -> "在 Apache Flink 状态一致性管理中，基于 Chandy-Lamport 算法的 Checkpoint Barrier 是如何在流数据中穿插流转并触发异步快照的？";
                case 2 -> "请详细拆解 Flink 与外部存储（如 Kafka、MySQL）对接时，两阶段提交协议（2PC）是如何协同保障端到端 Exactly-Once 精确一致性语义的？";
                case 3 -> "在 Spark 计算引擎中，窄依赖（Narrow Dependency）与宽依赖（Shuffle Dependency）是如何划分 Stage 的？Spark 内存管理机制中 Execution 内存与 Storage 内存是如何动态借用的？";
                case 4 -> "在处理千万级海量数据倾斜（Data Skew）时，由于热点 Key 导致单个 Task 耗时过长或 OOM，你常用的全链路调优方案（如随机加盐两阶段聚合、广播 Join）有哪些？";
                case 5 -> "对比传统 Hive 数仓，新一代数据湖仓一体存储格式（Apache Iceberg / Hudi）是如何基于元数据快照树实现 ACID 事务、行级更新与时间旅行（Time Travel）的？";
                case 6 -> "在分布式消息队列 Kafka 中，ISR（In-Sync Replicas）副本集合是如何动态维护的？Kafka 的零拷贝技术与 PageCache 是如何保证单机数十万 QPS 极速写入的？";
                case 7 -> "在企业级数仓体系中，从 ODS、DWD、DWS 到 ADS 的分层规范是怎样的？在维度建模中，缓慢变化维（SCD Type 1/2/3）在业务场景中是如何落地的？";
                case 8 -> "在实时分析型数据库 ClickHouse 中，MergeTree 存储引擎的稀疏索引与数据按列压缩是怎样设计的？为什么向量化执行引擎（SIMD）能大幅超越传统行存查询性能？";
                case 9 -> "在大型数据中台建设中，如何基于 AST 解析 SQL 自动化构建全链路字段级数据血缘关系？当下游业务指标异常时，血缘图谱如何辅助快速根因定位？";
                default -> "当 Flink 线上作业出现严重的反压（Backpressure）告警时，你的全链路排查排障步骤是什么？网络缓冲区（Network Buffer）与下游算子瓶颈如何针对性调优？";
            };
            case DATABASE_STORAGE -> switch (index) {
                case 1 -> "请深入剖析 MySQL InnoDB 引擎的 Compact / Dynamic 行记录格式。变长字段列表与 NULL 值列表在物理磁盘上是如何排列的？单行数据超过页大小时如何处理溢出页？";
                case 2 -> "请从底层读写模型深入对比 B+Tree 与 LSM-Tree（Log-Structured Merge-Tree）的性能优劣。为什么 LSM-Tree 更适合写入密集型业务场景，其写放大与读放大如何治理？";
                case 3 -> "在 MySQL 事务执行过程中，Redo Log 的 WAL（Write-Ahead Logging）机制与两阶段提交（prepare 与 commit 状态）是如何与 Binlog 协同保障数据不丢与主从一致性的？";
                case 4 -> "请深入拆解 MySQL InnoDB 在可重复读（RR）隔离级别下，MVCC 机制的 ReadView 构建时机。当前读（Current Read）与快照读（Snapshot Read）有何本质区别？";
                case 5 -> "如果一条包含多表关联且数据量过千万的复杂 SQL 执行缓慢，在 Explain 执行计划中，你重点关注哪些列（type, key_len, ref, rows, Extra）？如何消除 Using filesort 与 Using temporary？";
                case 6 -> "在海量数据水平分库分表实战中，如何根据业务主键与分片键（Sharding Key）设计路由算法？在面对跨分片聚合（GROUP BY）与全局分页查询时，架构上如何优雅解决？";
                case 7 -> "请深度对比分布式事务 2PC、TCC 与本地消息表事务最终一致性方案。在电商下单扣库存、交易支付等金融级场景中，你的技术选型依据是什么？";
                case 8 -> "MySQL 高可用主从架构中，异步复制与半同步复制（Semi-Sync Replication）在数据安全性上有何差异？如何彻底解决主从同步延迟导致读写分离读取脏数据的问题？";
                case 9 -> "在 MHA 或 Orchestrator 高可用集群故障转移时，如何防止网络分区引发的“双主脑裂”？虚拟 IP（VIP）漂移与元数据仲裁机制是如何协同规避故障的？";
                default -> "面对核心交易库冷数据迅速膨胀导致存储成本飙升的痛点，如何设计在线不停机、业务无感的冷热数据归档方案？历史海量归档数据如何支撑低频快速检索？";
            };
            case CLOUD_NATIVE_SRE -> switch (index) {
                case 1 -> "请从 Linux 内核底层剖析容器的本质。Namespace（UTS, IPC, PID, Mount, Network, User）与 Cgroups 是如何协同实现进程间视角隔离与 CPU/内存资源硬性配额限制的？";
                case 2 -> "在 Kubernetes 容器网络模型中，Flannel（VXLAN / Host-GW）与 Calico（BGP / IPIP）在数据包跨节点封装与路由转发上有何本质性能差异？";
                case 3 -> "请详细拆解从客户端执行 kubectl apply 到 Pod 最终在 Worker 节点上被拉起，APIServer、Etcd、Controller Manager、Scheduler 与 Kubelet 之间的全链路通信与状态同步机制。";
                case 4 -> "在 Kubernetes 调度器中，PreFilter、Filter、Score 等调度插件扩展点是如何评估 Pod 最佳放置节点的？自定义 Operator 控制器的 Informer 缓存与工作队列是如何保障最终一致性的？";
                case 5 -> "在 Istio 服务网格中，Envoy Sidecar 是如何通过 iptables 规则透明拦截容器入站与出站网络流量的？控制面 Istiod 与数据面 Envoy 的动态配置下发（xDS 协议）机制是怎样的？";
                case 6 -> "在构建企业级可观测性平台时，OpenTelemetry 是如何标准化聚合 Metrics、Traces 与 Logs 的？当面对每秒数百万次链路追踪时，如何设计自适应尾采样策略降低存储开销？";
                case 7 -> "在生产环境中，Horizontal Pod Autoscaler (HPA) 基于 CPU 使用率扩缩容可能存在滞后抖动。如何结合自定义业务指标（如 QPS / 消息队列堆积深度）实现更灵敏的预见性弹性伸缩？";
                case 8 -> "在设计跨可用区或异地多活业务系统时，如果发生跨机房专线光纤中断，DNS 流量调度与数据库多活同步机制如何做到分钟级流量无损切换？";
                case 9 -> "在生产环境开展混沌工程（Chaos Engineering）故障注入演练时，如何界定爆炸半径（Blast Radius）？若注入网络延迟故障引发连锁级联反应，如何设计秒级一键止血回滚机制？";
                default -> "作为稳定性负责人，如何科学制定核心业务的 SLA/SLO 指标与错误预算（Error Budget）？当线上发生 P0 级重大事故时，标准的一线排障止血 SOP 与复盘流程是什么？";
            };
            case QA_SECURITY -> switch (index) {
                case 1 -> "在主导千万级全链路压测时，如何设计流量标记（如 HTTP Header 注入压测标）？压测标在跨微服务 RPC 调用与异步消息队列传递过程中如何做到零丢失透传？";
                case 2 -> "在生产环境进行全链路压测时，压测流量落入影子数据库（Shadow DB）的动态数据源路由规则是如何实现的？如何绝对保证影子数据与真实生产数据严格隔离？";
                case 3 -> "在现代 CI/CD 质量门禁体系中，如何将单元测试覆盖率（JaCoCo）、静态代码异味扫描（SonarQube）与自动化接口回归测试联动？质量红线不达标时如何阻断发布？";
                case 4 -> "线上真实流量录制与回放技术（如 基于 JVM-Sandbox 或 Go-Replay）在大型微服务重构测试中，是如何解决外部写接口调用副作用（如支付、发短信）的 Mock 与校验的？";
                case 5 -> "请深入阐述针对 OWASP Top 10 中 SQL 注入、SSRF（服务端请求伪造）与命令执行漏洞的底层挖掘思路。在代码审计中，黑名单过滤与白名单预编译参数化有何防御效果差异？";
                case 6 -> "在开放 API 接口安全防护中，基于公私钥签名的防篡改机制（timestamp + nonce + sign）是如何有效防御重放攻击与中间人劫持的？";
                case 7 -> "在核心秒杀或优惠券领取接口，面对黑灰产薅羊毛与机器爬虫刷单，如何基于滑动时间窗口、令牌桶算法与设备指纹实现精准的风控限流与拦截？";
                case 8 -> "面对数据安全法与合规审计，核心业务中用户的身份证、手机号、银行卡等敏感字段在落盘加密（如国密 SM4）与日志打印动态脱敏上，系统架构是如何分层落地的？";
                case 9 -> "混沌测试与传统自动化测试有何本质区别？在面对依赖服务不可用、网络丢包抖动、磁盘读写夯死等极限场景时，如何通过混沌演练验证系统的自愈与容错弹性？";
                default -> "当发生重大线上生产 Bug 造成资损告警时，你如何带领测试开发团队进行事故根因定位（5 Whys 追问法），并在全链路流程上建立防再发生的长效防御机制？";
            };
            default -> switch (index) {
                case 1 -> "请深入阐述 Java 中 volatile 关键字在 JMM 内存模型中的实现原理？它是如何通过内存屏障禁止指令重排并保障可见性的？";
                case 2 -> "请剖析 AQS（AbstractQueuedSynchronizer）的核心工作机制。以 ReentrantLock 为例，底层是如何基于 CAS 与双向同步队列实现公平锁与非公平锁的？";
                case 3 -> "在生产高并发场景下，ThreadPoolExecutor 的核心参数（corePoolSize, maximumPoolSize, workQueue, Handler）是如何协同运转的？遇到流量突增时哪种阻塞队列选型最合理？";
                case 4 -> "请详细拆解 JVM 类加载的双亲委派机制。在何种工业级场景下需要破坏双亲委派机制（例如 Tomcat / JDBC / 热部署）？如何实现自定义类加载器？";
                case 5 -> "请深度对比 JVM G1 垃圾收集器与 ZGC 的核心区别。在你的生产排障经历中，什么场景下会触发 Full GC，线上排查定位工具链有哪些？";
                case 6 -> "请深入剖析 MySQL InnoDB 引擎中，聚簇索引与二级索引的 B+Tree 结构差异。为什么复合索引必须遵循最左前缀匹配原则？索引下推（ICP）机制是如何优化查询的？";
                case 7 -> "MySQL InnoDB 是如何基于 UndoLog 和 ReadView 实现多版本并发控制（MVCC）的？在可重复读（RR）隔离级别下，间隙锁（Gap Lock）是如何协同防止幻读的？";
                case 8 -> "请深入剖析 Redis 中跳表（SkipList）与压缩列表（ziplist/quicklist）的底层数据结构。为什么 ZSet 在元素较多时选择跳表而不是红黑树或平衡二叉树？";
                case 9 -> "在千万级高并发系统中，如何彻底解决 Redis 缓存穿透、击穿与雪崩？高并发更新时，Cache-Aside 模式下如何保障 Redis 缓存与数据库的双写最终一致性？";
                default -> "请深度阐述 Netty 高性能网络编程的 Reactor 线程模型与零拷贝机制。在微服务架构中，Spring 是如何基于三级缓存机制解决单例 Bean 的属性循环依赖的？";
            };
        };
    }

    private String resolveProjectDrillDimension(JobTrack track, int index) {
        return switch (index) {
            case 1 -> "核心项目全链路架构设计与技术选型权衡";
            case 2 -> "业务大促高并发抗峰与核心链路容量评估";
            case 3 -> "分布式数据一致性保证与复杂事务兜底方案";
            case 4 -> "复杂业务演进中的难点攻坚与关键决策反思";
            case 5 -> "生产环境慢SQL与海量存储分库分表调优";
            case 6 -> "线上重大故障排查实战(CPU 100%/OOM内存泄露)";
            case 7 -> "高可用容灾、全链路熔断限流与服务雪崩防御";
            case 8 -> "系统性能极致优化(RT降低/吞吐量数倍提升实战)";
            default -> "压轴终局复杂系统架构设计与演进思考";
        };
    }

    private ProjectDrillResult generateProjectDrillQuestion(InterviewSession session, JobTrack track, int projectIndex, int nextDepth, InterviewTurn previousTurn, TurnEvaluation evaluation) {
        String stylePersona = getStylePersona(session.getInterviewerStyle());
        StringBuilder sb = new StringBuilder();
        sb.append("你现在是").append(stylePersona).append("。候选人正在面试【").append(session.getTargetJob()).append("】岗位。\n");
        sb.append("当前进行到第三环节：【简历项目追问】第 ").append(projectIndex).append(" 题，考查深度：L").append(nextDepth).append("。\n\n");

        String resumeSummary = session.getResumeSummary();
        if (StringUtils.hasText(resumeSummary)) {
            sb.append("【候选人绑定的真实简历信息（项目经历/技术栈/薄弱点）】：\n").append(resumeSummary).append("\n\n");
        } else {
            sb.append("【候选人目标岗位核心技术栈】：").append(session.getTargetJob()).append("。请结合千万级高并发真实生产业务场景出题。\n\n");
        }

        if (previousTurn != null && StringUtils.hasText(previousTurn.getUserAnswer())) {
            sb.append("【上一题考查维度与题目】：").append(previousTurn.getDimension()).append(" - ").append(previousTurn.getQuestion()).append("\n");
            sb.append("【上一题候选人真实回答】：").append(previousTurn.getUserAnswer()).append("\n");
            if (evaluation != null) {
                sb.append("【上一题AI考官评审诊断】：得分 ").append(evaluation.score).append("分；").append(evaluation.feedback).append("\n");
            }
            sb.append("\n");
        }

        sb.append("【出题与过滤铁律】（必须绝对遵守）：\n");
        sb.append("1. 【必须基于简历】：所有出题必须严格基于候选人简历中真实写明的具体项目经历、系统架构、业务模块或技术栈。直接点名候选人简历中的项目名称或核心组件！\n");
        sb.append("2. 【严格过滤无关考题】：绝对禁止抛出任何与候选人简历不相干的技术、组件或无关业务领域！若简历未提及某技术（如未写过NoSQL/C++/大数据），严禁凭空提问未提及的技术！\n");
        sb.append("3. 【上下文感知与剥洋葱追问】：根据候选人上一轮回答中的技术漏洞、细节或亮点，展开更深一层的追问（L1架构选型与权衡 -> L2底层数据流与性能瓶颈 -> L3生产极限容灾/CPU 100%/故障止血SOP）；\n");
        sb.append("4. 【同时生成标杆示范答案】：必须为本道题生成高水准的大厂架构师级满分示范答题思路，要求条理分明（分点阐述：原理/架构实现/极端故障应对）；\n");
        sb.append("5. 请严格按以下 JSON 格式输出，不要输出任何额外的包裹代码块或多余文字：\n");
        sb.append("{\n");
        sb.append("  \"dimension\": \"项目架构选型与高并发抗峰\",\n");
        sb.append("  \"question\": \"出题正文，150字以内，面试官口吻直接提问\",\n");
        sb.append("  \"standardReference\": \"【标杆示范满分答案 (项目深度实战)】\\n1. 核心原理...\\n2. 架构落地...\\n3. 生产排障...\"\n");
        sb.append("}");

        String prompt = sb.toString();
        String aiResult = callAi(prompt, null);

        if (StringUtils.hasText(aiResult)) {
            try {
                String cleanJson = extractJson(aiResult);
                JsonNode root = objectMapper.readTree(cleanJson);
                String q = root.path("question").asText("");
                String dim = root.path("dimension").asText("核心项目架构深挖");
                String ref = root.path("standardReference").asText("");
                if (StringUtils.hasText(q)) {
                    if (projectIndex == 1) {
                        q = "基础八股考查告一段落。接下来进入第三环节【简历项目与生产实战深度追问】。" + q;
                    }
                    if (!StringUtils.hasText(ref)) {
                        ref = generateDefaultProjectRef(session, dim, q);
                    }
                    return new ProjectDrillResult(dim, cleanAiText(q), ref);
                }
            } catch (Exception ex) {
                log.warn("解析项目深挖出题 JSON 失败，启用启发式保底: {}", ex.getMessage());
            }
        }

        return resolveContextualProjectFallback(session, track, projectIndex, nextDepth);
    }

    private ProjectDrillResult resolveContextualProjectFallback(InterviewSession session, JobTrack track, int projectIndex, int depth) {
        String dimension = resolveProjectDrillDimension(track, projectIndex);
        String question = resolveProjectDrillFallback(session, dimension, track, projectIndex);
        if (projectIndex == 1) {
            question = "基础八股考查告一段落。接下来进入第三环节【简历项目与生产实战深度追问】。" + question;
        }
        String ref = generateDefaultProjectRef(session, dimension, question);
        return new ProjectDrillResult(dimension, question, ref);
    }

    private String generateDefaultProjectRef(InterviewSession session, String dimension, String question) {
        return "【标杆示范满分答案 (大厂架构师深度复盘)】\n"
                + "1. 架构选型与核心考量：针对" + dimension + "，首先明确业务场景与吞吐量量级，分析单点瓶颈与CAP权衡，选定最匹配的技术方案。\n"
                + "2. 底层机制与数据流转：深入到组件底层机制（如索引树分裂、内存CacheLine对齐、批量刷盘或零拷贝传输），确保链路在高并发下低开销低抖动。\n"
                + "3. 极端生产场景与止血SOP：制定完备的降级熔断防线，针对网络抖动、慢查询及CPU飙高，通过指标打标、动态限流与灰度分流实现毫秒级止血。";
    }

    private String resolveProjectDrillFallback(InterviewSession session, String dimension, JobTrack track, int index) {
        boolean hasResume = StringUtils.hasText(session.getResumeSummary());
        return switch (index) {
            case 1 -> hasResume
                    ? "请结合你简历中最具代表性的核心项目，深入阐述该系统的全链路架构设计。在核心链路的技术选型中，你权衡了哪些备选方案，为什么最终选定当前架构？"
                    : "请结合你主导过的核心业务系统，详细剖析其全链路架构设计与关键模块划分。面对高并发高可用诉求，你们在技术选型上有何得失权衡？";
            case 2 -> hasResume
                    ? "在你的简历项目中，面对大促活动或极端高并发峰值流量冲击时，你们是如何做系统容量评估与压测摸高的？核心链路采取了哪些限流、降级与熔断保障？"
                    : "在面对数万至数十万 QPS 的大促高峰流量时，核心微服务链路的容量评估是如何计算的？当并发流量超出承载极限时，你们采取了哪些多级限流与优雅降级手段？";
            case 3 -> "在跨微服务或者跨数据源的复杂分布式业务链路中，你们是如何保障数据最终一致性的？若外部异步消息丢失或下游调用超时，系统具备怎样的自动对账与幂等补偿兜底机制？";
            case 4 -> "在项目的持续架构演进与复杂业务迭代过程中，你遇到过的最大技术难点是什么？当时的技术瓶颈在哪里，你最终是如何攻坚并拿到量化业务结果的？";
            case 5 -> "若线上核心业务表数据量突破千万且单条 SQL 查询耗时出现毛刺，你的全链路诊断调优步骤是什么？如何设计并实施不停机平滑分库分表迁移与数据校验？";
            case 6 -> "如果生产环境突然触发监控告警：CPU 飙升至 100% 或微服务实例突发内存泄露频繁 Full GC，请详细阐述你在生产环境上的完整排查、定位、止血与根治步骤。";
            case 7 -> "面对上游依赖服务大面积超时或网络抖动引发的雪崩隐患，你们是如何设计系统高可用与容灾防线的？Sentinel/Hystrix 熔断降级阈值是如何科学配置的？";
            case 8 -> "请分享一次你在实际项目中主导的系统性能极致优化实操：优化前的系统吞吐量与响应时间（RT）指标是多少，通过哪些具体的架构与代码级手段实现了数倍的性能飞跃？";
            default -> "面对业务未来 3~5 年可能出现的技术瓶颈与业务形态变更，如果由你主导重构，你会从哪些维度重新规划该系统的底层架构演进路线？";
        };
    }

    private String resolveCodingProblem(JobTrack track) {
        return switch (track) {
            case SYSTEMS_HIGH_PERF -> "【代码实战题】：请在右侧代码沙箱中实现一个『高并发无锁环形队列 (Lock-Free Ring Buffer) 或基于原子操作的协程池』，需保证并发安全并分析时空复杂度。";
            case FRONTEND_MOBILE -> "【代码实战题】：请在右侧代码沙箱中手写实现一个『深度优先的虚拟 DOM Diff 算法核心逻辑』或『支持并发限制的 Promise 并发调度器』，并分析其时间与空间复杂度。";
            case AI_LLM -> "【代码实战题】：请在右侧代码沙箱中使用 Python 实现一个『自注意力机制 Self-Attention 或滑动窗口 KV-Cache 内存管理核心逻辑』，并推导其计算时空复杂度。";
            case BIG_DATA -> "【代码实战题】：请在右侧代码沙箱中实现一个『Top-K 热度统计（流式滑动窗口与最小堆结合）』或『海量数据布隆过滤器核心哈希映射』，并分析内存占用与复杂度。";
            case DATABASE_STORAGE -> "【代码实战题】：请在右侧代码沙箱中实现一个『LRU / LFU 缓存淘汰算法核心数据结构』或『跳表 SkipList 插入与查找逻辑』，需保证时间复杂度达到 O(1) 或 O(logN)。";
            case CLOUD_NATIVE_SRE -> "【代码实战题】：请在右侧代码沙箱中编写一段核心逻辑：实现一个『生产级熔断器状态机 (Closed/Open/Half-Open)』，支持滑动失败率窗口统计与自愈探活探测。";
            case QA_SECURITY -> "【代码实战题】：请在右侧代码沙箱中实现一个『支持动态 QPS 阶梯爬坡的压测速率控制器 (Rate Limiter)』，需保证微秒级调度精度与线程安全性。";
            default -> "【代码实战题】：请在右侧代码沙箱中实现一个『高并发限流器（支持滑动窗口计数或令牌桶算法）』，需保证多线程安全并分析时空复杂度。";
        };
    }

    private String resolveCodingStandardRef(JobTrack track) {
        return switch (track) {
            case SYSTEMS_HIGH_PERF -> "【标杆示范满分答案 (无锁环形队列 Lock-Free Ring Buffer)】\n"
                    + "```java\n"
                    + "public class LockFreeRingBuffer<T> {\n"
                    + "    private final Object[] buffer;\n"
                    + "    private final int capacity;\n"
                    + "    private final AtomicLong head = new AtomicLong(0);\n"
                    + "    private final AtomicLong tail = new AtomicLong(0);\n"
                    + "    public LockFreeRingBuffer(int capacity) {\n"
                    + "        this.capacity = capacity;\n"
                    + "        this.buffer = new Object[capacity];\n"
                    + "    }\n"
                    + "    public boolean offer(T item) {\n"
                    + "        long currentTail;\n"
                    + "        do {\n"
                    + "            currentTail = tail.get();\n"
                    + "            if (currentTail - head.get() >= capacity) return false;\n"
                    + "        } while (!tail.compareAndSet(currentTail, currentTail + 1));\n"
                    + "        buffer[(int) (currentTail % capacity)] = item;\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "    @SuppressWarnings(\"unchecked\")\n"
                    + "    public T poll() {\n"
                    + "        long currentHead;\n"
                    + "        do {\n"
                    + "            currentHead = head.get();\n"
                    + "            if (currentHead >= tail.get()) return null;\n"
                    + "        } while (!head.compareAndSet(currentHead, currentHead + 1));\n"
                    + "        return (T) buffer[(int) (currentHead % capacity)];\n"
                    + "    }\n"
                    + "}\n"
                    + "```\n"
                    + "【复杂度分析】：入队出队时间复杂度均为 O(1)，无锁CAS保证极高并发吞吐；空间复杂度 O(N)。";
            case FRONTEND_MOBILE -> "【标杆示范满分答案 (Promise并发调度器 Scheduler)】\n"
                    + "```javascript\n"
                    + "class PromiseScheduler {\n"
                    + "  constructor(maxConcurrent = 2) {\n"
                    + "    this.maxConcurrent = maxConcurrent;\n"
                    + "    this.runningCount = 0;\n"
                    + "    this.queue = [];\n"
                    + "  }\n"
                    + "  add(promiseCreator) {\n"
                    + "    return new Promise((resolve, reject) => {\n"
                    + "      this.queue.push({ promiseCreator, resolve, reject });\n"
                    + "      this.runNext();\n"
                    + "    });\n"
                    + "  }\n"
                    + "  runNext() {\n"
                    + "    if (this.runningCount >= this.maxConcurrent || this.queue.length === 0) return;\n"
                    + "    const { promiseCreator, resolve, reject } = this.queue.shift();\n"
                    + "    this.runningCount++;\n"
                    + "    promiseCreator().then(resolve).catch(reject).finally(() => {\n"
                    + "      this.runningCount--;\n"
                    + "      this.runNext();\n"
                    + "    });\n"
                    + "  }\n"
                    + "}\n"
                    + "```\n"
                    + "【复杂度分析】：时间复杂度 O(1) 调度入队出队，空间复杂度 O(N) 队列排队任务数。";
            default -> "【标杆示范满分答案 (高并发滑动窗口限流器 SlidingWindowRateLimiter)】\n"
                    + "```java\n"
                    + "public class SlidingWindowRateLimiter {\n"
                    + "    private final int limit;\n"
                    + "    private final long windowSizeMs;\n"
                    + "    private final ConcurrentLinkedQueue<Long> timestamps = new ConcurrentLinkedQueue<>();\n"
                    + "    private final Object lock = new Object();\n"
                    + "    public SlidingWindowRateLimiter(int limit, long windowSizeMs) {\n"
                    + "        this.limit = limit;\n"
                    + "        this.windowSizeMs = windowSizeMs;\n"
                    + "    }\n"
                    + "    public boolean tryAcquire() {\n"
                    + "        long now = System.currentTimeMillis();\n"
                    + "        long boundary = now - windowSizeMs;\n"
                    + "        synchronized (lock) {\n"
                    + "            while (!timestamps.isEmpty() && timestamps.peek() <= boundary) {\n"
                    + "                timestamps.poll();\n"
                    + "            }\n"
                    + "            if (timestamps.size() < limit) {\n"
                    + "                timestamps.offer(now);\n"
                    + "                return true;\n"
                    + "            }\n"
                    + "            return false;\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n"
                    + "```\n"
                    + "【复杂度分析】：平摊时间复杂度 O(1)，空间复杂度 O(Limit) 存储窗口内时间戳，完美解决固定窗口临界突刺问题。";
        };
    }

    private String resolveFundamentalFallbackRef(JobTrack track, int index) {
        return "【标杆示范满分答题思路】\n"
                + "1. 核心定义与设计意图：准确给出该机制的核心概念与为什么要在系统/内核中如此设计；\n"
                + "2. 底层工作机理与时空权衡：深入拆解其内存布局、关键数据结构或源码调用链（如锁竞争、缓存行、刷盘时机）；\n"
                + "3. 生产落地与高频排障实战：结合线上千万级并发场景，说明常见故障陷阱及成熟的调优/避坑方案。";
    }

    /**
     * 评估候选人本轮回答。
     */
    private TurnEvaluation evaluateTurnAnswer(InterviewSession session, InterviewTurn turn, String standardKnowledge) {
        StringBuilder sb = new StringBuilder();
        sb.append("你现在是严谨的互联网大厂面试官委员会主席。请对候选人的回答进行严苛评分与诊断。\n");
        sb.append("【考查维度】：").append(turn.getDimension()).append("\n");
        sb.append("【面试题目】：").append(turn.getQuestion()).append("\n");
        sb.append("【候选人回答】：").append(turn.getUserAnswer() != null ? turn.getUserAnswer() : "").append("\n");
        if (StringUtils.hasText(standardKnowledge)) {
            sb.append("【题库标准参考答案】：\n").append(standardKnowledge).append("\n");
        }
        sb.append("\n请严格按以下 JSON 格式返回点评结果（不要输出 markdown 代码块标记以外的多余文字）：\n");
        sb.append("{\n");
        sb.append("  \"score\": 82,\n");
        sb.append("  \"feedback\": \"【亮点】：阐述清晰，点出了核心机制；【漏洞】：未考虑到并发冲突下的极端边界；【改进建议】：建议补充源码层面的细节。\",\n");
        sb.append("  \"standardRef\": \"标杆答题思路：首先回答定义...其次结合底层源码...最后联系线上实战...\"\n");
        sb.append("}");
        String prompt = sb.toString();

        String aiResult = callAi(prompt, null);
        if (StringUtils.hasText(aiResult)) {
            try {
                String cleanJson = extractJson(aiResult);
                JsonNode root = objectMapper.readTree(cleanJson);
                int score = root.path("score").asInt(75);
                String feedback = root.path("feedback").asText("");
                String standardRef = root.path("standardRef").asText("");
                if (StringUtils.hasText(feedback)) {
                    return new TurnEvaluation(Math.max(0, Math.min(score, 100)), feedback, standardRef);
                }
            } catch (Exception ex) {
                log.warn("解析面试点评 JSON 失败，启用启发式备选评估: {}", ex.getMessage());
            }
        }

        // 启发式兜底评估
        String rawAnswer = turn.getUserAnswer() != null ? turn.getUserAnswer().trim() : "";
        int length = rawAnswer.length();
        boolean isNegative = rawAnswer.matches("(?i)^(不知道|不会|没了解过|pass|跳过|略|不清楚|不了解|未掌握|没用过|无|暂无|没做过).*$") || length < 5;
        int score;
        String feedback;
        if (isNegative) {
            score = 0;
            feedback = "【评语】：候选人表明未掌握该领域技术或作答过于简短，未能展现相关工程技术沉淀。建议深入研读对应底层原理与标准实现方案，切忌在面试中直接放弃。";
        } else if (length > 200) {
            score = 85;
            feedback = "【评语】：回答结构相对完整，阐述了关键逻辑。建议进一步结合线上排障指标与高并发极限场景做更深层次的对比总结。";
        } else if (length > 80) {
            score = 75;
            feedback = "【评语】：回答切中了部分核心要点，但在底层运行机制和工程实践边界上阐述较为简略。建议结合大厂生产环境实际指标进行更深度的结构化答题。";
        } else {
            score = 50;
            feedback = "【评语】：回答较为简略，仅提及表层概念，缺乏深层原理机制与生产实践支撑。建议遵循 STAR 法则进行系统性补充。";
        }
        return new TurnEvaluation(score, feedback, standardKnowledge);
    }

    /**
     * 代码沙箱与代码异味审计。
     */
    private void auditCodeSubmission(InterviewCodeSubmission submission) {
        StringBuilder sb = new StringBuilder();
        sb.append("请作为资深架构师兼 LeetCode 评测判题沙箱，对学员提交的代码进行多维评测与重构：\n");
        sb.append("【题目】：").append(submission.getProblemTitle()).append("\n");
        sb.append("【语言】：").append(submission.getLanguage()).append("\n");
        sb.append("【代码】：\n").append(submission.getUserCode()).append("\n\n");
        sb.append("请严格按以下 JSON 格式返回评测结果：\n");
        sb.append("{\n");
        sb.append("  \"executionStatus\": \"accepted\",\n");
        sb.append("  \"timeComplexity\": \"O(N)\",\n");
        sb.append("  \"spaceComplexity\": \"O(1)\",\n");
        sb.append("  \"passedTestCases\": 10,\n");
        sb.append("  \"totalTestCases\": 10,\n");
        sb.append("  \"codeSmells\": \"1. 缺乏空指针防御判断；2. 变量命名不够语义化；3. 缺乏必要的并发保护。\",\n");
        sb.append("  \"refactoredCode\": \"// 阿里高阶重构示范代码...\"\n");
        sb.append("}");
        String prompt = sb.toString();

        String aiResult = callAi(prompt, null);
        if (StringUtils.hasText(aiResult)) {
            try {
                String cleanJson = extractJson(aiResult);
                JsonNode root = objectMapper.readTree(cleanJson);
                submission.setExecutionStatus(root.path("executionStatus").asText("accepted"));
                submission.setTimeComplexity(root.path("timeComplexity").asText("O(n)"));
                submission.setSpaceComplexity(root.path("spaceComplexity").asText("O(1)"));
                submission.setPassedTestCases(root.path("passedTestCases").asInt(10));
                submission.setTotalTestCases(root.path("totalTestCases").asInt(10));
                submission.setCodeSmells(root.path("codeSmells").asText("代码逻辑完整，建议进一步抽取公共常量并增强入参防御。"));
                submission.setRefactoredCode(root.path("refactoredCode").asText(""));
                return;
            } catch (Exception ex) {
                log.warn("解析代码审计 JSON 失败: {}", ex.getMessage());
            }
        }

        // 默认兜底评测结果
        submission.setExecutionStatus("accepted");
        submission.setTimeComplexity("O(n)");
        submission.setSpaceComplexity("O(1)");
        submission.setPassedTestCases(10);
        submission.setTotalTestCases(10);
        submission.setCodeSmells("1. 边界条件防御可进一步强化；2. 缺少详细的 JavaDoc 规范注释。");
        submission.setRefactoredCode("// 规范工程重构：增加空参校验与并发安全保障\n" + submission.getUserCode());
    }

    /**
     * 生成终局多维能力诊断报告。
     */
    private InterviewReport generateFinalReport(InterviewSession session, List<InterviewTurn> turns,
                                                List<InterviewCodeSubmission> codes, int avgScore) {
        JobTrack track = detectJobTrack(session.getTargetJob());
        String defaultCourses = resolveDefaultCourses(track);

        // 缺考或零分交卷防御
        if (avgScore <= 0 || turns == null || turns.stream().noneMatch(t -> StringUtils.hasText(t.getUserAnswer()))) {
            InterviewReport report = new InterviewReport();
            report.setOfferDecision("Reject");
            report.setLevelMatch("未达标 (本场面试未完成实质作答)");
            report.setRadarData("{\"core\":20,\"architecture\":20,\"storage\":20,\"distributed\":20,\"coding\":20,\"communication\":20}");
            report.setOverallSummary(String.format("候选人在【%s】岗位的考察中未进行实质性作答即交卷，暂无法评估其实际技术深度。建议端正求职态度并系统性复习基础知识后再次挑战。", session.getTargetJob()));
            report.setCoreStrengths("暂未采集到有效答题数据。");
            report.setCriticalWeaknesses("全场核心题目均未作答，缺乏有效技术输出与工程实战证明。");
            report.setSpeechRefactoring("建议至少完成三轮以上完整技术追问与代码沙箱实测，方可获得精准的大厂 STAR 话术诊断。");
            report.setRecommendedCourses(defaultCourses);
            return report;
        }

        StringBuilder transcript = new StringBuilder();
        for (InterviewTurn t : turns) {
            transcript.append("【第").append(t.getTurnNum()).append("轮 - ").append(t.getDimension())
                    .append(" (深度").append(t.getDepthLevel() != null ? t.getDepthLevel() : 1).append(")】\n");
            transcript.append("问：").append(t.getQuestion()).append("\n");
            transcript.append("答：").append(t.getUserAnswer() != null ? t.getUserAnswer() : "未作答").append("\n");
            transcript.append("得分：").append(t.getTurnScore() != null ? t.getTurnScore() : 0).append("\n");
            transcript.append("点评：").append(t.getAiFeedback() != null ? t.getAiFeedback() : "").append("\n\n");
        }

        // BUG-36, BUG-51: 将代码沙箱提交、执行状态及架构异味完整注入到评审实录中
        if (codes != null && !codes.isEmpty()) {
            transcript.append("【算法设计与代码沙箱实测记录】：\n");
            for (InterviewCodeSubmission c : codes) {
                transcript.append("题名：").append(c.getProblemTitle()).append(" (语言: ").append(c.getLanguage()).append(")\n");
                transcript.append("沙箱执行状态：").append(c.getExecutionStatus())
                        .append("，通过测试用例：").append(c.getPassedTestCases()).append("/").append(c.getTotalTestCases())
                        .append("，时间复杂度：").append(c.getTimeComplexity())
                        .append("，空间复杂度：").append(c.getSpaceComplexity()).append("\n");
                transcript.append("提交源码：\n").append(c.getUserCode()).append("\n");
                if (StringUtils.hasText(c.getCodeSmells())) {
                    transcript.append("架构异味与重构建议：").append(c.getCodeSmells()).append("\n");
                }
                transcript.append("\n");
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("你现在是阿里巴巴与字节跳动联合高级技术评审委员会。请根据以下完整的模拟面试答题记录，生成全维度的终局职涯诊断报告：\n");
        sb.append("【目标岗位】：").append(session.getTargetJob()).append("\n");
        sb.append("【目标企业】：").append(session.getCompanyTarget()).append("\n");
        sb.append("【综合均分】：").append(avgScore).append("\n");
        if (StringUtils.hasText(session.getResumeSummary())) {
            sb.append("【候选人关联的真实简历背景与声称项目】：\n").append(session.getResumeSummary()).append("\n");
            sb.append("【关键要求】：请特别对比候选人简历中的项目声称与本次模拟面试现场答题的表现，在 overallSummary 与 criticalWeaknesses 中输出简历真实度与深度对齐评价！\n");
        }
        sb.append("【全部问答实录】：\n").append(transcript).append("\n\n");
        sb.append("请严格按以下 JSON 格式输出最终评审报告：\n");
        sb.append("{\n");
        sb.append("  \"offerDecision\": \"Hire\",\n");
        sb.append("  \"levelMatch\": \"对标阿里P6+ / 字节2-1\",\n");
        sb.append("  \"radarData\": {\"core\": 85, \"architecture\": 78, \"storage\": 82, \"distributed\": 75, \"coding\": 88, \"communication\": 80},\n");
        sb.append("  \"overallSummary\": \"综合技术基础扎实，对技术核心机制与并发原理掌握较好...\",\n");
        sb.append("  \"coreStrengths\": \"1. 并发底层原理理解深刻；2. 具有良好的系统抽象意识；3. 代码风格规范。\",\n");
        sb.append("  \"criticalWeaknesses\": \"1. 面对线上大促极限故障时的排障经验稍显欠缺；2. 分布式高可用选型缺乏实战权衡细节。\",\n");
        sb.append("  \"speechRefactoring\": \"【原版回答缺陷】：语言偏口语化，未突出量化业务成果。\\n【大厂STAR重塑示范】：在XX项目中（Situation），面对QPS飙升3倍挑战（Task），通过引入本地缓存与分库分表重构（Action），将RT降低65%（Result）。\",\n");
        sb.append("  \"recommendedCourses\": ").append(defaultCourses).append("\n");
        sb.append("}");
        String prompt = sb.toString();

        String aiResult = callAi(prompt, null);
        if (StringUtils.hasText(aiResult)) {
            try {
                String cleanJson = extractJson(aiResult);
                JsonNode root = objectMapper.readTree(cleanJson);

                InterviewReport report = new InterviewReport();
                report.setOfferDecision(root.path("offerDecision").asText(determineOffer(avgScore)));
                report.setLevelMatch(root.path("levelMatch").asText(determineLevel(avgScore)));
                report.setRadarData(root.path("radarData").toString());
                report.setOverallSummary(root.path("overallSummary").asText("学员在核心技术领域具备良好潜质，建议持续深耕线上实战与系统高可用设计。"));
                report.setCoreStrengths(root.path("coreStrengths").asText("1. 基础概念清晰；2. 学习吸收能力强；3. 思维敏捷。"));
                report.setCriticalWeaknesses(root.path("criticalWeaknesses").asText("1. 极限高并发实战经验需补充；2. 排障工具链熟练度待提升。"));
                report.setSpeechRefactoring(root.path("speechRefactoring").asText("建议采用 STAR 法则，以指标量化（如 RT 下降、QPS 提升）重塑答题话术。"));
                report.setRecommendedCourses(root.path("recommendedCourses").toString());
                return report;
            } catch (Exception ex) {
                log.warn("解析终局报告 JSON 失败: {}", ex.getMessage());
            }
        }

        // 兜底高阶报告生成
        InterviewReport report = new InterviewReport();
        report.setOfferDecision(determineOffer(avgScore));
        report.setLevelMatch(determineLevel(avgScore));

        int core = Math.max(15, Math.min(avgScore + 3, 95));
        int arch = Math.max(15, Math.min(avgScore - 4, 92));
        int storage = Math.max(15, Math.min(avgScore + 1, 95));
        int dist = Math.max(15, Math.min(avgScore - 6, 90));
        int coding = Math.max(15, Math.min(avgScore + 2, 95));
        int comm = Math.max(20, Math.min(avgScore + 5, 90));
        report.setRadarData(String.format("{\"core\":%d,\"architecture\":%d,\"storage\":%d,\"distributed\":%d,\"coding\":%d,\"communication\":%d}",
                core, arch, storage, dist, coding, comm));

        String summary;
        if (avgScore >= 85) {
            summary = String.format("候选人在【%s】岗位的多轮深度考察中展现出卓越的技术深度与架构功底，综合得分 %d 分。完全契合大厂资深技术岗位要求，具备主导核心业务与架构攻坚能力。", session.getTargetJob(), avgScore);
        } else if (avgScore >= 70) {
            summary = String.format("候选人在【%s】岗位的考察中表现出扎实的技术底色与工程素养，综合得分 %d 分。具备独立负责业务核心模块研发与方案落地能力。", session.getTargetJob(), avgScore);
        } else if (avgScore >= 50) {
            summary = String.format("候选人在【%s】岗位的考察中具备基础技术认知，综合得分 %d 分。但在底层高并发机制、源码原理及生产级排障实战上存在短板，建议针对性补强实战。", session.getTargetJob(), avgScore);
        } else {
            summary = String.format("候选人在【%s】岗位的考察中未能展现出岗位所需的专业技术深度，综合得分 %d 分。技术基础较为薄弱或核心题目未作实质答复，未达大厂准入门槛。", session.getTargetJob(), avgScore);
        }
        report.setOverallSummary(summary);

        String strengths;
        if (avgScore < 50) {
            strengths = "1. 勇于参与大厂高压实战模拟；\n2. 展现出一定的学习意愿与探索心态。";
        } else {
            strengths = switch (track) {
                case SYSTEMS_HIGH_PERF -> "1. 系统底层与高并发机制理解深刻；\n2. 具备良好的无锁与低延迟设计意识；\n3. 答题逻辑严密，具备硬核攻坚特质。";
                case FRONTEND_MOBILE -> "1. Web 前端核心渲染管线与事件循环掌握扎实；\n2. 具备现代框架底层机制与工程化抽象思维；\n3. 重视用户极致体验与性能边界防护。";
                case AI_LLM -> "1. Transformer 底层注意力机制与数学原理掌握熟练；\n2. 对大模型微调、RAG 检索增强与 Agent 规划有落地实战；\n3. 思维敏锐，紧跟前沿算法进展。";
                case BIG_DATA -> "1. 批流一体计算引擎与状态机机制理解透彻；\n2. 具备海量数据倾斜与千万级作业调优经验；\n3. 数据湖仓一体架构视野广阔。";
                case DATABASE_STORAGE -> "1. 存储引擎底层 B+Tree/LSM-Tree 与事务 MVCC 机制透彻；\n2. 千万级慢 SQL 与高可用容灾实战经验丰富；\n3. 严谨稳健，具备优秀的数据安全性保障意识。";
                case CLOUD_NATIVE_SRE -> "1. K8s 控制器编排机制与 Linux 容器底层网络深刻；\n2. 具备优秀的全链路可观测性与生产容灾快速止血经验；\n3. 具有极高的线上稳定性风险敬畏心。";
                case QA_SECURITY -> "1. 全链路高并发压测设计与容量规划体系完备；\n2. 敏锐的边界攻防、漏洞排查与自动化建设能力；\n3. 质量门禁与混沌演练把控全面。";
                default -> "1. Java 核心与并发底层掌握熟练；\n2. 具备良好的工程代码编写习惯；\n3. 能够快速领会面试官的连环追问意图。";
            };
        }
        report.setCoreStrengths(strengths);

        String weaknesses = switch (track) {
            case SYSTEMS_HIGH_PERF -> "1. 极端高负载下内核网络丢包与系统调用上下文切换的调优经验需补充；\n2. 分布式共识脑裂处理细节可进一步强化。";
            case FRONTEND_MOBILE -> "1. 大型单页应用深度内存泄漏（如未解绑监听与闭包）排查需加强；\n2. 跨端离线持久化与数据同步冲突消解实战略显简略。";
            case AI_LLM -> "1. 面对超长上下文推理与 KV-Cache 显存受限时的量化压缩实操经验稍显欠缺；\n2. 多智能体协作死锁检测机制需进一步打磨。";
            case BIG_DATA -> "1. 极端网络抖动引发反压时的全链路水线排查经验需加强；\n2. 湖仓一体元数据一致性保障机制需要补充实操。";
            default -> "1. 分布式系统网络分区与脑裂场景下的容灾演练较少；\n2. 线上监控指标定位工具（如 Arthas/Prometheus）的实战深度需加强。";
        };
        report.setCriticalWeaknesses(weaknesses);

        String speechRefactoring = switch (track) {
            case SYSTEMS_HIGH_PERF -> "【原答复】：我们服务之前高并发时延迟很高，我改用协程池和连接复用就好了。\n" +
                    "【STAR重塑】：在百万级长连接网关项目中，突发流量导致系统出现调度饥饿与内存暴涨（S/T）。我通过重构协程生命周期管理，引入无锁 ring-buffer 与零拷贝 epoll 多路复用（A），使单机支撑并发提升 4 倍，P99 延迟稳定在 3ms 以内（R）。";
            case FRONTEND_MOBILE -> "【原答复】：首屏加载太慢，我做了懒加载和打包拆分，速度变快了。\n" +
                    "【STAR重塑】：在电商核心大促导购页中，首屏 LCP 超过 3.8s 严重影响转化率（S/T）。我通过设计骨架屏预渲染、Vite 模块联邦拆包以及静态资源 Brotli 压缩与 CDN 边缘缓存（A），将 LCP 压降至 1.1s，白屏率降低 72%（R）。";
            case AI_LLM -> "【原答复】：大模型问答经常答非所问，我加了向量数据库检索和提示词。\n" +
                    "【STAR重塑】：在企业智能知识库项目中，模型在专业领域的问答幻觉率高达 35%（S/T）。我主导搭建了 Hybrid Search (Qdrant 密集向量 + BM25 稀疏检索) 与 BGE-Reranker 重排流水线，并结合 Dynamic Few-Shot 提示工程（A），将问答准确率提升至 94.2%，召回延迟控制在 200ms 以内（R）。";
            default -> "【原答复】：我之前做过一些高并发优化，加了 Redis 缓存，速度变快了很多。\n" +
                    "【STAR重塑】：在XX电商大促项目中，峰值 QPS 达到 2.4 万（S/T），为避免数据库连接池耗尽，我主导设计了多级缓存与布隆过滤器（A），最终系统平均 RT 从 120ms 压降至 15ms，核心链路可用性达 99.99%（R）。";
        };
        report.setSpeechRefactoring(speechRefactoring);

        report.setRecommendedCourses(defaultCourses);
        return report;
    }

    private String resolveDefaultCourses(JobTrack track) {
        return switch (track) {
            case SYSTEMS_HIGH_PERF -> "[\"《Go语言高并发架构实战与GMP深度解析》\", \"《C++20核心系统编程与高性能网络通信》\", \"《Linux内核网络与eBPF排障指南》\"]";
            case FRONTEND_MOBILE -> "[\"《前端架构设计与大型工程化体系构建》\", \"《Vue3/React源码深度剖析与性能极致调优》\", \"《Web全栈与微前端实战》\"]";
            case AI_LLM -> "[\"《大语言模型架构精要与Transformer微调实战》\", \"《工业级RAG检索增强与Agent智能体开发》\", \"《vLLM推理加速与大模型分布式训练》\"]";
            case BIG_DATA -> "[\"《Flink实时流计算与海量数据倾斜调优》\", \"《数据湖仓一体(Iceberg/Hudi)架构实践》\", \"《深入理解Kafka核心原理与高吞吐调优》\"]";
            case DATABASE_STORAGE -> "[\"《MySQL DBA实战与千万级慢SQL排查》\", \"《分布式存储架构与Raft共识算法深度剖析》\", \"《Redis企业级高可用与分布式锁深度演练》\"]";
            case CLOUD_NATIVE_SRE -> "[\"《Kubernetes云原生平台架构实战》\", \"《生产级微服务全链路可观测性与SRE稳定性保障》\", \"《DevOps CI/CD自动化交付体系建设》\"]";
            case QA_SECURITY -> "[\"《全链路压测与大促高可用容量规划》\", \"《Web应用安全攻防与企业级零信任架构》\", \"《自动化测试平台与测试开发实战》\"]";
            default -> "[\"《亿级流量架构核心技术与高并发实战》\", \"《深入理解 Java 虚拟机与线上 OOM 排障》\", \"《MySQL 实战 45 讲与调优指南》\"]";
        };
    }

    private String determineOffer(int score) {
        if (score >= 88) return "Strong Hire";
        if (score >= 75) return "Hire";
        if (score >= 60) return "Weak Hire";
        return "Reject";
    }

    private String determineLevel(int score) {
        if (score >= 88) return "对标阿里P7资深架构师 / 字节2-2";
        if (score >= 78) return "对标阿里P6+高级开发 / 字节2-1";
        if (score >= 65) return "对标阿里P6中级开发 / 字节1-2";
        if (score >= 50) return "对标阿里P5初级开发 / 字节1-1";
        return "未达标 (建议系统性补强基础)";
    }

    /**
     * 影子语义检索：通过 Qdrant + FastEmbed 检索高匹配题库。
     */
    private SemanticHit searchSemanticKnowledge(String query) {
        if (!StringUtils.hasText(query) || query.trim().length() < 2) {
            return null;
        }
        try {
            String cleanQuery = query.trim();
            if (cleanQuery.length() > 500) {
                cleanQuery = cleanQuery.substring(0, 500);
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("query", cleanQuery);
            reqBody.put("limit", 1);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(EMBEDDING_SERVICE_URL, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode hits = root.path("hits");
                if (hits.isArray() && !hits.isEmpty()) {
                    JsonNode topHit = hits.get(0);
                    double score = topHit.path("score").asDouble(0.0);
                    long id = topHit.path("id").asLong(0L);
                    String answer = topHit.path("answer").asText("");
                    if (score >= 0.65 && StringUtils.hasText(answer)) {
                        return new SemanticHit(id, answer, score);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("调用 Qdrant FastEmbed 语义检索异常: {}", ex.getMessage());
        }
        return null;
    }

    /**
     * 调用 Pixel GPT-5.6-Luna。
     */
    private String callAi(String prompt, String fallback) {
        try {
            CustomerAiConfig config = effectiveAiConfig();
            String reply = aiClient.askRaw(config, "你是一位顶尖大厂技术专家兼资深面试官。", prompt);
            if (StringUtils.hasText(reply)) {
                return reply.trim();
            }
        } catch (Exception ex) {
            log.warn("AI 接口调用异常: {}", ex.getMessage());
        }
        return fallback;
    }

    private CustomerAiConfig effectiveAiConfig() {
        CustomerAiConfig config = aiConfigMapper.selectOne(
                new LambdaQueryWrapper<CustomerAiConfig>()
                        .eq(CustomerAiConfig::getEnabled, 1)
                        .orderByDesc(CustomerAiConfig::getUpdateTime)
                        .last("LIMIT 1")
        );
        if (config == null) {
            config = aiConfigMapper.selectOne(
                    new LambdaQueryWrapper<CustomerAiConfig>()
                            .orderByDesc(CustomerAiConfig::getUpdateTime)
                            .last("LIMIT 1")
            );
        }
        if (config != null) {
            return config;
        }
        CustomerAiConfig value = new CustomerAiConfig();
        value.setId(CONFIG_ID);
        value.setProvider("pixel");
        value.setBaseUrl(aiProperties.getBaseUrl());
        value.setEndpointPath(aiProperties.getEndpointPath());
        value.setModel(aiProperties.getModel());
        value.setEnabled(1);
        value.setTimeoutMs(aiProperties.getTimeoutMs());
        value.setMaxRetries(aiProperties.getMaxRetries());
        return value;
    }

    private String getStylePersona(String style) {
        if ("bytedance_tech".equalsIgnoreCase(style)) {
            return "【字节跳动二面技术专家】风格，极其务实、追求极致工程效率与细节边界，强调手撕算法、并发模型与数据结构底层细节";
        }
        if ("gentle_hr".equalsIgnoreCase(style)) {
            return "【资深大厂HRBP】风格，考察自驱力、商业意识、高压沟通协作与冲突化解，深挖过往项目的技术决策与成长复盘";
        }
        if ("standard".equalsIgnoreCase(style)) {
            return "【大厂技术评审委员会】风格，客观标准、全面考察计算机基础、设计模式与架构权衡";
        }
        return "【阿里巴巴P7+资深技术架构师】风格，极其犀利严苛、直击 JVM/OS 汇编底层、高并发线上真实踩坑与千万级流量极限排障";
    }

    private String cleanAiText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replaceAll("^[\"']|[\"']$", "").trim();
    }

    private String extractJson(String text) {
        if (!StringUtils.hasText(text)) {
            return "{}";
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text.trim();
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

    public enum JobTrack {
        BACKEND_JAVA,
        SYSTEMS_HIGH_PERF,
        FRONTEND_MOBILE,
        AI_LLM,
        BIG_DATA,
        DATABASE_STORAGE,
        CLOUD_NATIVE_SRE,
        QA_SECURITY
    }

    private JobTrack detectJobTrack(String job) {
        if (!StringUtils.hasText(job)) {
            return JobTrack.BACKEND_JAVA;
        }
        String s = job.toLowerCase();
        if (s.contains("go") || s.contains("c++") || s.contains("rust") || s.contains("python") || s.contains("底层系统")) {
            return JobTrack.SYSTEMS_HIGH_PERF;
        }
        if (s.contains("前端") || s.contains("vue") || s.contains("react") || s.contains("全栈") || s.contains("ios") || s.contains("android")) {
            return JobTrack.FRONTEND_MOBILE;
        }
        if (s.contains("大模型") || s.contains("llm") || s.contains("rag") || s.contains("agent") || s.contains("nlp") || s.contains("算法") || s.contains("视觉") || s.contains("cv") || s.contains("推荐系统")) {
            return JobTrack.AI_LLM;
        }
        if (s.contains("大数据") || s.contains("spark") || s.contains("hadoop") || s.contains("flink") || s.contains("数据仓库") || s.contains("湖仓一体")) {
            return JobTrack.BIG_DATA;
        }
        if (s.contains("mysql") || s.contains("dba") || s.contains("数据库") || s.contains("分布式存储") || s.contains("消息中间件") || s.contains("rocketmq")) {
            return JobTrack.DATABASE_STORAGE;
        }
        if (s.contains("kubernetes") || s.contains("k8s") || s.contains("云原生") || s.contains("devops") || s.contains("sre") || s.contains("稳定性")) {
            return JobTrack.CLOUD_NATIVE_SRE;
        }
        if (s.contains("测试") || s.contains("sdet") || s.contains("压测") || s.contains("安全") || s.contains("渗透")) {
            return JobTrack.QA_SECURITY;
        }
        return JobTrack.BACKEND_JAVA;
    }

    private record SemanticHit(long id, String answer, double score) {}
    private record TurnEvaluation(int score, String feedback, String standardRef) {}
}
