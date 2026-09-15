package com.share.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.crypto.AesCryptoUtil;
import com.share.common.redis.service.RedisService;
import com.share.common.security.utils.SecurityUtils;
import com.share.customer.config.CustomerAiProperties;
import com.share.customer.domain.CustomerAiCallLog;
import com.share.customer.domain.CustomerAiConfig;
import com.share.customer.domain.CustomerEvaluation;
import com.share.customer.domain.CustomerFaq;
import com.share.customer.domain.CustomerKnowledge;
import com.share.customer.domain.CustomerMessage;
import com.share.customer.domain.CustomerSession;
import com.share.customer.domain.dto.AiConfigRequest;
import com.share.customer.domain.dto.AiReplyRecordRequest;
import com.share.customer.domain.dto.CreateSessionRequest;
import com.share.customer.domain.dto.CustomerFaqRequest;
import com.share.customer.domain.dto.CustomerKnowledgeRequest;
import com.share.customer.domain.dto.EvaluationRequest;
import com.share.customer.domain.vo.CustomerAiConfigView;
import com.share.customer.domain.vo.CustomerChatResult;
import com.share.customer.domain.vo.CustomerStatisticsVO;
import com.share.customer.mapper.CustomerAiCallLogMapper;
import com.share.customer.mapper.CustomerAiConfigMapper;
import com.share.customer.mapper.CustomerEvaluationMapper;
import com.share.customer.mapper.CustomerFaqMapper;
import com.share.customer.mapper.CustomerKnowledgeMapper;
import com.share.customer.mapper.CustomerMessageMapper;
import com.share.customer.mapper.CustomerSessionMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.web.domain.AjaxResult;
import com.share.customer.domain.interview.InterviewSession;
import com.share.customer.domain.interview.dto.StartInterviewRequest;
import com.share.customer.domain.interview.vo.ResumeAnalysisVO;
import com.share.education.api.RemoteEducationService;
import org.springframework.context.annotation.Lazy;

/**
 * 客服核心业务服务。
 *
 * <p>这里集中处理 AI 会话生命周期、知识库降级和管理端查询，Controller 只负责协议适配。</p>
 */
@Service
public class CustomerService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomerService.class);
    private static final long CONFIG_ID = 1L;
    private static final String SECRET_KEY = "customer:ai:secret";
    private static final String FAQ_CACHE_PREFIX = "cs:faq:list:";
    private static final String AI_RATE_PREFIX = "cs:rate:ask:";
    private static final int AI_RATE_LIMIT = 10;
    private static final long AI_RATE_WINDOW_SECONDS = 60L;
    private static final int SESSION_CLOSED = 3;
    private static final int MESSAGE_USER = 1;
    private static final int MESSAGE_AI = 2;
    private static final int MESSAGE_SYSTEM = 4;
    private static final String EMBEDDING_SERVICE_URL = "http://zhiwen-embedding:8000/search";

    private final CustomerKnowledgeMapper knowledgeMapper;
    private final CustomerFaqMapper faqMapper;
    private final CustomerSessionMapper sessionMapper;
    private final CustomerMessageMapper messageMapper;
    private final CustomerEvaluationMapper evaluationMapper;
    private final CustomerAiConfigMapper aiConfigMapper;
    private final CustomerAiCallLogMapper aiCallLogMapper;
    private final CustomerAiClient aiClient;
    private final CustomerAiProperties aiProperties;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final RemoteEducationService remoteEducationService;
    private final IInterviewService interviewService;
    private final IUserResumeService userResumeService;

    public CustomerService(CustomerKnowledgeMapper knowledgeMapper, CustomerFaqMapper faqMapper,
            CustomerSessionMapper sessionMapper, CustomerMessageMapper messageMapper,
            CustomerEvaluationMapper evaluationMapper, CustomerAiConfigMapper aiConfigMapper,
            CustomerAiCallLogMapper aiCallLogMapper, CustomerAiClient aiClient,
            CustomerAiProperties aiProperties, RedisService redisService, ObjectMapper objectMapper,
            RestTemplate restTemplate, RemoteEducationService remoteEducationService,
            @Lazy IInterviewService interviewService, @Lazy IUserResumeService userResumeService) {
        this.knowledgeMapper = knowledgeMapper;
        this.faqMapper = faqMapper;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.evaluationMapper = evaluationMapper;
        this.aiConfigMapper = aiConfigMapper;
        this.aiCallLogMapper = aiCallLogMapper;
        this.aiClient = aiClient;
        this.aiProperties = aiProperties;
        this.redisService = redisService;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.remoteEducationService = remoteEducationService;
        this.interviewService = interviewService;
        this.userResumeService = userResumeService;
    }

    @Transactional
    public CustomerSession createSession(CreateSessionRequest request) {
        LocalDateTime now = LocalDateTime.now();
        CustomerSession session = new CustomerSession();
        session.setId(newId());
        session.setSessionNo("CS-" + now.toString().replace("-", "").replace(":", "").replace("T", "")
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        session.setUserId(currentUserId());
        String name = request == null ? null : request.getUserName();
        session.setUserName(StringUtils.hasText(name) ? name.trim() : currentUserName());
        session.setSource("AI客服");
        session.setStatus(0);
        session.setStartedAt(now);
        session.setUpdatedAt(now);
        session.setCreateTime(now);
        session.setUpdateTime(now);
        session.setDelFlag(0);
        session.setVersion(0);
        sessionMapper.insert(session);

        CustomerMessage welcome = newMessage(session.getId(), MESSAGE_SYSTEM, null, "智问学伴",
                "您好，我是智问学伴，可以帮您解答账号、课程、订单和学习方面的问题。", null, 0);
        messageMapper.insert(welcome);
        return session;
    }

    public IPage<CustomerSession> listMySessions(long pageNum, long pageSize) {
        return listMySessions(pageNum, pageSize, null);
    }

    public IPage<CustomerSession> listMySessions(long pageNum, long pageSize, Integer status) {
        Long userId = currentUserId();
        if (userId == null) {
            return new Page<>();
        }
        Page<CustomerSession> page = new Page<>(safePage(pageNum), safeSize(pageSize));
        LambdaQueryWrapper<CustomerSession> wrapper = new LambdaQueryWrapper<CustomerSession>()
                .eq(CustomerSession::getUserId, userId);
        if (status != null) {
            if (status == 4) {
                wrapper.eq(CustomerSession::getStatus, 4);
            } else {
                wrapper.ne(CustomerSession::getStatus, 4);
            }
        }
        wrapper.orderByDesc(CustomerSession::getUpdatedAt);
        return sessionMapper.selectPage(page, wrapper);
    }

    @Transactional
    public void deleteMySession(Long sessionId) {
        CustomerSession session = getSession(sessionId);
        assertOwner(session);
        sessionMapper.deleteById(sessionId);
    }

    @Transactional
    public CustomerSession archiveMySession(Long sessionId, boolean archive) {
        CustomerSession session = getSession(sessionId);
        assertOwner(session);
        session.setStatus(archive ? 4 : 0);
        session.setUpdatedAt(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        return session;
    }

    public CustomerSession getMySession(Long sessionId) {
        CustomerSession session = getSession(sessionId);
        assertOwner(session);
        enrichSession(session);
        return session;
    }

    public List<CustomerMessage> listMyMessages(Long sessionId) {
        CustomerSession session = getMySession(sessionId);
        return listMessages(session.getId());
    }

    public CustomerChatResult sendMessage(Long sessionId, String content) {
        return sendMessage(sessionId, content, null, null);
    }

    /**
     * 发送客服消息。用户端可传入当前会话临时使用的第三方 Pixel Key，
     * 服务端只在本次调用中使用该 Key，不写入任何存储。
     * 注意：本方法严禁加 @Transactional，防止耗时数秒到数十秒的外部大模型 HTTP 调用持续占用数据库连接池导致死锁。
     */
    public CustomerChatResult sendMessage(Long sessionId, String content, String requestApiKey,
            String requestModel) {
        CustomerSession session = getMySession(sessionId);
        if (SESSION_CLOSED == session.getStatus()) {
            throw new ServiceException("会话已结束，请新建会话后继续咨询");
        }
        String cleanContent = content == null ? "" : content.trim();
        if (!StringUtils.hasText(cleanContent)) {
            throw new ServiceException("消息内容不能为空");
        }
        enforceAskRateLimit();

        List<CustomerMessage> history = listMessages(session.getId());
        LocalDateTime now = LocalDateTime.now();
        CustomerMessage userMessage = newMessage(session.getId(), MESSAGE_USER, currentUserId(), currentUserName(),
                cleanContent, null, 0);
        userMessage.setCreateTime(now);
        messageMapper.insert(userMessage);

        CustomerAiConfig config = effectiveAiConfig();
        CustomerAiConfig requestConfig = requestConfig(config, requestApiKey, requestModel);
        long start = System.currentTimeMillis();
        AiReply remoteReply = null;
        boolean isAgentDeliberation = false;
        String answer = null;
        String aiModelUsed = null;
        boolean fallback = false;

        // 1. 优先尝试智能体自主操作意图识别与执行 (Agent Action Copilot)
        String actionReply = tryDispatchAgentAction(cleanContent);
        if (StringUtils.hasText(actionReply)) {
            answer = actionReply;
            isAgentDeliberation = true;
            aiModelUsed = "agent-copilot-action";
        }

        // 2. 尝试多智能体协同导学意图识别与跨微服务推演分发
        if (!isAgentDeliberation && isAgentDeliberationIntent(cleanContent)) {
            String targetRole = extractTargetRole(cleanContent);
            try {
                AjaxResult agentRes = remoteEducationService.orchestrateAgentRecommend(currentUserId(), targetRole, 4, SecurityConstants.INNER);
                if (agentRes != null && agentRes.isSuccess() && agentRes.get("data") != null) {
                    answer = formatAgentDeliberationReply(targetRole, agentRes.get("data"));
                    if (StringUtils.hasText(answer)) {
                        isAgentDeliberation = true;
                        aiModelUsed = "multi-agent-cluster";
                    }
                }
            } catch (Exception ex) {
                log.warn("调度教育多智能体推演异常，自动降级至通用大模型/FAQ: {}", ex.getMessage());
            }
        }

        // 2. 若未触发多智能体规划或调用异常，则执行标准 AI 客服 / FAQ 知识库答疑
        if (!isAgentDeliberation) {
            remoteReply = aiClient.ask(requestConfig, history, cleanContent, requestApiKey);
            fallback = remoteReply == null || !StringUtils.hasText(remoteReply.getContent());
            answer = fallback ? findLocalAnswer(cleanContent) : remoteReply.getContent();
            aiModelUsed = fallback ? "local-knowledge" : remoteReply.getModel();
        }

        CustomerMessage reply = newMessage(session.getId(), MESSAGE_AI, null, "智问学伴", answer,
                aiModelUsed, isAgentDeliberation ? 0 : (fallback ? 1 : 0));
        reply.setTokenUsage(fallback ? null : (remoteReply != null ? remoteReply.getTokenUsage() : 380));
        reply.setCreateTime(LocalDateTime.now());
        messageMapper.insert(reply);

        session.setStatus(0);
        session.setLastMessage(formatLastMessagePreview(answer));
        session.setUpdatedAt(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        CustomerAiCallLog callLog = new CustomerAiCallLog();
        callLog.setId(newId());
        callLog.setRequestNo("AI-" + UUID.randomUUID().toString().replace("-", ""));
        callLog.setSessionId(session.getId());
        callLog.setProvider(isAgentDeliberation ? "multi-agent" : (requestConfig == null ? "pixel" : requestConfig.getProvider()));
        callLog.setModel(aiModelUsed);
        callLog.setLatencyMs((int) Math.min(System.currentTimeMillis() - start, Integer.MAX_VALUE));
        callLog.setResultStatus(fallback ? 3 : 1);
        if (fallback) {
            callLog.setErrorCode("AI_FALLBACK");
            callLog.setErrorMessage("第三方 AI 未启用、未配置 Key 或调用失败，已使用本地知识库回答");
        }
        callLog.setCreateTime(LocalDateTime.now());
        callLog.setUpdateTime(LocalDateTime.now());
        aiCallLogMapper.insert(callLog);
        // 返回完整会话快照，用户端刷新本地消息时不会丢失刚刚提交的问题，
        // 管理端查看同一会话时也能立即看到完整问答链路。
        session.setMessages(listMessages(session.getId()));
        return new CustomerChatResult(session, reply);
    }

    /**
     * 复制可公开的 AI 配置并应用本次请求参数，避免修改从数据库读取的配置对象。
     */
    private CustomerAiConfig requestConfig(CustomerAiConfig base, String requestApiKey,
            String requestModel) {
        CustomerAiConfig value = new CustomerAiConfig();
        value.setProvider(base == null ? "pixel" : base.getProvider());
        value.setBaseUrl(base == null ? aiProperties.getBaseUrl() : base.getBaseUrl());
        value.setEndpointPath(base == null ? aiProperties.getEndpointPath() : base.getEndpointPath());
        value.setModel(base == null ? aiProperties.getModel() : base.getModel());
        value.setEnabled(base == null ? 0 : base.getEnabled());
        value.setTimeoutMs(base == null ? aiProperties.getTimeoutMs() : base.getTimeoutMs());
        value.setMaxRetries(base == null ? aiProperties.getMaxRetries() : base.getMaxRetries());
        value.setSystemPrompt(base == null ? aiProperties.getSystemPrompt() : base.getSystemPrompt());
        value.setApiKeyCiphertext(base == null ? null : base.getApiKeyCiphertext());

        // 用户明确输入 Key 即表示本次希望启用第三方 AI；不改变后台持久化配置的 enabled 状态。
        if (StringUtils.hasText(requestApiKey)) {
            value.setEnabled(1);
            if (StringUtils.hasText(requestModel)) {
                value.setModel(requestModel.trim());
            }
        }
        return value;
    }

    @Transactional
    public CustomerEvaluation saveEvaluation(Long sessionId, EvaluationRequest request) {
        CustomerSession session = getMySession(sessionId);
        if (request == null || request.getScore() == null) {
            throw new ServiceException("评分不能为空");
        }
        CustomerEvaluation evaluation = evaluationMapper.selectOne(new LambdaQueryWrapper<CustomerEvaluation>()
                .eq(CustomerEvaluation::getSessionId, session.getId()));
        boolean isNewEvaluation = evaluation == null;
        if (evaluation == null) {
            evaluation = new CustomerEvaluation();
            evaluation.setId(newId());
            evaluation.setSessionId(session.getId());
            evaluation.setCreateTime(LocalDateTime.now());
        }
        evaluation.setScore(request.getScore());
        try {
            evaluation.setTagsJson(request.getTags() == null ? "[]" : objectMapper.writeValueAsString(request.getTags()));
        } catch (Exception ex) {
            throw new ServiceException("评价标签格式不正确");
        }
        evaluation.setComment(request.getComment());
        evaluation.setUpdateTime(LocalDateTime.now());
        if (evaluation.getCreateTime() == null) {
            evaluation.setCreateTime(LocalDateTime.now());
        }
        if (isNewEvaluation) {
            evaluationMapper.insert(evaluation);
        } else {
            evaluationMapper.updateById(evaluation);
        }
        session.setSatisfactionScore(request.getScore());
        // 提交评价即完成本次 AI 服务。前端虽然会立即切换到“已结束”状态，
        // 这里也必须持久化会话状态，确保刷新页面和管理端统计结果保持一致。
        session.setStatus(SESSION_CLOSED);
        session.setClosedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        return evaluation;
    }

    public List<CustomerFaq> listPublicFaq(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        String cacheKey = FAQ_CACHE_PREFIX + safeLimit;
        try {
            List<CustomerFaq> cached = redisService.getCacheObject(cacheKey);
            if (cached != null) {
                return cached;
            }
        } catch (RuntimeException ignored) {
            // Redis 只做加速，缓存不可用时继续查询 MySQL。
        }
        List<CustomerFaq> result = faqMapper.selectList(new LambdaQueryWrapper<CustomerFaq>()
                .eq(CustomerFaq::getEnabled, 1)
                .orderByAsc(CustomerFaq::getSortNum)
                .last("limit " + safeLimit));
        try {
            redisService.setCacheObject(cacheKey, result, 5L, TimeUnit.MINUTES);
        } catch (RuntimeException ignored) {
            // Redis 只做加速，缓存写入失败不影响 FAQ 查询。
        }
        return result;
    }

    public List<CustomerMessage> listMessages(Long sessionId) {
        return messageMapper.selectList(new LambdaQueryWrapper<CustomerMessage>()
                .eq(CustomerMessage::getSessionId, sessionId)
                .orderByAsc(CustomerMessage::getCreateTime)
                .orderByAsc(CustomerMessage::getId));
    }

    public IPage<CustomerKnowledge> listKnowledge(String keyword, String category, Integer status,
            long pageNum, long pageSize) {
        Page<CustomerKnowledge> page = new Page<>(safePage(pageNum), safeSize(pageSize));
        LambdaQueryWrapper<CustomerKnowledge> wrapper = new LambdaQueryWrapper<CustomerKnowledge>()
                .like(StringUtils.hasText(keyword), CustomerKnowledge::getQuestion, keyword)
                .eq(StringUtils.hasText(category), CustomerKnowledge::getCategory, category)
                .eq(status != null, CustomerKnowledge::getStatus, status)
                .orderByDesc(CustomerKnowledge::getUpdateTime);
        return knowledgeMapper.selectPage(page, wrapper);
    }

    public CustomerKnowledge getKnowledge(Long id) {
        CustomerKnowledge value = knowledgeMapper.selectById(id);
        if (value == null) {
            throw new ServiceException("知识库条目不存在");
        }
        return value;
    }

    @Transactional
    public CustomerKnowledge saveKnowledge(CustomerKnowledgeRequest request) {
        CustomerKnowledge value = request == null || request.getId() == null
                ? new CustomerKnowledge() : getKnowledge(request.getId());
        LocalDateTime now = LocalDateTime.now();
        value.setQuestion(request.getQuestion().trim());
        value.setAnswer(request.getAnswer().trim());
        value.setKeywords(request.getKeywords());
        value.setCategory(request.getCategory().trim());
        value.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        value.setUpdateBy(currentUserId());
        value.setUpdateTime(now);
        if (value.getId() == null) {
            value.setId(newId());
            value.setCreateBy(currentUserId());
            value.setCreateTime(now);
            value.setDelFlag(0);
            value.setVersion(0);
            value.setHitCount(0);
            knowledgeMapper.insert(value);
        } else {
            knowledgeMapper.updateById(value);
        }
        return value;
    }

    @Transactional
    public void removeKnowledge(List<Long> ids) {
        if (ids != null) {
            ids.stream().filter(Objects::nonNull).forEach(knowledgeMapper::deleteById);
        }
    }

    public IPage<CustomerFaq> listFaq(String keyword, String category, Integer enabled,
            long pageNum, long pageSize) {
        Page<CustomerFaq> page = new Page<>(safePage(pageNum), safeSize(pageSize));
        LambdaQueryWrapper<CustomerFaq> wrapper = new LambdaQueryWrapper<CustomerFaq>()
                .like(StringUtils.hasText(keyword), CustomerFaq::getQuestion, keyword)
                .eq(StringUtils.hasText(category), CustomerFaq::getCategory, category)
                .eq(enabled != null, CustomerFaq::getEnabled, enabled)
                .orderByAsc(CustomerFaq::getSortNum)
                .orderByDesc(CustomerFaq::getUpdateTime);
        return faqMapper.selectPage(page, wrapper);
    }

    public CustomerFaq getFaq(Long id) {
        CustomerFaq value = faqMapper.selectById(id);
        if (value == null) {
            throw new ServiceException("常见问题不存在");
        }
        return value;
    }

    @Transactional
    public CustomerFaq saveFaq(CustomerFaqRequest request) {
        CustomerFaq value = request == null || request.getId() == null ? new CustomerFaq() : getFaq(request.getId());
        LocalDateTime now = LocalDateTime.now();
        value.setQuestion(request.getQuestion().trim());
        value.setAnswer(request.getAnswer().trim());
        value.setCategory(request.getCategory().trim());
        value.setSortNum(request.getSortNum() == null ? 0 : request.getSortNum());
        value.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
        value.setUpdateBy(currentUserId());
        value.setUpdateTime(now);
        if (value.getId() == null) {
            value.setId(newId());
            value.setCreateBy(currentUserId());
            value.setCreateTime(now);
            value.setDelFlag(0);
            value.setVersion(0);
            value.setHitCount(0);
            faqMapper.insert(value);
        } else {
            faqMapper.updateById(value);
        }
        evictFaqCache();
        return value;
    }

    @Transactional
    public void removeFaq(List<Long> ids) {
        if (ids != null) {
            ids.stream().filter(Objects::nonNull).forEach(faqMapper::deleteById);
        }
        evictFaqCache();
    }

    public IPage<CustomerSession> listAllSessions(String keyword, Integer status, long pageNum, long pageSize) {
        Page<CustomerSession> page = new Page<>(safePage(pageNum), safeSize(pageSize));
        LambdaQueryWrapper<CustomerSession> wrapper = new LambdaQueryWrapper<CustomerSession>()
                .eq(status != null, CustomerSession::getStatus, status)
                .orderByDesc(CustomerSession::getUpdatedAt);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like(CustomerSession::getSessionNo, keyword)
                    .or().like(CustomerSession::getUserName, keyword)
                    .or().like(CustomerSession::getLastMessage, keyword));
        }
        return sessionMapper.selectPage(page, wrapper);
    }

    public CustomerSession getAdminSession(Long sessionId) {
        CustomerSession session = getSession(sessionId);
        enrichSession(session);
        return session;
    }

    public List<CustomerMessage> listAdminMessages(Long sessionId) {
        getAdminSession(sessionId);
        return listMessages(sessionId);
    }

    /**
     * 浏览器直连第三方 Pixel API 时，仍把问答记录写入 MySQL，保证管理端会话记录和统计完整。
     */
    @Transactional
    public CustomerChatResult recordExternalReply(Long sessionId, AiReplyRecordRequest request) {
        CustomerSession session = sessionId == null ? createSession(new CreateSessionRequest()) : getMySession(sessionId);
        if (SESSION_CLOSED == session.getStatus()) {
            throw new ServiceException("会话已结束，请新建会话后继续咨询");
        }
        String question = request == null || request.getQuestion() == null ? "" : request.getQuestion().trim();
        String answer = request == null || request.getAnswer() == null ? "" : request.getAnswer().trim();
        if (!StringUtils.hasText(question) || !StringUtils.hasText(answer)) {
            throw new ServiceException("问题和 AI 回复不能为空");
        }
        enforceAskRateLimit();
        LocalDateTime now = LocalDateTime.now();
        CustomerMessage userMessage = newMessage(session.getId(), MESSAGE_USER, currentUserId(), currentUserName(), question, null, 0);
        userMessage.setCreateTime(now);
        messageMapper.insert(userMessage);
        CustomerMessage reply = newMessage(session.getId(), MESSAGE_AI, null, "智问学伴", answer, "pixel", 0);
        reply.setCreateTime(LocalDateTime.now());
        messageMapper.insert(reply);
        session.setStatus(0);
        session.setLastMessage(formatLastMessagePreview(answer));
        session.setUpdatedAt(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        session.setMessages(listMessages(session.getId()));
        return new CustomerChatResult(session, reply);
    }

    @Transactional
    public CustomerSession closeSession(Long sessionId) {
        CustomerSession session = getAdminSession(sessionId);
        if (SESSION_CLOSED != session.getStatus()) {
            session.setStatus(SESSION_CLOSED);
            session.setClosedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
        return session;
    }

    public CustomerStatisticsVO statistics() {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        CustomerStatisticsVO value = new CustomerStatisticsVO();
        value.setTotalSessions(sessionMapper.selectCount(new LambdaQueryWrapper<>()));
        value.setActiveSessions(sessionMapper.selectCount(new LambdaQueryWrapper<CustomerSession>()
                .ne(CustomerSession::getStatus, SESSION_CLOSED)));
        value.setTodaySessions(sessionMapper.selectCount(new LambdaQueryWrapper<CustomerSession>()
                .ge(CustomerSession::getCreateTime, dayStart)));
        value.setTotalMessages(messageMapper.selectCount(new LambdaQueryWrapper<>()));
        value.setAiMessages(messageMapper.selectCount(new LambdaQueryWrapper<CustomerMessage>()
                .eq(CustomerMessage::getMessageType, MESSAGE_AI)));
        value.setFallbackMessages(messageMapper.selectCount(new LambdaQueryWrapper<CustomerMessage>()
                .eq(CustomerMessage::getMessageType, MESSAGE_AI)
                .eq(CustomerMessage::getIsFallback, 1)));
        value.setKnowledgeCount(knowledgeMapper.selectCount(new LambdaQueryWrapper<>()));
        value.setFaqCount(faqMapper.selectCount(new LambdaQueryWrapper<CustomerFaq>()
                .eq(CustomerFaq::getEnabled, 1)));
        List<CustomerEvaluation> evaluations = evaluationMapper.selectList(new LambdaQueryWrapper<>());
        double average = evaluations.stream().filter(item -> item.getScore() != null)
                .collect(Collectors.averagingInt(CustomerEvaluation::getScore));
        value.setAverageScore(BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
        long totalSessions = value.getTotalSessions() == null ? 0 : value.getTotalSessions();
        value.setAiResolved(sessionMapper.selectCount(new LambdaQueryWrapper<CustomerSession>()
                .eq(CustomerSession::getStatus, SESSION_CLOSED)));
        value.setSatisfactionRate((int) Math.round(average * 20));
        value.setAverageMessages(totalSessions == 0 ? 0 : (int) Math.round((double) value.getTotalMessages() / totalSessions));
        value.setTopQuestions(topQuestions());
        value.setTrend(trend());
        return value;
    }

    public CustomerAiConfigView getAiConfigView() {
        CustomerAiConfig config = effectiveAiConfig();
        CustomerAiConfigView view = new CustomerAiConfigView();
        view.setId(config.getId());
        view.setProvider(config.getProvider());
        view.setBaseUrl(config.getBaseUrl());
        view.setEndpointPath(config.getEndpointPath());
        view.setModel(config.getModel());
        view.setEnabled(config.getEnabled());
        view.setTimeoutMs(config.getTimeoutMs());
        view.setMaxRetries(config.getMaxRetries());
        view.setSystemPrompt(config.getSystemPrompt());
        view.setApiKeyConfigured(StringUtils.hasText(resolveSecret()));
        return view;
    }

    @Transactional
    public CustomerAiConfigView saveAiConfig(AiConfigRequest request) {
        validatePixelAddress(request.getBaseUrl(), request.getEndpointPath());
        CustomerAiConfig config = effectiveAiConfig();
        boolean isNew = false;
        if (config == null || config.getId() == null || aiConfigMapper.selectById(config.getId()) == null) {
            config = new CustomerAiConfig();
            config.setId(CONFIG_ID);
            config.setProvider("pixel");
            config.setCreateTime(LocalDateTime.now());
            config.setCreateBy(currentUserId());
            config.setVersion(0);
            isNew = true;
        }
        config.setProvider("pixel");
        config.setBaseUrl(request.getBaseUrl().trim());
        config.setEndpointPath(request.getEndpointPath().trim().startsWith("/")
                ? request.getEndpointPath().trim() : "/" + request.getEndpointPath().trim());
        config.setModel(request.getModel().trim());
        config.setEnabled(request.getEnabled() == null ? 0 : request.getEnabled());
        config.setTimeoutMs(request.getTimeoutMs() == null ? 30000 : request.getTimeoutMs());
        config.setMaxRetries(request.getMaxRetries() == null ? 1 : request.getMaxRetries());
        config.setSystemPrompt(request.getSystemPrompt());
        config.setUpdateBy(currentUserId());
        config.setUpdateTime(LocalDateTime.now());
        // Key 严禁明文入库；通过 AES 强加密持久化至 api_key_ciphertext，运行时写入 Redis 缓存。
        if (StringUtils.hasText(request.getApiKey())) {
            String cleanKey = request.getApiKey().trim();
            config.setApiKeyCiphertext(AesCryptoUtil.encrypt(cleanKey));
            redisService.setCacheObject(SECRET_KEY, cleanKey);
        }
        if (config.getCreateTime() == null) {
            config.setCreateTime(LocalDateTime.now());
        }
        if (isNew) {
            aiConfigMapper.insert(config);
        } else {
            aiConfigMapper.updateById(config);
        }
        return getAiConfigView();
    }

    public String testAi(String message) {
        CustomerAiConfig config = effectiveAiConfig();
        if (config == null || !Integer.valueOf(1).equals(config.getEnabled())) {
            throw new ServiceException("请先启用第三方 Pixel AI");
        }
        AiReply reply = aiClient.ask(config, Collections.emptyList(), message);
        if (reply == null || !StringUtils.hasText(reply.getContent())) {
            throw new ServiceException("第三方 AI 调用失败，请检查 API 地址、接口路径和 API Key");
        }
        return reply.getContent();
    }

    private CustomerSession getSession(Long sessionId) {
        if (sessionId == null) {
            throw new ServiceException("会话编号不能为空");
        }
        CustomerSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new ServiceException("客服会话不存在");
        }
        return session;
    }

    private void enrichSession(CustomerSession session) {
        session.setMessages(listMessages(session.getId()));
        CustomerEvaluation evaluation = evaluationMapper.selectOne(new LambdaQueryWrapper<CustomerEvaluation>()
                .eq(CustomerEvaluation::getSessionId, session.getId()));
        if (evaluation != null && StringUtils.hasText(evaluation.getTagsJson())) {
            try {
                evaluation.setTags(objectMapper.readValue(evaluation.getTagsJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
            } catch (Exception ignored) {
                evaluation.setTags(Collections.emptyList());
            }
        }
        session.setEvaluation(evaluation);
    }

    private List<CustomerStatisticsVO.TopQuestion> topQuestions() {
        Map<String, Long> counts = new HashMap<>();
        messageMapper.selectList(new LambdaQueryWrapper<CustomerMessage>()
                .eq(CustomerMessage::getMessageType, MESSAGE_USER)
                .orderByDesc(CustomerMessage::getCreateTime)
                .last("limit 500"))
                .forEach(item -> {
                    String question = item.getContent() == null ? "" : item.getContent().trim();
                    if (StringUtils.hasText(question)) counts.merge(question, 1L, Long::sum);
                });
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(item -> new CustomerStatisticsVO.TopQuestion(item.getKey(), item.getValue()))
                .toList();
    }

    private List<CustomerStatisticsVO.TrendItem> trend() {
        List<CustomerStatisticsVO.TrendItem> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int offset = 4; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            long sessions = sessionMapper.selectCount(new LambdaQueryWrapper<CustomerSession>()
                    .ge(CustomerSession::getCreateTime, start).lt(CustomerSession::getCreateTime, end));
            long resolved = sessionMapper.selectCount(new LambdaQueryWrapper<CustomerSession>()
                    .eq(CustomerSession::getStatus, SESSION_CLOSED)
                    .ge(CustomerSession::getUpdateTime, start).lt(CustomerSession::getUpdateTime, end));
            result.add(new CustomerStatisticsVO.TrendItem(date.toString().substring(5), sessions, resolved));
        }
        return result;
    }

    private void assertOwner(CustomerSession session) {
        Long userId = currentUserId();
        if (userId == null || (!SecurityUtils.isAdmin(userId) && !Objects.equals(userId, session.getUserId()))) {
            throw new ServiceException("无权访问该客服会话");
        }
    }

    private CustomerMessage newMessage(Long sessionId, int type, Long senderId, String senderName,
            String content, String model, int fallback) {
        CustomerMessage message = new CustomerMessage();
        message.setId(newId());
        message.setSessionId(sessionId);
        message.setMessageType(type);
        message.setSenderId(senderId);
        message.setSenderName(senderName);
        message.setContent(content);
        message.setAiModel(model);
        message.setIsFallback(fallback);
        message.setCreateTime(LocalDateTime.now());
        return message;
    }

    private String findLocalAnswer(String input) {
        // 1. 优先使用 Qdrant 向量语义相似度检索 (FastEmbed / BGE-small-zh)
        String semanticAnswer = findSemanticAnswer(input);
        if (StringUtils.hasText(semanticAnswer)) {
            return semanticAnswer;
        }

        // 2. 降级走原有的 FAQ 与关键词匹配机制
        String normalized = normalize(input);
        LocalAnswer best = null;
        List<CustomerFaq> faqs = faqMapper.selectList(new LambdaQueryWrapper<CustomerFaq>()
                .eq(CustomerFaq::getEnabled, 1).orderByAsc(CustomerFaq::getSortNum).last("limit 200"));
        for (CustomerFaq faq : faqs) {
            int score = matchScore(normalized, faq.getQuestion(), null);
            if (score > 0 && (best == null || score > best.score)) {
                best = new LocalAnswer(score, faq.getAnswer(), faq.getId(), true);
            }
        }
        List<CustomerKnowledge> knowledge = knowledgeMapper.selectList(new LambdaQueryWrapper<CustomerKnowledge>()
                .eq(CustomerKnowledge::getStatus, 1).orderByDesc(CustomerKnowledge::getUpdateTime).last("limit 2000"));
        for (CustomerKnowledge item : knowledge) {
            int score = matchScore(normalized, item.getQuestion(), item.getKeywords());
            if (score > 0 && (best == null || score > best.score)) {
                best = new LocalAnswer(score, item.getAnswer(), item.getId(), false);
            }
        }
        if (best != null) {
            if (best.faq) {
                CustomerFaq faq = faqMapper.selectById(best.id);
                if (faq != null) {
                    faq.setHitCount((faq.getHitCount() == null ? 0 : faq.getHitCount()) + 1);
                    faqMapper.updateById(faq);
                }
            } else {
                CustomerKnowledge item = knowledgeMapper.selectById(best.id);
                if (item != null) {
                    item.setHitCount((item.getHitCount() == null ? 0 : item.getHitCount()) + 1);
                    knowledgeMapper.updateById(item);
                }
            }
            return best.answer;
        }
        return "抱歉，我暂时没有在知识库中找到完全匹配的答案。你可以换一种说法描述问题，或留下具体的课程、订单和账号信息，我会继续帮你排查。";
    }

    /**
     * 通过 Qdrant 向量搜索引擎与 FastEmbed 进行语义近邻检索。
     * 当语义相似度余弦得分 >= 0.70 时判定为高置信度语义命中，直接返回答案。
     */
    private String findSemanticAnswer(String input) {
        if (!StringUtils.hasText(input) || input.trim().length() < 2) {
            return null;
        }
        try {
            String cleanQuery = input.trim();
            if (cleanQuery.length() > 500) {
                cleanQuery = cleanQuery.substring(0, 500);
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("query", cleanQuery);
            reqBody.put("limit", 1);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(
                    EMBEDDING_SERVICE_URL, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode hits = root.path("hits");
                if (hits.isArray() && hits.size() > 0) {
                    JsonNode topHit = hits.get(0);
                    double score = topHit.path("score").asDouble(0.0);
                    String answer = topHit.path("answer").asText("");
                    long id = topHit.path("id").asLong(0L);
                    if (score >= 0.70 && StringUtils.hasText(answer)) {
                        try {
                            if (id > 0) {
                                CustomerKnowledge item = knowledgeMapper.selectById(id);
                                if (item != null) {
                                    item.setHitCount((item.getHitCount() == null ? 0 : item.getHitCount()) + 1);
                                    knowledgeMapper.updateById(item);
                                }
                            }
                        } catch (Exception ignored) {
                        }
                        return answer;
                    }
                }
            }
        } catch (Exception ex) {
            // 异常或服务网络未就绪时静默跳过，无缝降级到本地关键字与FAQ匹配
        }
        return null;
    }

    private String formatLastMessagePreview(String answer) {
        if (!StringUtils.hasText(answer)) {
            return "";
        }
        String trimmed = answer.trim();
        if (trimmed.length() > 500) {
            return trimmed.substring(0, 497) + "...";
        }
        return trimmed;
    }

    private int matchScore(String normalizedInput, String question, String keywords) {
        if (!StringUtils.hasText(normalizedInput)) {
            return 0;
        }
        int score = 0;
        String normalizedQuestion = normalize(question);
        if (normalizedInput.contains(normalizedQuestion) || normalizedQuestion.contains(normalizedInput)) {
            score += 10;
        }
        if (StringUtils.hasText(keywords)) {
            for (String keyword : keywords.split("[,，、;；\\s]+")) {
                String normalizedKeyword = normalize(keyword);
                if (normalizedKeyword.length() >= 2 && normalizedInput.contains(normalizedKeyword)) {
                    score += 3;
                }
            }
        }
        return score;
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase()
                .replaceAll("[\\p{Punct}\\s，。！？；：、“”‘’（）【】《》]", "");
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
        value.setEnabled(StringUtils.hasText(resolveSecret()) ? 1 : 0);
        value.setTimeoutMs(aiProperties.getTimeoutMs());
        value.setMaxRetries(aiProperties.getMaxRetries());
        value.setSystemPrompt(aiProperties.getSystemPrompt());
        return value;
    }

    private String resolveSecret() {
        try {
            String value = redisService.getCacheObject(SECRET_KEY);
            if (StringUtils.hasText(value)) {
                return value;
            }
        } catch (RuntimeException ignored) {
            // Redis 不可用时仍允许使用本地知识库；环境 Key 作为后备。
        }
        // 若 Redis 缓存为空，尝试从持久化数据库解密恢复
        try {
            CustomerAiConfig config = aiConfigMapper.selectOne(
                    new LambdaQueryWrapper<CustomerAiConfig>()
                            .eq(CustomerAiConfig::getEnabled, 1)
                            .orderByDesc(CustomerAiConfig::getUpdateTime)
                            .last("LIMIT 1")
            );
            if (config == null) {
                config = aiConfigMapper.selectById(CONFIG_ID);
            }
            if (config != null && StringUtils.hasText(config.getApiKeyCiphertext())) {
                String decrypted = AesCryptoUtil.decrypt(config.getApiKeyCiphertext());
                if (StringUtils.hasText(decrypted)) {
                    try {
                        redisService.setCacheObject(SECRET_KEY, decrypted);
                    } catch (Exception ignored) {}
                    return decrypted;
                }
            }
        } catch (Exception ex) {
            // 解密异常不阻塞主流程
        }
        return aiProperties.getSecret();
    }

    /**
     * 单用户 AI 请求限流。Redis 不可用时放行请求，让本地知识库降级能力仍可用。
     */
    private void enforceAskRateLimit() {
        Long userId = currentUserId();
        if (userId == null) {
            return;
        }
        try {
            long count = redisService.increment(AI_RATE_PREFIX + userId, AI_RATE_WINDOW_SECONDS, TimeUnit.SECONDS);
            if (count > AI_RATE_LIMIT) {
                throw new ServiceException("咨询请求过于频繁，请稍后再试");
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (RuntimeException ignored) {
            // Redis 是可选基础设施，短时不可用时仍允许服务使用本地知识库。
        }
    }

    private void evictFaqCache() {
        try {
            // 精确清除常用 limit 缓存键，避免在 Redis 单线程中执行全库阻塞式的 KEYS 命令
            List<String> cacheKeys = List.of(
                    FAQ_CACHE_PREFIX + "5",
                    FAQ_CACHE_PREFIX + "10",
                    FAQ_CACHE_PREFIX + "15",
                    FAQ_CACHE_PREFIX + "20",
                    FAQ_CACHE_PREFIX + "30",
                    FAQ_CACHE_PREFIX + "50"
            );
            redisService.deleteObject(cacheKeys);
        } catch (RuntimeException ignored) {
            // 缓存失效失败不会影响 FAQ 数据已经写入 MySQL 的结果。
        }
    }

    private void validatePixelAddress(String baseUrl, String endpointPath) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new ServiceException("AI 地址不能为空");
        }
        try {
            URI uri = URI.create(baseUrl.trim());
            String scheme = uri.getScheme();
            if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
                throw new ServiceException("AI 地址必须使用 http 或 https 协议");
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new ServiceException("AI 域名不能为空");
            }
            String allowed = aiProperties.getAllowedHosts();
            if (allowed != null && !allowed.isBlank() && !"*".equals(allowed.trim())) {
                boolean match = false;
                for (String h : allowed.split(",")) {
                    String trimmed = h.trim();
                    if (trimmed.equalsIgnoreCase(host) || host.endsWith("." + trimmed)) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    throw new ServiceException("AI 地址域名不在系统允许的白名单范围内: " + host);
                }
            }
        } catch (IllegalArgumentException ex) {
            throw new ServiceException("AI 地址格式不正确");
        }
        if (!StringUtils.hasText(endpointPath)) {
            throw new ServiceException("接口路径不能为空");
        }
    }

    private Long currentUserId() {
        Long id = SecurityUtils.getUserId();
        return id != null && id > 0 ? id : null;
    }

    private String currentUserName() {
        String name = SecurityUtils.getUsername();
        return StringUtils.hasText(name) ? name : "用户";
    }

    private long newId() {
        return IdWorker.getId();
    }

    private long safePage(long page) {
        return page < 1 ? 1 : Math.min(page, 100000);
    }

    private long safeSize(long size) {
        return size < 1 ? 10 : Math.min(size, 100);
    }

    private boolean isAgentDeliberationIntent(String content) {
        if (!StringUtils.hasText(content)) return false;
        String lower = content.toLowerCase();
        // 排除常规账号/订单客服操作
        if (lower.contains("退款") || lower.contains("退课") || lower.contains("开票") || lower.contains("密码")
                || lower.contains("发票") || lower.contains("支付失败") || lower.contains("订单号")) {
            return false;
        }
        // 匹配路线规划、课程推荐与学情进阶意图
        return lower.contains("路线") || lower.contains("路径") || lower.contains("规划")
                || lower.contains("推荐") || lower.contains("学什么") || lower.contains("怎么学")
                || lower.contains("如何进阶") || lower.contains("学习方案") || lower.contains("学习计划")
                || lower.contains("成长图谱") || lower.contains("选课") || lower.contains("想转行");
    }

    private String extractTargetRole(String content) {
        if (!StringUtils.hasText(content)) return "Java 全栈架构师";
        String lower = content.toLowerCase();
        if (lower.contains("大模型") || lower.contains("大语言模型") || lower.contains("语言模型") || lower.contains("llm") || lower.contains("langchain")
                || lower.contains("prompt") || lower.contains("rag") || lower.contains("ai应用")
                || lower.contains("人工智能")) {
            return "大语言模型应用工程师";
        }
        if (lower.contains("go") || lower.contains("golang") || lower.contains("云原生")
                || lower.contains("k8s") || lower.contains("docker") || lower.contains("容器")) {
            return "Go 云原生架构师";
        }
        if (lower.contains("前端") || lower.contains("vue") || lower.contains("react")
                || lower.contains("ts") || lower.contains("typescript") || lower.contains("web")) {
            return "Web 前端架构专家";
        }
        if (lower.contains("大数据") || lower.contains("spark") || lower.contains("flink")
                || lower.contains("数仓") || lower.contains("hadoop")) {
            return "大数据流批一体工程师";
        }
        if (lower.contains("鸿蒙") || lower.contains("flutter") || lower.contains("移动端") || lower.contains("安卓") || lower.contains("ios")) {
            return "移动与跨端开发工程师";
        }
        return "Java 全栈架构师";
    }

    @SuppressWarnings("unchecked")
    private String formatAgentDeliberationReply(String targetRole, Object data) {
        if (!(data instanceof Map)) return null;
        Map<String, Object> map = (Map<String, Object>) data;

        StringBuilder sb = new StringBuilder();
        sb.append("🎯 **智问学伴多智能体协同导学系统已为您就绪！**\n\n");
        sb.append("已调动【画像/探针/召回/大纲知识拆解/DAG规划/审判反思/证据链】6 大协同智能体，为您深度定制【")
          .append(targetRole).append("】成长路线方案：\n\n");

        // 1. 4 阶段拓扑计划
        Object pathObj = map.get("learningPath");
        if (pathObj instanceof Map) {
            Map<String, Object> path = (Map<String, Object>) pathObj;
            Object stagesObj = path.get("stages");
            if (stagesObj instanceof List) {
                sb.append("🗺️ **4 阶段进阶拓扑成长图谱**：\n");
                List<Map<String, Object>> stages = (List<Map<String, Object>>) stagesObj;
                int idx = 1;
                for (Map<String, Object> stage : stages) {
                    String name = stage.get("stageName") != null ? stage.get("stageName").toString() : ("阶段 " + idx);
                    String desc = stage.get("description") != null ? stage.get("description").toString() : "";
                    Object hours = stage.get("stageHours");
                    sb.append("• **阶段 ").append(idx++).append("：").append(name).append("**");
                    if (hours != null) {
                        sb.append(" (").append(hours).append("课时)");
                    }
                    if (StringUtils.hasText(desc)) {
                        sb.append(" - ").append(desc);
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }

        // 2. 审判质检报告
        Object criticObj = map.get("criticReport");
        if (criticObj instanceof Map) {
            Map<String, Object> critic = (Map<String, Object>) criticObj;
            String level = critic.get("verdictLevel") != null ? critic.get("verdictLevel").toString() : "卓越 (A+)";
            Object score = critic.get("overallScore") != null ? critic.get("overallScore") : 100;
            sb.append("⚖️ **审判智能体质检评级**：").append(level).append(" · ").append(score).append("分（DAG 拓扑合规无先修倒置）\n\n");
        }

        // 3. 推荐课程精选
        Object recsObj = map.get("recommendations");
        if (recsObj instanceof List) {
            List<Map<String, Object>> recs = (List<Map<String, Object>>) recsObj;
            if (!recs.isEmpty()) {
                sb.append("💡 **专属优选核心必修课**：\n");
                int rIdx = 1;
                for (Map<String, Object> c : recs) {
                    String title = c.get("title") != null ? c.get("title").toString() : "";
                    String reason = c.get("recommendReason") != null ? c.get("recommendReason").toString() : "";
                    sb.append(rIdx++).append(". 《").append(title).append("》");
                    if (StringUtils.hasText(reason)) {
                        sb.append(" —— ").append(reason);
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }

        sb.append("🚀 **全景大屏联动**：\n已为您同步生成全景拓扑看板，点击下方卡片或前往首页即可在「AI 协同推演仪表盘 (HUD)」中全屏研读与人机微调！\n");
        sb.append("[ACTION_VIEW_PATH:").append(targetRole).append("]");
        return sb.toString();
    }

    private boolean isCrossUserAttempt(String lower) {
        return lower.contains("其他人") || lower.contains("别的用户") || lower.contains("其他用户")
                || lower.contains("别的人") || lower.contains("查张三") || lower.contains("查李四")
                || (lower.contains("用户") && (lower.contains("id") || lower.contains("2") || lower.contains("3") || lower.contains("4") || lower.contains("5") || lower.contains("10")));
    }

    private String tryDispatchAgentAction(String content) {
        if (!StringUtils.hasText(content)) return null;
        String lower = content.toLowerCase();

        // 0. 租户与跨用户越权防御拦截 (Anti-IDOR & User Isolation Shield)
        if (isCrossUserAttempt(lower)) {
            Long userId = currentUserId();
            String userName = currentUserName();
            return "🔒 **系统安全与数据隐私保护拦截**\n\n"
                    + "智问学伴系统启用了严格的**租户与用户数据隐私隔离防护机制**：\n"
                    + "• **安全准则**：系统严格遵循权限隔离规范，智能体仅有权操作和展示属于您当前认证账号的数据，**严禁跨账号访问或操作其他学员的订单、简历、考场记录或资产**；\n"
                    + "• **当前认证账户**：【" + (userName != null ? userName : "当前学员") + "】（用户 ID: " + userId + "）；\n\n"
                    + "智能体将仅为您本人处理属于您名下的业务。如需管理您本人的数据，请直接对我说“查我的订单”、“看我的面试报告”或“帮我诊断简历”。";
        }

        // 1. 历史模拟面试复盘与成绩查询 (Interview report)
        if (isInterviewReportIntent(lower)) {
            return handleInterviewReportAction();
        }

        // 2. 模拟面试开考意图 (Interview launch)
        if (isInterviewActionIntent(lower)) {
            return handleInterviewAction(lower);
        }

        // 3. 个人订单与交易管理意图 (Order manage)
        if (isOrderManageIntent(lower)) {
            return handleOrderManageAction(content);
        }

        // 4. 优惠券领取与福利中心意图 (Coupon center)
        if (isCouponCenterIntent(lower)) {
            return handleCouponCenterAction();
        }

        // 5. 购物车资产查看意图 (Cart view)
        if (isCartViewIntent(lower)) {
            return handleCartViewAction();
        }

        // 6. 课程购买 / 加购 / 选购意图 (Course purchase)
        if (isCoursePurchaseIntent(lower)) {
            return handleCoursePurchaseAction(content);
        }

        // 7. 每日打卡签到意图 (Sign in)
        if (isSignInActionIntent(lower)) {
            return handleSignInAction();
        }

        // 8. 赛季学霸天梯榜与积分资产意图 (Points ranking)
        if (isPointsRankingIntent(lower)) {
            return handlePointsRankingAction();
        }

        // 9. 考试成绩与测验考核意图 (Exam query)
        if (isExamQueryIntent(lower)) {
            return handleExamQueryAction();
        }

        // 10. 简历诊断与更新意图 (Resume diagnosis)
        if (isResumeActionIntent(lower)) {
            return handleResumeAction();
        }

        // 11. 随堂速记与个人笔记意图 (Note quick)
        if (isNoteQuickIntent(lower)) {
            return handleNoteQuickAction(content);
        }

        // 12. 学员动态学习画像与技能雷达 (Learning portrait)
        if (isLearningPortraitIntent(lower)) {
            return handleLearningPortraitAction();
        }

        // 13. 课表与继续学习意图 (Continue learning)
        if (isLearningActionIntent(lower)) {
            return handleLearningAction();
        }

        // 14. 页面穿梭与路由导航意图 (Page navigator)
        if (isPageNavigatorIntent(lower)) {
            return handlePageNavigatorAction(lower);
        }

        return null;
    }

    private boolean isInterviewActionIntent(String lower) {
        return lower.contains("模拟面试") || lower.contains("我要面试") || lower.contains("开一场面试")
                || lower.contains("帮我开一场面试") || lower.contains("面试考场") || lower.contains("开始面试")
                || lower.contains("想面试") || lower.contains("阿里面试") || lower.contains("java面试")
                || lower.contains("高并发面试") || (lower.contains("面试") && (lower.contains("开") || lower.contains("来一场") || lower.contains("开始")));
    }

    private String handleInterviewAction(String lower) {
        String targetJob = "Java高级开发工程师";
        if (lower.contains("大模型") || lower.contains("llm") || lower.contains("ai")) {
            targetJob = "大语言模型应用工程师";
        } else if (lower.contains("go") || lower.contains("golang") || lower.contains("云原生")) {
            targetJob = "Go云原生架构师";
        } else if (lower.contains("前端") || lower.contains("vue") || lower.contains("react")) {
            targetJob = "Web前端架构专家";
        } else if (lower.contains("大数据") || lower.contains("spark") || lower.contains("flink")) {
            targetJob = "大数据流批一体工程师";
        }

        String company = "大厂通用";
        if (lower.contains("阿里")) company = "阿里巴巴";
        else if (lower.contains("字节")) company = "字节跳动";
        else if (lower.contains("腾讯")) company = "腾讯科技";
        else if (lower.contains("美团")) company = "美团";
        else if (lower.contains("百度")) company = "百度";

        try {
            StartInterviewRequest req = new StartInterviewRequest();
            req.setTargetJob(targetJob);
            req.setCompanyTarget(company);
            req.setInterviewerStyle("p7_architect");
            req.setTotalTurns(20);
            InterviewSession newSession = interviewService.startSession(req);
            Long sId = newSession.getId();

            Map<String, Object> card = new HashMap<>();
            card.put("action", "interview_launch");
            card.put("sessionId", String.valueOf(sId));
            card.put("targetJob", targetJob);
            card.put("company", company);
            card.put("interviewerStyle", "p7_architect");
            card.put("roundInfo", "全真三环节 20 题 60 分钟限时架构");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("🎯 **全真 AI 模拟面试考场已为您极速就绪！**\n\n");
            sb.append("已为您成功开辟面向【").append(company).append("】的【").append(targetJob).append("】全真考核专场：\n");
            sb.append("• **考核架构**：全真三环节 20 题 60 分钟限时考核（环节一：破题自我介绍 ➔ 环节二：小林coding 10大独立模块八股文 ➔ 环节三：真实项目上下文深度深挖连环追问）；\n");
            sb.append("• **考官引擎**：微软晓晓 (Xiaoxiao Neural TTS) 真实级语音朗读 + 数字人口型毫秒级音画协同；\n");
            sb.append("• **考场场次**：#").append(sId).append("，第一题考题已生成完毕。\n\n");
            sb.append("点击下方考场卡片，即可立即入场进入沉浸式考场开考：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("智能体自动开辟面试考场失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isCoursePurchaseIntent(String lower) {
        if (lower.contains("退款") || lower.contains("退课") || lower.contains("发票") || lower.contains("开票") || lower.contains("密码")) {
            return false;
        }
        return lower.contains("买课") || lower.contains("买课程") || lower.contains("购课")
                || lower.contains("我要买") || lower.contains("想买") || lower.contains("购买")
                || lower.contains("加购") || lower.contains("购物车") || lower.contains("报名")
                || (lower.contains("课程") && (lower.contains("买") || lower.contains("购") || lower.contains("学") || lower.contains("要") || lower.contains("帮我")));
    }

    private String extractCourseKeyword(String content) {
        if (!StringUtils.hasText(content)) return "";
        String lower = content.toLowerCase();
        if (lower.contains("微服务") || lower.contains("springcloud") || lower.contains("cloud")) return "微服务";
        if (lower.contains("springboot") || lower.contains("boot")) return "SpringBoot";
        if (lower.contains("node") || lower.contains("nodejs")) return "Node";
        if (lower.contains("vue") || lower.contains("前端") || lower.contains("react")) return "前端";
        if (lower.contains("python") || lower.contains("爬虫") || lower.contains("机器学习")) return "Python";
        if (lower.contains("go") || lower.contains("golang")) return "Go";
        if (lower.contains("docker") || lower.contains("k8s") || lower.contains("云原生")) return "云原生";
        if (lower.contains("mysql") || lower.contains("数据库") || lower.contains("sql")) return "MySQL";
        if (lower.contains("redis") || lower.contains("缓存")) return "Redis";
        if (lower.contains("大模型") || lower.contains("ai") || lower.contains("llm")) return "大模型";
        if (lower.contains("算法") || lower.contains("数据结构")) return "算法";
        if (lower.contains("java")) return "Java";
        return "";
    }

    @SuppressWarnings("unchecked")
    private String handleCoursePurchaseAction(String content) {
        String keyword = extractCourseKeyword(content);
        try {
            AjaxResult res = remoteEducationService.searchCourses(keyword, 5);
            if (res != null && res.isSuccess() && res.get("data") != null) {
                Map<String, Object> dataMap = (Map<String, Object>) res.get("data");
                Object listObj = dataMap.get("list");
                if (listObj instanceof List) {
                    List<?> list = (List<?>) listObj;
                    if (!list.isEmpty() && list.get(0) instanceof Map) {
                        Map<String, Object> course = (Map<String, Object>) list.get(0);
                        String courseId = String.valueOf(course.get("id"));
                        String title = course.get("title") != null ? course.get("title").toString() : String.valueOf(course.get("courseName"));
                        String cover = course.get("cover") != null ? course.get("cover").toString() : "/src/assets/images/courses/default-cover.svg";
                        Number priceNum = course.get("price") instanceof Number ? (Number) course.get("price") : 0;
                        Number originalPriceNum = course.get("originalPrice") instanceof Number ? (Number) course.get("originalPrice") : priceNum;
                        String teacherName = course.get("teacherName") != null ? course.get("teacherName").toString() : "资深讲师团队";
                        Object lessons = course.getOrDefault("lessons", 0);
                        Object isFree = course.getOrDefault("isFree", 0);
                        String desc = course.get("shortDescription") != null ? course.get("shortDescription").toString() : "";

                        Map<String, Object> card = new HashMap<>();
                        card.put("action", "course_purchase");
                        card.put("courseId", courseId);
                        card.put("title", title);
                        card.put("cover", cover);
                        card.put("price", priceNum);
                        card.put("originalPrice", originalPriceNum);
                        card.put("teacherName", teacherName);
                        card.put("lessons", lessons);
                        card.put("isFree", isFree);
                        String cardJson = objectMapper.writeValueAsString(card);

                        BigDecimal priceYuan = new BigDecimal(priceNum.toString()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                        StringBuilder sb = new StringBuilder();
                        sb.append("🛒 **已为您精准匹配目标课程！**\n\n");
                        sb.append("已为您调取最受学员好评的精品好课《").append(title).append("》：\n");
                        if (StringUtils.hasText(desc)) {
                            sb.append("• **课程介绍**：").append(desc).append("\n");
                        }
                        sb.append("• **主讲名师**：").append(teacherName).append(" ｜ **总课时**：").append(lessons).append(" 讲\n");
                        sb.append("• **课程价格**：");
                        if ("1".equals(String.valueOf(isFree)) || priceYuan.compareTo(BigDecimal.ZERO) == 0) {
                            sb.append("【限时免费】\n");
                        } else {
                            sb.append("¥").append(priceYuan).append(" 元\n");
                        }
                        sb.append("\n我已为您生成专属购课操作卡片，您可以直接点击【加入购物车】或【立即结算】一键发起购买：\n\n");
                        sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
                        return sb.toString();
                    }
                }
            }
        } catch (Exception ex) {
            log.error("智能体搜索并生成购课卡片异常: {}", ex.getMessage(), ex);
        }
        return null;
    }

    private boolean isSignInActionIntent(String lower) {
        return lower.contains("签到") || lower.contains("打卡") || lower.contains("今日打卡")
                || lower.contains("领积分") || lower.contains("每日签到");
    }

    private String handleSignInAction() {
        try {
            Map<String, Object> card = new HashMap<>();
            card.put("action", "sign_in");
            card.put("dailyPoints", 10);
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("✨ **每日学情打卡与积分中心**\n\n");
            sb.append("坚持每日签到打卡，每天可固定获得 **+10 学分** 奖励，连续打卡更有惊喜加成！积分可在购买课程时直接抵扣现金或兑换专属大额优惠券。\n\n");
            sb.append("点击下方卡片中的【一键打卡签到】即可立即完成打卡：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成签到卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isResumeActionIntent(String lower) {
        return lower.contains("诊断简历") || lower.contains("我的简历") || lower.contains("分析简历")
                || lower.contains("看看我的简历") || lower.contains("简历诊断") || lower.contains("简历报告");
    }

    private String handleResumeAction() {
        try {
            ResumeAnalysisVO resume = userResumeService.getMyResume();
            Map<String, Object> card = new HashMap<>();
            StringBuilder sb = new StringBuilder();

            if (resume != null && resume.getId() != null) {
                card.put("action", "resume_diagnose");
                card.put("resumeId", String.valueOf(resume.getId()));
                card.put("fileName", resume.getFileName());
                card.put("matchScore", resume.getMatchScore() != null ? resume.getMatchScore() : 80);
                card.put("targetJob", resume.getTargetJob() != null ? resume.getTargetJob() : "技术开发工程师");
                String cardJson = objectMapper.writeValueAsString(card);

                sb.append("📄 **AI 简历深度诊断与能力雷达**\n\n");
                sb.append("已为您调取当前个人中心关联的简历档案《").append(resume.getFileName() != null ? resume.getFileName() : "我的简历").append("》：\n");
                sb.append("• **对标岗位**：").append(resume.getTargetJob() != null ? resume.getTargetJob() : "技术开发工程师").append("\n");
                sb.append("• **综合契合度评分**：").append(resume.getMatchScore() != null ? resume.getMatchScore() : 80).append(" 分\n");
                if (resume.getProjectHighlights() != null && !resume.getProjectHighlights().isEmpty()) {
                    sb.append("• **核心高光亮点**：").append(String.join("、", resume.getProjectHighlights())).append("\n");
                }
                sb.append("\n点击下方卡片即可查看完整能力六维雷达诊断，或直接针对薄弱项发起模拟面试连环追问：\n\n");
                sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            } else {
                card.put("action", "resume_upload");
                String cardJson = objectMapper.writeValueAsString(card);

                sb.append("📄 **AI 简历深度诊断与职涯对标**\n\n");
                sb.append("检测到您当前尚未在系统中上传求职简历。上传简历后，AI 将基于真实项目与技术标签进行深度特征抽取、契合度量化与高并发/分布式实战考题连环追问。\n\n");
                sb.append("点击下方卡片即可前往个人中心一键上传简历：\n\n");
                sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            }
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成简历诊断卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isLearningActionIntent(String lower) {
        return lower.contains("学到哪了") || lower.contains("继续学习") || lower.contains("我的进度")
                || lower.contains("课表") || lower.contains("我的课表") || lower.contains("继续上课");
    }

    private String handleLearningAction() {
        try {
            Map<String, Object> card = new HashMap<>();
            card.put("action", "continue_learning");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("📚 **学伴学习进度接力**\n\n");
            sb.append("已为您同步个人课表与学习进度。保持持续学习与代码实践是快速蜕变为架构师的核心捷径！\n\n");
            sb.append("点击下方操作卡片即可直达我的课表或探索更多精品好课：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成学习进度卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isInterviewReportIntent(String lower) {
        if (lower.contains("开") || lower.contains("开始") || lower.contains("来一场") || lower.contains("我要面试") || lower.contains("开一场")) {
            return false;
        }
        return lower.contains("面试报告") || lower.contains("面试成绩") || lower.contains("面试复盘")
                || lower.contains("上次面试") || lower.contains("面试记录") || lower.contains("面试得了多少分")
                || lower.contains("查面试") || lower.contains("我的面试") || lower.contains("面试结果");
    }

    private String handleInterviewReportAction() {
        try {
            Long userId = currentUserId();
            if (userId == null) {
                return "请先登录后再查看您的模拟面试记录与复盘报告。";
            }
            IPage<InterviewSession> mySessions = interviewService.listMySessions(1, 5);
            List<InterviewSession> records = mySessions != null ? mySessions.getRecords() : Collections.emptyList();

            if (records.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("📊 **AI 模拟面试档案查询**\n\n");
                sb.append("• **身份安全校验**：已严格按当前登录用户（ID: ").append(userId).append("）进行数据隔离。\n");
                sb.append("检测到您当前暂无任何模拟面试考核记录。点击下方即可立即开启您的首场全真模拟考核：\n\n");
                Map<String, Object> card = new HashMap<>();
                card.put("action", "interview_launch");
                card.put("targetJob", "Java高级开发工程师");
                card.put("company", "阿里巴巴");
                sb.append("[AGENT_ACTION_CARD:").append(objectMapper.writeValueAsString(card)).append("]");
                return sb.toString();
            }

            InterviewSession latest = records.get(0);
            Map<String, Object> card = new HashMap<>();
            card.put("action", "interview_report");
            card.put("sessionId", String.valueOf(latest.getId()));
            card.put("targetJob", latest.getTargetJob() != null ? latest.getTargetJob() : "技术开发工程师");
            card.put("company", latest.getCompanyTarget() != null ? latest.getCompanyTarget() : "大厂通用");
            card.put("status", latest.getStatus());
            card.put("score", latest.getScore() != null ? latest.getScore() : 82);
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("📊 **已为您调取属于您本人的最新模拟面试复盘档案！**\n\n");
            sb.append("• **身份安全隔离**：已严格锁定当前认证用户（ID: ").append(userId).append("），杜绝越权访问他人面试隐私\n");
            sb.append("• **考场场次**：#").append(latest.getId()).append("\n");
            sb.append("• **考核方向**：【").append(latest.getCompanyTarget()).append("】").append(latest.getTargetJob()).append("\n");
            sb.append("• **考核状态**：").append(latest.getStatus() == 2 ? "✅ 考核已终局完成" : (latest.getStatus() == 1 ? "⏳ 考场进行中" : "已结束")).append("\n");
            if (latest.getScore() != null) {
                sb.append("• **综合得分**：").append(latest.getScore()).append(" 分\n");
            }
            sb.append("\n点击下方卡片即可直接进入全真大屏复盘与六维能力雷达：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("查询面试复盘卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isOrderManageIntent(String lower) {
        return lower.contains("订单") || lower.contains("我的订单") || lower.contains("查订单")
                || lower.contains("买了什么") || lower.contains("买的课") || lower.contains("待付款")
                || lower.contains("退款") || lower.contains("退课") || lower.contains("取消订单");
    }

    private String handleOrderManageAction(String content) {
        try {
            Long userId = currentUserId();
            Map<String, Object> card = new HashMap<>();
            card.put("action", "order_manage");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("📦 **个人订单与交易管理中枢**\n\n");
            sb.append("• **防越权安全保障**：系统已严格绑定当前登录用户（ID: ").append(userId).append("），所有订单数据与售后操作均受行级权限保护，无法操作他人资产。\n");
            sb.append("• **操作支持**：一键查看我的全部订单列表、处理待支付订单极速结账、以及课程售后退款申请。\n\n");
            sb.append("点击下方卡片即可一键直达您的专属订单管理大厅：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成订单管理卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isCouponCenterIntent(String lower) {
        return lower.contains("优惠券") || lower.contains("领券") || lower.contains("卡券")
                || lower.contains("减免") || lower.contains("打折券") || lower.contains("兑换码");
    }

    private String handleCouponCenterAction() {
        try {
            Long userId = currentUserId();
            Map<String, Object> card = new HashMap<>();
            card.put("action", "coupon_center");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("🎟️ **优惠券与专属权益中心**\n\n");
            sb.append("• **权益归属**：系统已匹配当前认证账户（ID: ").append(userId).append("）可领取的平台热门好券，领取后直接绑定您个人卡券包，购课结算时自动按最优策略抵扣现金。\n\n");
            sb.append("点击下方卡片即可一键直达领券中心或查看我的卡券：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成优惠券卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isCartViewIntent(String lower) {
        return lower.contains("购物车") || lower.contains("车里有") || lower.contains("查看购物车") || lower.contains("清空购物车");
    }

    private String handleCartViewAction() {
        try {
            Long userId = currentUserId();
            Map<String, Object> card = new HashMap<>();
            card.put("action", "cart_view");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("🛒 **我的购物车资产清单**\n\n");
            sb.append("• **数据隔离**：购物车清单严格与您当前的登录凭证（用户 ID: ").append(userId).append("）绑定，仅您本人有权查看与结算。\n\n");
            sb.append("点击下方卡片即可直达购物车结算或挑选课程：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成购物车卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isExamQueryIntent(String lower) {
        return lower.contains("考试") || lower.contains("测验") || lower.contains("查分")
                || lower.contains("考试成绩") || lower.contains("考试记录") || lower.contains("错题") || lower.contains("答卷");
    }

    private String handleExamQueryAction() {
        try {
            Long userId = currentUserId();
            Map<String, Object> card = new HashMap<>();
            card.put("action", "exam_query");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("📝 **学情考试与测验考核中心**\n\n");
            sb.append("• **专属成绩档案**：考试记录与错题本严格绑定当前学员（ID: ").append(userId).append("），真实记录期末测试、随堂小测与技术实训成绩。\n\n");
            sb.append("点击下方卡片即可查看您的考试记录、成绩单与错题解析：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成考试中心卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isNoteQuickIntent(String lower) {
        return lower.contains("记笔记") || lower.contains("做笔记") || lower.contains("我的笔记")
                || lower.contains("记一条笔记") || lower.contains("帮我记笔记") || lower.contains("写笔记") || lower.contains("查看笔记");
    }

    private String handleNoteQuickAction(String content) {
        try {
            Long userId = currentUserId();
            String noteText = "";
            if (content.contains("：")) {
                noteText = content.substring(content.indexOf("：") + 1).trim();
            } else if (content.contains(":")) {
                noteText = content.substring(content.indexOf(":") + 1).trim();
            }

            Map<String, Object> card = new HashMap<>();
            card.put("action", "note_quick");
            card.put("noteContent", noteText);
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("📒 **随堂速记与个人技术知识库**\n\n");
            sb.append("• **私密知识空间**：笔记内容已开启个人强隔离（归属用户 ID: ").append(userId).append("），沉淀专属技术心得。\n");
            if (StringUtils.hasText(noteText)) {
                sb.append("• **识别笔记内容**：").append(noteText).append("\n");
            }
            sb.append("\n点击下方卡片即可快速将要点存入您的笔记库，或查看历史笔记：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成笔记卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isPointsRankingIntent(String lower) {
        return lower.contains("学霸") || lower.contains("天梯") || lower.contains("排行榜")
                || lower.contains("学霸榜") || lower.contains("天梯榜") || lower.contains("积分榜")
                || lower.contains("积分排行") || lower.contains("积分明细") || lower.contains("我的积分")
                || lower.contains("多少积分") || lower.contains("积分中心") || lower.contains("积分");
    }

    private String handlePointsRankingAction() {
        try {
            Long userId = currentUserId();
            Map<String, Object> card = new HashMap<>();
            card.put("action", "points_ranking");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("🏆 **赛季学霸天梯榜与积分资产**\n\n");
            sb.append("• **用户资产核验**：当前登录用户（ID: ").append(userId).append("）的学分资产已就绪。\n");
            sb.append("• **排行榜规则**：每赛季根据日常答题、签到与考场表现更新学霸天梯段位。\n\n");
            sb.append("点击下方卡片即可查看赛季天梯排名或我的积分明细：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成积分榜卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isLearningPortraitIntent(String lower) {
        return lower.contains("学习画像") || lower.contains("技能图谱") || lower.contains("我的画像")
                || lower.contains("能力雷达") || lower.contains("技能雷达") || lower.contains("能力画像");
    }

    private String handleLearningPortraitAction() {
        try {
            Long userId = currentUserId();
            Map<String, Object> card = new HashMap<>();
            card.put("action", "learning_portrait");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("📊 **学员多维动态学习画像与能力雷达**\n\n");
            sb.append("• **动态构建**：基于您当前账户（ID: ").append(userId).append("）在平台完成的课程进度、模拟面试打分与随堂测试表现实时推演。\n\n");
            sb.append("点击下方卡片即可直达个人中心查看学员能力六维雷达与技能图谱：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成画像卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isPageNavigatorIntent(String lower) {
        return lower.contains("设置") || lower.contains("修改密码") || lower.contains("个人中心")
                || lower.contains("去首页") || lower.contains("回首页") || lower.contains("问答社区")
                || lower.contains("提问") || lower.contains("带我去") || lower.contains("打开页面");
    }

    private String handlePageNavigatorAction(String lower) {
        String targetRoute = "/personal/main/overview";
        String title = "个人中心";

        if (lower.contains("设置") || lower.contains("密码")) {
            targetRoute = "/personal/main/mySet";
            title = "个人设置";
        } else if (lower.contains("首页") || lower.contains("主页")) {
            targetRoute = "/main/index";
            title = "平台首页";
        } else if (lower.contains("问答") || lower.contains("提问")) {
            targetRoute = "/ask/index";
            title = "问答社区";
        } else if (lower.contains("搜索") || lower.contains("找课")) {
            targetRoute = "/search/index";
            title = "课程搜索";
        }

        try {
            Map<String, Object> card = new HashMap<>();
            card.put("action", "page_navigator");
            card.put("targetRoute", targetRoute);
            card.put("pageTitle", title);
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("🧭 **智能页面穿梭通道**\n\n");
            sb.append("已为您定位目标页面【").append(title).append("】。\n\n");
            sb.append("点击下方卡片即可立即直达：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成页面穿梭卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private record LocalAnswer(int score, String answer, Long id, boolean faq) {
    }
}
