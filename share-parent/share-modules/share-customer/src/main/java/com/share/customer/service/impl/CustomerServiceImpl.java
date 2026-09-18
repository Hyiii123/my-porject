package com.share.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.crypto.AesCryptoUtil;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.redis.service.RedisService;
import com.share.common.security.utils.SecurityUtils;
import com.share.customer.config.CustomerAiProperties;
import com.share.customer.config.CustomerSessionProperties;
import com.share.customer.domain.*;
import com.share.customer.domain.dto.*;
import com.share.customer.domain.vo.CustomerAiConfigView;
import com.share.customer.domain.vo.CustomerChatResult;
import com.share.customer.domain.vo.CustomerStatisticsVO;
import com.share.customer.mapper.*;
import com.share.customer.service.AiReply;
import com.share.customer.service.CustomerAiClient;
import com.share.customer.service.ICustomerService;
import com.share.customer.service.support.CustomerActionCardAssembler;
import com.share.customer.service.support.CustomerSecurityShield;
import com.share.education.api.RemoteEducationService;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.fastjson2.JSON;
import com.share.customer.mq.CustomerRocketMQConstants;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 客服与智能体核心业务实现类
 */
@Slf4j
@Service
@Primary
public class CustomerServiceImpl implements ICustomerService {

    private static final long CONFIG_ID = 1L;
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
    private final CustomerSessionProperties sessionProperties;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final RemoteEducationService remoteEducationService;
    private final CustomerSecurityShield securityShield;
    private final CustomerActionCardAssembler cardAssembler;

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    public CustomerServiceImpl(
            CustomerKnowledgeMapper knowledgeMapper,
            CustomerFaqMapper faqMapper,
            CustomerSessionMapper sessionMapper,
            CustomerMessageMapper messageMapper,
            CustomerEvaluationMapper evaluationMapper,
            CustomerAiConfigMapper aiConfigMapper,
            CustomerAiCallLogMapper aiCallLogMapper,
            CustomerAiClient aiClient,
            CustomerAiProperties aiProperties,
            CustomerSessionProperties sessionProperties,
            RedisService redisService,
            ObjectMapper objectMapper,
            RestTemplate restTemplate,
            RemoteEducationService remoteEducationService,
            CustomerSecurityShield securityShield,
            CustomerActionCardAssembler cardAssembler) {
        this.knowledgeMapper = knowledgeMapper;
        this.faqMapper = faqMapper;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.evaluationMapper = evaluationMapper;
        this.aiConfigMapper = aiConfigMapper;
        this.aiCallLogMapper = aiCallLogMapper;
        this.aiClient = aiClient;
        this.aiProperties = aiProperties;
        this.sessionProperties = sessionProperties != null ? sessionProperties : new CustomerSessionProperties();
        this.redisService = redisService;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.remoteEducationService = remoteEducationService;
        this.securityShield = securityShield;
        this.cardAssembler = cardAssembler;
    }

    @Override
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

    @Override
    public IPage<CustomerSession> listMySessions(long pageNum, long pageSize) {
        return listMySessions(pageNum, pageSize, null);
    }

    @Override
    public IPage<CustomerSession> listMySessions(long pageNum, long pageSize, Integer status) {
        Long userId = currentUserId();
        if (userId == null) {
            return new Page<>();
        }
        // 若查询活跃会话（status == null 或 status != 4），且开启自动清理开关，主动排查并清理当前用户超过指定天数无对话的活跃会话，保持列表实时干净
        boolean autoClean = sessionProperties == null || sessionProperties.isAutoCleanEnabled();
        if ((status == null || status != 4) && autoClean) {
            try {
                int expireDays = sessionProperties != null ? sessionProperties.getExpireDays() : 2;
                cleanExpiredActiveSessionsForUser(userId, expireDays);
            } catch (Exception e) {
                log.warn("【客服会话】为用户 {} 动态排查过期活跃会话异常: {}", userId, e.getMessage());
            }
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

    @Override
    @Transactional
    public void deleteMySession(Long sessionId) {
        CustomerSession session = getSession(sessionId);
        assertOwner(session);
        sessionMapper.deleteById(sessionId);
    }

    @Override
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

    @Override
    public CustomerSession getMySession(Long sessionId) {
        CustomerSession session = getSession(sessionId);
        assertOwner(session);
        enrichSession(session);
        return session;
    }

    @Override
    public List<CustomerMessage> listMyMessages(Long sessionId) {
        CustomerSession session = getMySession(sessionId);
        return listMessages(session.getId());
    }

    @Override
    public CustomerChatResult sendMessage(Long sessionId, String content) {
        return sendMessage(sessionId, content, null, null);
    }

    @Override
    public CustomerChatResult sendMessage(Long sessionId, String content, String requestApiKey, String requestModel) {
        CustomerSession session = getMySession(sessionId);
        if (SESSION_CLOSED == session.getStatus()) {
            throw new ServiceException("会话已结束，请新建会话后继续咨询");
        }
        String cleanContent = content == null ? "" : content.trim();
        if (!StringUtils.hasText(cleanContent)) {
            throw new ServiceException("消息内容不能为空");
        }
        securityShield.enforceAskRateLimit(currentUserId());

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

        // 1. 优先尝试智能体自主操作意图识别与执行
        String actionReply = cardAssembler.tryDispatchAgentAction(cleanContent, currentUserId(), currentUserName());
        if (StringUtils.hasText(actionReply)) {
            answer = actionReply;
            isAgentDeliberation = true;
            aiModelUsed = "agent-copilot-action";
        }

        // 2. 尝试多智能体协同导学意图识别与跨微服务推演分发
        if (!isAgentDeliberation && cardAssembler.isAgentDeliberationIntent(cleanContent)) {
            String targetRole = cardAssembler.extractTargetRole(cleanContent);
            Integer difficulty = cardAssembler.extractTargetDifficulty(cleanContent);
            try {
                AjaxResult agentRes = remoteEducationService.orchestrateAgentRecommend(currentUserId(), targetRole, 4, difficulty, SecurityConstants.INNER);
                if (agentRes != null && agentRes.isSuccess() && agentRes.get("data") != null) {
                    answer = cardAssembler.formatAgentDeliberationReply(targetRole, agentRes.get("data"));
                    if (StringUtils.hasText(answer)) {
                        isAgentDeliberation = true;
                        aiModelUsed = "multi-agent-cluster";
                    }
                }
            } catch (Exception ex) {
                log.warn("调度教育多智能体推演异常，自动降级至通用大模型/FAQ: {}", ex.getMessage());
            }
        }

        // 3. 若未触发多智能体规划，则执行标准 AI 客服 / FAQ 知识库答疑
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

        session.setMessages(listMessages(session.getId()));
        return new CustomerChatResult(session, reply);
    }

    private CustomerAiConfig requestConfig(CustomerAiConfig base, String requestApiKey, String requestModel) {
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

        if (StringUtils.hasText(requestApiKey)) {
            value.setEnabled(1);
            if (StringUtils.hasText(requestModel)) {
                value.setModel(requestModel.trim());
            }
        }
        return value;
    }

    @Override
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
        session.setStatus(SESSION_CLOSED);
        session.setClosedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        return evaluation;
    }

    @Override
    public List<CustomerFaq> listPublicFaq(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        String cacheKey = CustomerSecurityShield.FAQ_CACHE_PREFIX + safeLimit;
        try {
            List<CustomerFaq> cached = redisService.getCacheObject(cacheKey);
            if (cached != null) {
                return cached;
            }
        } catch (RuntimeException ignored) {
        }
        List<CustomerFaq> result = faqMapper.selectList(new LambdaQueryWrapper<CustomerFaq>()
                .eq(CustomerFaq::getEnabled, 1)
                .orderByAsc(CustomerFaq::getSortNum)
                .last("limit " + safeLimit));
        try {
            redisService.setCacheObject(cacheKey, result, 5L, TimeUnit.MINUTES);
        } catch (RuntimeException ignored) {
        }
        return result;
    }

    @Override
    public List<CustomerMessage> listMessages(Long sessionId) {
        return messageMapper.selectList(new LambdaQueryWrapper<CustomerMessage>()
                .eq(CustomerMessage::getSessionId, sessionId)
                .orderByAsc(CustomerMessage::getCreateTime)
                .orderByAsc(CustomerMessage::getId));
    }

    @Override
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

    @Override
    public CustomerKnowledge getKnowledge(Long id) {
        CustomerKnowledge value = knowledgeMapper.selectById(id);
        if (value == null) {
            throw new ServiceException("知识库条目不存在");
        }
        return value;
    }

    @Override
    @Transactional
    public CustomerKnowledge saveKnowledge(CustomerKnowledgeRequest request) {
        CustomerKnowledge value = request == null || request.getId() == null
                ? new CustomerKnowledge() : getKnowledge(request.getId());
        LocalDateTime now = LocalDateTime.now();
        value.setQuestion(request.getQuestion() != null ? request.getQuestion().trim() : "");
        value.setAnswer(request.getAnswer() != null ? request.getAnswer().trim() : "");
        value.setKeywords(request.getKeywords());
        value.setCategory(request.getCategory() != null ? request.getCategory().trim() : "通用");
        value.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        value.setUpdateBy(currentUserId());
        value.setUpdateTime(now);
        boolean isNew = value.getId() == null;
        if (isNew) {
            value.setId(newId());
            value.setCreateBy(currentUserId());
            value.setCreateTime(now);
            value.setDelFlag(0);
            value.setVersion(0);
            value.setHitCount(0);
            knowledgeMapper.insert(value);
            sendKnowledgeSyncMessage("INSERT", value.getId(), value.getQuestion(), value.getCategory());
        } else {
            knowledgeMapper.updateById(value);
            sendKnowledgeSyncMessage("UPDATE", value.getId(), value.getQuestion(), value.getCategory());
        }
        return value;
    }

    @Override
    @Transactional
    public void removeKnowledge(List<Long> ids) {
        if (ids != null) {
            ids.stream().filter(Objects::nonNull).forEach(id -> {
                knowledgeMapper.deleteById(id);
                sendKnowledgeSyncMessage("DELETE", id, null, null);
            });
        }
    }

    private void sendKnowledgeSyncMessage(String action, Long knowledgeId, String question, String category) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQTemplate not available, skipping knowledge sync message for knowledgeId {}", knowledgeId);
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("action", action);
            payload.put("knowledgeId", knowledgeId);
            payload.put("question", question);
            payload.put("category", category);
            payload.put("timestamp", System.currentTimeMillis());

            rocketMQTemplate.syncSend(
                CustomerRocketMQConstants.CUSTOMER_KNOWLEDGE_SYNC_TOPIC,
                MessageBuilder.withPayload(JSON.toJSONString(payload)).build(),
                3000
            );
            log.info("【RocketMQ】成功发送知识库异步同步消息, action={}, knowledgeId={}", action, knowledgeId);
        } catch (Exception e) {
            log.error("【RocketMQ】发送知识库异步同步消息失败, action={}, knowledgeId={}", action, knowledgeId, e);
        }
    }

    @Override
    public IPage<CustomerFaq> listFaq(String keyword, String category, Integer status, long pageNum, long pageSize) {
        Page<CustomerFaq> page = new Page<>(safePage(pageNum), safeSize(pageSize));
        LambdaQueryWrapper<CustomerFaq> wrapper = new LambdaQueryWrapper<CustomerFaq>()
                .like(StringUtils.hasText(keyword), CustomerFaq::getQuestion, keyword)
                .eq(StringUtils.hasText(category), CustomerFaq::getCategory, category)
                .eq(status != null, CustomerFaq::getEnabled, status)
                .orderByAsc(CustomerFaq::getSortNum);
        return faqMapper.selectPage(page, wrapper);
    }

    @Override
    public CustomerFaq getFaq(Long id) {
        CustomerFaq value = faqMapper.selectById(id);
        if (value == null) {
            throw new ServiceException("常见问题不存在");
        }
        return value;
    }

    @Override
    @Transactional
    public CustomerFaq saveFaq(CustomerFaqRequest request) {
        CustomerFaq value = request == null || request.getId() == null ? new CustomerFaq() : getFaq(request.getId());
        LocalDateTime now = LocalDateTime.now();
        value.setQuestion(request.getQuestion() != null ? request.getQuestion().trim() : "");
        value.setAnswer(request.getAnswer() != null ? request.getAnswer().trim() : "");
        value.setCategory(request.getCategory() != null ? request.getCategory().trim() : "通用");
        value.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
        value.setSortNum(request.getSortNum() == null ? 0 : request.getSortNum());
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
        securityShield.evictFaqCache();
        return value;
    }

    @Override
    @Transactional
    public void removeFaq(List<Long> ids) {
        if (ids != null) {
            ids.stream().filter(Objects::nonNull).forEach(faqMapper::deleteById);
        }
        securityShield.evictFaqCache();
    }

    @Override
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

    @Override
    public CustomerSession getAdminSession(Long sessionId) {
        CustomerSession session = getSession(sessionId);
        enrichSession(session);
        return session;
    }

    @Override
    public List<CustomerMessage> listAdminMessages(Long sessionId) {
        getAdminSession(sessionId);
        return listMessages(sessionId);
    }

    @Override
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
        securityShield.enforceAskRateLimit(currentUserId());
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

    @Override
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

    @Override
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

    @Override
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
        view.setApiKeyConfigured(StringUtils.hasText(securityShield.resolveSecret()));
        return view;
    }

    @Override
    @Transactional
    public CustomerAiConfigView saveAiConfig(AiConfigRequest request) {
        securityShield.validatePixelAddress(request.getBaseUrl(), request.getEndpointPath());
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
        if (StringUtils.hasText(request.getApiKey())) {
            String cleanKey = request.getApiKey().trim();
            config.setApiKeyCiphertext(AesCryptoUtil.encrypt(cleanKey));
            redisService.setCacheObject(CustomerSecurityShield.SECRET_KEY, cleanKey);
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

    @Override
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
        String semanticAnswer = findSemanticAnswer(input);
        if (StringUtils.hasText(semanticAnswer)) {
            return semanticAnswer;
        }

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

            ResponseEntity<String> response = restTemplate.postForEntity(
                    EMBEDDING_SERVICE_URL, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode hits = root.path("hits");
                if (hits.isArray() && !hits.isEmpty()) {
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
        }
        return null;
    }

    private String formatLastMessagePreview(String answer) {
        if (!StringUtils.hasText(answer)) {
            return "";
        }
        String trimmed = answer.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 497) + "..." : trimmed;
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
        value.setEnabled(StringUtils.hasText(securityShield.resolveSecret()) ? 1 : 0);
        value.setTimeoutMs(aiProperties.getTimeoutMs());
        value.setMaxRetries(aiProperties.getMaxRetries());
        value.setSystemPrompt(aiProperties.getSystemPrompt());
        return value;
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

    @Override
    @Transactional
    public int cleanExpiredActiveSessions(int expireDays) {
        int defaultDays = sessionProperties != null && sessionProperties.getExpireDays() > 0
                ? sessionProperties.getExpireDays() : 2;
        int days = expireDays > 0 ? expireDays : defaultDays;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        List<Long> expiredIds = sessionMapper.selectExpiredActiveSessionIds(cutoff, null);
        if (expiredIds == null || expiredIds.isEmpty()) {
            log.debug("【客服活跃会话治理】无超过 {} 天无对话的活跃会话需要清理", days);
            return 0;
        }
        int totalDeleted = 0;
        int batchSize = 200;
        for (int i = 0; i < expiredIds.size(); i += batchSize) {
            List<Long> subList = expiredIds.subList(i, Math.min(i + batchSize, expiredIds.size()));
            sessionMapper.deleteBatchIds(subList);
            totalDeleted += subList.size();
        }
        log.info("【客服活跃会话治理】成功自动清理 {} 个超过 {} 天无对话的活跃会话: {}", totalDeleted, days, expiredIds);
        return totalDeleted;
    }

    @Override
    @Transactional
    public int cleanExpiredActiveSessionsForUser(Long userId, int expireDays) {
        if (userId == null) {
            return 0;
        }
        int defaultDays = sessionProperties != null && sessionProperties.getExpireDays() > 0
                ? sessionProperties.getExpireDays() : 2;
        int days = expireDays > 0 ? expireDays : defaultDays;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        List<Long> expiredIds = sessionMapper.selectExpiredActiveSessionIds(cutoff, userId);
        if (expiredIds == null || expiredIds.isEmpty()) {
            return 0;
        }
        int totalDeleted = 0;
        int batchSize = 200;
        for (int i = 0; i < expiredIds.size(); i += batchSize) {
            List<Long> subList = expiredIds.subList(i, Math.min(i + batchSize, expiredIds.size()));
            sessionMapper.deleteBatchIds(subList);
            totalDeleted += subList.size();
        }
        log.info("【客服活跃会话治理】已自动清理学员 (userId: {}) 的 {} 个超过 {} 天无对话的活跃会话: {}",
                userId, totalDeleted, days, expiredIds);
        return totalDeleted;
    }

    private record LocalAnswer(int score, String answer, Long id, boolean faq) {}
}
