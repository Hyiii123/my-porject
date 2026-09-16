package com.share.customer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.share.customer.domain.CustomerEvaluation;
import com.share.customer.domain.CustomerFaq;
import com.share.customer.domain.CustomerKnowledge;
import com.share.customer.domain.CustomerMessage;
import com.share.customer.domain.CustomerSession;
import com.share.customer.domain.dto.*;
import com.share.customer.domain.vo.CustomerAiConfigView;
import com.share.customer.domain.vo.CustomerChatResult;
import com.share.customer.domain.vo.CustomerStatisticsVO;

import java.util.List;

/**
 * 客服与智能体核心业务接口
 */
public interface ICustomerService {

    CustomerSession createSession(CreateSessionRequest request);

    IPage<CustomerSession> listMySessions(long pageNum, long pageSize);

    IPage<CustomerSession> listMySessions(long pageNum, long pageSize, Integer status);

    void deleteMySession(Long sessionId);

    CustomerSession archiveMySession(Long sessionId, boolean archive);

    CustomerSession getMySession(Long sessionId);

    List<CustomerMessage> listMyMessages(Long sessionId);

    CustomerChatResult sendMessage(Long sessionId, String content);

    CustomerChatResult sendMessage(Long sessionId, String content, String requestApiKey, String requestModel);

    CustomerEvaluation saveEvaluation(Long sessionId, EvaluationRequest request);

    List<CustomerFaq> listPublicFaq(int limit);

    List<CustomerMessage> listMessages(Long sessionId);

    IPage<CustomerKnowledge> listKnowledge(String keyword, String category, Integer status, long pageNum, long pageSize);

    CustomerKnowledge getKnowledge(Long id);

    CustomerKnowledge saveKnowledge(CustomerKnowledgeRequest request);

    void removeKnowledge(List<Long> ids);

    IPage<CustomerFaq> listFaq(String keyword, String category, Integer status, long pageNum, long pageSize);

    CustomerFaq getFaq(Long id);

    CustomerFaq saveFaq(CustomerFaqRequest request);

    void removeFaq(List<Long> ids);

    IPage<CustomerSession> listAllSessions(String keyword, Integer status, long pageNum, long pageSize);

    CustomerSession getAdminSession(Long sessionId);

    List<CustomerMessage> listAdminMessages(Long sessionId);

    CustomerChatResult recordExternalReply(Long sessionId, AiReplyRecordRequest request);

    CustomerSession closeSession(Long sessionId);

    CustomerStatisticsVO statistics();

    CustomerAiConfigView getAiConfigView();

    CustomerAiConfigView saveAiConfig(AiConfigRequest request);

    String testAi(String message);
}
