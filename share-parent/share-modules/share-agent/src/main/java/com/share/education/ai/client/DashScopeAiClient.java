package com.share.education.ai.client;

import com.share.common.redis.service.RedisService;
import com.share.education.ai.config.AiRecommendProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 历史类名兼容包装类。所有底层能力已全面收敛至 {@link ThirdPartyAiClient}。
 * 平台统一接入第三方 Pixel API (gpt-5.6-luna)，禁止直连外部其他官方大模型。
 *
 * @deprecated 请直接使用 {@link ThirdPartyAiClient}
 */
@Deprecated
@Component
public class DashScopeAiClient extends ThirdPartyAiClient {

    @Autowired
    public DashScopeAiClient(AiRecommendProperties properties,
                             ChatClient chatClient,
                             ChatModel chatModel,
                             @Autowired(required = false) RedisService redisService) {
        super(properties, chatClient, chatModel, redisService);
    }
}
