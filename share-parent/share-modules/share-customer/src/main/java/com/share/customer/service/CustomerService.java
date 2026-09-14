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
import com.share.education.api.RemoteEducationService;

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

    public CustomerService(CustomerKnowledgeMapper knowledgeMapper, CustomerFaqMapper faqMapper,
            CustomerSessionMapper sessionMapper, CustomerMessageMapper messageMapper,
            CustomerEvaluationMapper evaluationMapper, CustomerAiConfigMapper aiConfigMapper,
            CustomerAiCallLogMapper aiCallLogMapper, CustomerAiClient aiClient,
            CustomerAiProperties aiProperties, RedisService redisService, ObjectMapper objectMapper,
            RestTemplate restTemplate, RemoteEducationService remoteEducationService) {
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
        Long userId = currentUserId();
        if (userId == null) {
            return new Page<>();
        }
        Page<CustomerSession> page = new Page<>(safePage(pageNum), safeSize(pageSize));
        LambdaQueryWrapper<CustomerSession> wrapper = new LambdaQueryWrapper<CustomerSession>()
                .eq(CustomerSession::getUserId, userId)
                .orderByDesc(CustomerSession::getUpdatedAt);
        return sessionMapper.selectPage(page, wrapper);
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

        // 1. 优先进行多智能体协同导学意图识别与跨微服务推演分发
        if (isAgentDeliberationIntent(cleanContent)) {
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

    private record LocalAnswer(int score, String answer, Long id, boolean faq) {
    }
}
