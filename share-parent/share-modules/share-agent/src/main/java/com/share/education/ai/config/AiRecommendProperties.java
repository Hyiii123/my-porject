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

    /** 第三方大模型 API Key (从配置中心、环境变量或安全缓存动态加载，严禁明文硬编码) */
    private String apiKey;

    /** 模型名称 (始终默认采用 gpt-5.6-luna) */
    private String model = "gpt-5.6-luna";

    /** 模型请求基地址 (支持中转站端点与官方兼容端点) */
    private String baseUrl = "https://ai-pixel.online";

    /** 创造性采样参数 (0.0 ~ 1.0) */
    private double temperature = 0.7;

    /** 最大重试次数 */
    private int maxRetries = 2;

    /** LLM 超时时间 (毫秒，适配第三方 API 网络往返耗时) */
    private int timeoutMs = 30000;

    /** 缓存有效时长 (分钟) */
    private int cacheTtlMinutes = 15;

    /** 独立 Python 推荐算法模型服务配置 */
    private PythonServiceProperties pythonService = new PythonServiceProperties();

    public String getApiKey() {
        if (StringUtils.hasText(apiKey) && !"sk-tianji-spring-ai-token".equals(apiKey.trim())) {
            return apiKey.trim();
        }
        String env = System.getenv("AI_API_KEY");
        if (StringUtils.hasText(env) && !"sk-tianji-spring-ai-token".equals(env.trim())) {
            return env.trim();
        }
        env = System.getenv("TJ_AI_SECRET");
        if (StringUtils.hasText(env) && !"sk-tianji-spring-ai-token".equals(env.trim())) {
            return env.trim();
        }
        return System.getProperty("ai.recommend.api-key");
    }

    public String getBaseUrl() {
        String env = System.getenv("AI_BASE_URL");
        if (StringUtils.hasText(env)) return env.trim();
        env = System.getenv("TJ_PIXEL_API_BASE_URL");
        if (StringUtils.hasText(env)) {
            String b = env.trim();
            return b.endsWith("/v1") ? b : (b.endsWith("/") ? b + "v1" : b + "/v1");
        }
        return baseUrl;
    }

    public String getModel() {
        String env = System.getenv("AI_MODEL");
        if (StringUtils.hasText(env)) return env.trim();
        env = System.getenv("TJ_PIXEL_API_MODEL");
        if (StringUtils.hasText(env)) return env.trim();
        return model;
    }

    @Data
    public static class PythonServiceProperties {
        /** 是否启用远程 Python 推荐模型服务 */
        private boolean enabled = true;

        /** Python 模型服务预测接口 URL (容器互联默认为 http://zhiwen-recommend:5000/api/recommend/predict) */
        private String url = "http://zhiwen-recommend:5000/api/recommend/predict";

        /** 调用超时时间 (毫秒) */
        private int timeoutMs = 5000;

        public String getUrl() {
            String env = System.getenv("PYTHON_REC_SERVICE_URL");
            if (StringUtils.hasText(env)) return env;
            return url;
        }
    }
}
