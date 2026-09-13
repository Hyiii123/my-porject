package com.share.education.ai.algorithm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.education.ai.config.AiRecommendProperties;
import com.share.education.ai.model.AlgorithmCandidateDTO;
import com.share.education.ai.model.UserProfileContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 独立 Python 推荐算法模型服务适配器 (RemotePythonAlgorithmEngine)。
 *
 * <p>【对接用户自研实验算法】：
 * 1. 本类为算法引擎的核心实现（标注 @Primary）；
 * 2. 负责向用户的独立 Python 接口服务（如 FastAPI / Flask / PyTorch / TensorFlow 模型服务）发送 HTTP 预测请求；
 * 3. 具备自动双模降级：当 Python 服务暂未启动或处于离线训练中时，自动毫秒级降级为本地 DefaultHybridAlgorithmEngine；
 * 4. 用户后续启动 Python 服务后，系统自动即时接入真实训练模型的预测得分与候选召回！</p>
 */
@Primary
@Component
public class RemotePythonAlgorithmEngine implements IRecommendAlgorithmEngine {

    private static final Logger log = LoggerFactory.getLogger(RemotePythonAlgorithmEngine.class);

    private final AiRecommendProperties properties;
    private final DefaultHybridAlgorithmEngine fallbackEngine;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public RemotePythonAlgorithmEngine(AiRecommendProperties properties,
                                       DefaultHybridAlgorithmEngine fallbackEngine,
                                       ObjectMapper objectMapper) {
        this.properties = properties;
        this.fallbackEngine = fallbackEngine;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = properties.getPythonService() != null ? properties.getPythonService().getTimeoutMs() : 5000;
        factory.setConnectTimeout(Math.min(timeout, 3000));
        factory.setReadTimeout(Math.max(timeout, 5000));
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public List<AlgorithmCandidateDTO> recallCandidates(Long userId, UserProfileContext profile, int topK) {
        AiRecommendProperties.PythonServiceProperties pyProps = properties != null ? properties.getPythonService() : null;
        String serviceUrl = pyProps != null ? pyProps.getUrl() : "http://tianji-recommend:5000/api/recommend/predict";
        boolean serviceEnabled = pyProps == null || pyProps.isEnabled();

        log.info("[AlgorithmEngine] recallCandidates: userId={}, enabled={}, targetUrl={}", userId, serviceEnabled, serviceUrl);

        // 1. 检查是否开启远程 Python 服务
        if (serviceEnabled && StringUtils.hasText(serviceUrl)) {
            try {
                Map<String, Object> requestPayload = new LinkedHashMap<>();
                requestPayload.put("userId", userId != null ? userId : 0L);
                requestPayload.put("historyCourseIds", (profile != null && profile.getChronologicalCourseIds() != null)
                    ? profile.getChronologicalCourseIds() : Collections.emptyList());
                requestPayload.put("intendedRole", profile != null ? profile.getIntendedRole() : "");
                requestPayload.put("preferredDifficulty", profile != null ? profile.getPreferredDifficulty() : 2);
                requestPayload.put("topSkills", (profile != null && profile.getTopSkills() != null)
                    ? profile.getTopSkills() : Collections.emptyList());
                requestPayload.put("skillWeights", (profile != null && profile.getSkillWeights() != null)
                    ? profile.getSkillWeights() : Collections.emptyMap());
                requestPayload.put("skillGaps", (profile != null && profile.getSkillGaps() != null)
                    ? profile.getSkillGaps() : Collections.emptyList());
                requestPayload.put("enrolledCourseIds", (profile != null && profile.getEnrolledCourseIds() != null)
                    ? profile.getEnrolledCourseIds() : Collections.emptySet());
                requestPayload.put("topK", topK);
                requestPayload.put("useFrontier", true);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestPayload, headers);

                ResponseEntity<String> response = restTemplate.postForEntity(serviceUrl, entity, String.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    List<AlgorithmCandidateDTO> parsed = parsePythonResponse(response.getBody());
                    if (parsed != null && !parsed.isEmpty()) {
                        log.info("成功从独立 Python 推荐模型服务召回 {} 门候选课程 (DRAG-KP4SR 知识增强)", parsed.size());
                        return parsed;
                    }
                }
            } catch (Exception ex) {
                log.warn("Python 推荐模型服务调用异常 ({})，自动使用本地混合基线引擎兜底: {}",
                    serviceUrl, ex.getMessage());
            }
        }

        // 2. 平滑降级至本地混合特征基线引擎
        return fallbackEngine.recallCandidates(userId, profile, topK);
    }

    @Override
    public String getEngineName() {
        return "RemotePythonModelEngine(DRAG-KP4SR-Hybrid-Fallback)";
    }

    private List<AlgorithmCandidateDTO> parsePythonResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode dataNode = root.has("data") ? root.get("data") : root;
            if (dataNode.isArray()) {
                List<AlgorithmCandidateDTO> list = new ArrayList<>();
                for (JsonNode item : dataNode) {
                    Long courseId = item.has("courseId") ? item.get("courseId").asLong() : null;
                    Double score = item.has("score") ? item.get("score").asDouble() : 85.0;
                    String matchTag = item.has("matchTag") ? item.get("matchTag").asText() : "自研模型精准召回";

                    Map<String, Object> featureMap = new HashMap<>();
                    List<String> evidencePaths = new ArrayList<>();
                    if (item.has("features")) {
                        featureMap = objectMapper.convertValue(item.get("features"), new TypeReference<Map<String, Object>>() {});
                        if (item.get("features").has("evidencePaths")) {
                            evidencePaths = objectMapper.convertValue(item.get("features").get("evidencePaths"), new TypeReference<List<String>>() {});
                        }
                    }

                    if (courseId != null && courseId > 0) {
                        list.add(AlgorithmCandidateDTO.builder()
                            .courseId(courseId)
                            .score(score)
                            .matchTag(matchTag)
                            .evidencePaths(evidencePaths)
                            .featureMap(featureMap)
                            .build());
                    }
                }
                return list;
            }
        } catch (Exception ex) {
            log.warn("解析 Python 推荐模型响应失败: {}", ex.getMessage());
        }
        return Collections.emptyList();
    }
}
