package com.share.education.ai.config;

import com.share.education.ai.algorithm.IRecommendAlgorithmEngine;
import com.share.education.ai.model.AlgorithmCandidateDTO;
import com.share.education.ai.model.UserProfileContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Function;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;

/**
 * 官方 Spring AI 核心配置与工具 Bean 注册中心。
 *
 * <p>为系统注入标准 Spring AI 体系核心能力：
 * 1. {@link ChatModel}：OpenAI / 通义千问 DashScope 兼容大语言模型底座；
 * 2. {@link ChatClient}：Spring AI 官方推荐的流式 / 提示词驱动客户端；
 * 3. 注册 {@code dragRecommendTool} 为 Spring AI Function 回调，使自研 DRAG-KP4SR 算法具备标准 Tool Calling 能力。</p>
 */
@Configuration
@AutoConfigureBefore(OpenAiChatAutoConfiguration.class)
public class SpringAiConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SpringAiConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(OpenAiApi.class)
    public OpenAiApi openAiApi(AiRecommendProperties properties) {
        String baseUrl = properties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }
        String apiKey = properties.getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            apiKey = "dummy-key-for-spring-ai-init";
        }
        log.info("[Spring AI] Initialized official OpenAiApi bean with baseUrl: {}", baseUrl);
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel openAiChatModel(OpenAiApi openAiApi, AiRecommendProperties properties) {
        String model = properties.getModel();
        if (!StringUtils.hasText(model)) {
            model = "qwen-turbo";
        }

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(properties.getTemperature())
                .build();

        log.info("[Spring AI] Initialized official OpenAiChatModel bean with model: {}", model);
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(ChatClient.class)
    public ChatClient chatClient(ChatModel chatModel) {
        log.info("[Spring AI] Initialized official ChatClient bean based on ChatModel");
        return ChatClient.builder(chatModel).build();
    }

    /**
     * 将自研 DRAG-KP4SR 知识图谱序列推荐算法注册为 Spring AI 官方 Function / Tool
     */
    @Bean
    @Description("调用 DRAG-KP4SR 知识图谱序列推荐算法服务，基于学员时序学习轨迹与先修依赖拓扑召回候选课程列表")
    public Function<RecommendToolRequest, RecommendToolResponse> dragRecommendTool(IRecommendAlgorithmEngine algorithmEngine) {
        return request -> {
            log.info("[Spring AI Tool] Executing dragRecommendTool for userId: {}, historyCount: {}",
                    request.getUserId(), request.getHistoryCourseIds() != null ? request.getHistoryCourseIds().size() : 0);
            UserProfileContext profile = UserProfileContext.builder()
                    .userId(request.getUserId())
                    .chronologicalCourseIds(request.getHistoryCourseIds())
                    .topSkills(request.getTopSkills())
                    .intendedRole(request.getIntendedRole())
                    .build();

            List<AlgorithmCandidateDTO> candidates = algorithmEngine.recallCandidates(
                    request.getUserId(),
                    profile,
                    request.getLimit() != null ? request.getLimit() : 20
            );
            return new RecommendToolResponse(candidates);
        };
    }

    /**
     * Spring AI Tool 请求 DTO
     */
    public static class RecommendToolRequest {
        private Long userId;
        private List<Long> historyCourseIds;
        private List<String> topSkills;
        private String intendedRole;
        private Integer limit;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public List<Long> getHistoryCourseIds() { return historyCourseIds; }
        public void setHistoryCourseIds(List<Long> historyCourseIds) { this.historyCourseIds = historyCourseIds; }
        public List<String> getTopSkills() { return topSkills; }
        public void setTopSkills(List<String> topSkills) { this.topSkills = topSkills; }
        public String getIntendedRole() { return intendedRole; }
        public void setIntendedRole(String intendedRole) { this.intendedRole = intendedRole; }
        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
    }

    /**
     * Spring AI Tool 响应 DTO
     */
    public static class RecommendToolResponse {
        private List<AlgorithmCandidateDTO> candidates;

        public RecommendToolResponse() {}
        public RecommendToolResponse(List<AlgorithmCandidateDTO> candidates) {
            this.candidates = candidates;
        }

        public List<AlgorithmCandidateDTO> getCandidates() { return candidates; }
        public void setCandidates(List<AlgorithmCandidateDTO> candidates) { this.candidates = candidates; }
    }
}
