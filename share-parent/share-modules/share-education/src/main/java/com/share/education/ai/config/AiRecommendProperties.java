package com.share.education.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AI 智能推荐与多智能体系统配置属性。
 * 支持第三方 API Key、聚合端点与独立 Python 推荐模型服务。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.recommend")
public class AiRecommendProperties {

    /** 是否启用 AI 智能推荐系统 */
    private boolean enabled = true;

    /** 第三方大模型 API Key (如 DashScope、OpenAI 或聚合中转 Key) */
    private String apiKey = "sk-bbca5271c9ed04fa86449bf5e18e236cdd42830e47a1b32591ffba7aff538ec9";

    /** 模型名称 (支持 gpt-5.4-mini, gpt-5.5, qwen-plus 等) */
    private String model = "gpt-5.4-mini";

    /** 模型请求基地址 (支持中转站端点与官方兼容端点) */
    private String baseUrl = "https://ai-pixel.online/v1";

    /** 创造性采样参数 (0.0 ~ 1.0) */
    private double temperature = 0.7;

    /** 最大重试次数 */
    private int maxRetries = 2;

    /** LLM 超时时间 (毫秒) */
    private int timeoutMs = 5000;

    /** 缓存有效时长 (分钟) */
    private int cacheTtlMinutes = 15;

    /** 独立 Python 推荐算法模型服务配置 */
    private PythonServiceProperties pythonService = new PythonServiceProperties();

    public String getApiKey() {
        if (StringUtils.hasText(apiKey)) {
            return apiKey;
        }
        String env = System.getenv("DASHSCOPE_API_KEY");
        if (StringUtils.hasText(env)) return env;
        env = System.getenv("AI_API_KEY");
        if (StringUtils.hasText(env)) return env;
        env = System.getenv("OPENAI_API_KEY");
        if (StringUtils.hasText(env)) return env;
        return System.getProperty("ai.recommend.api-key");
    }

    public String getBaseUrl() {
        String env = System.getenv("AI_BASE_URL");
        if (StringUtils.hasText(env)) return env;
        env = System.getenv("DASHSCOPE_BASE_URL");
        if (StringUtils.hasText(env)) return env;
        return baseUrl;
    }

    public String getModel() {
        String env = System.getenv("AI_MODEL");
        if (StringUtils.hasText(env)) return env;
        return model;
    }

    @Data
    public static class PythonServiceProperties {
        /** 是否启用远程 Python 推荐模型服务 */
        private boolean enabled = true;

        /** Python 模型服务预测接口 URL */
        private String url = "http://127.0.0.1:5000/api/recommend/predict";

        /** 调用超时时间 (毫秒) */
        private int timeoutMs = 2500;

        public String getUrl() {
            String env = System.getenv("PYTHON_REC_SERVICE_URL");
            if (StringUtils.hasText(env)) return env;
            return url;
        }
    }
}
