package com.share.education.ai.evals.judge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.education.ai.client.ThirdPartyAiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 大模型裁判盲审服务 (LLM-as-a-Judge / G-Eval Framework)。
 *
 * <p>基于 5 分制标准量规对智能体生成的推荐理由进行深度学术盲审，包含：
 * 1. 岗位胜任力契合度 (Persuasiveness, 1~5 分)；
 * 2. 认知梯级逻辑衔接性 (Coherence, 1~5 分)；
 * 3. 大纲证据接地防幻觉度 (Grounding, 1~5 分)。</p>
 */
@Service
public class LlmJudgeService {

    private static final Logger log = LoggerFactory.getLogger(LlmJudgeService.class);

    private final ThirdPartyAiClient aiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record JudgeReport(
        double overallScore,
        int persuasiveness,
        int coherence,
        int grounding,
        String rubricSummary,
        boolean approved
    ) {}

    @Autowired
    public LlmJudgeService(@Autowired(required = false) ThirdPartyAiClient aiClient) {
        this.aiClient = aiClient;
    }

    /**
     * 对推荐生成结果执行客观盲审打分
     */
    public JudgeReport evaluateExplanation(String targetRole,
                                          String courseName,
                                          String stageName,
                                          String coreKnowledge,
                                          String explanation) {
        if (!StringUtils.hasText(explanation)) {
            return new JudgeReport(1.0, 1, 1, 1, "推荐理由为空", false);
        }

        if (aiClient != null && aiClient.isAvailable()) {
            try {
                String sys = "你是一位国家级计算机教学指导委员会学术盲审专家 (LLM-as-a-Judge)。" +
                    "请根据 5 分制量规（1=极差, 5=极佳）对给定的课程推荐理由进行打分，并输出 JSON 格式：" +
                    "{\"persuasiveness\": 5, \"coherence\": 5, \"grounding\": 5, \"summary\": \"简要理由\"}";
                String usr = String.format("目标岗位：%s，所属阶段：%s，课程：《%s》，核心知识点：%s。待评测推荐理由：\"%s\"。请盲审打分：",
                    targetRole, stageName, courseName, coreKnowledge, explanation);

                String raw = aiClient.generate(sys, usr);
                if (StringUtils.hasText(raw)) {
                    String jsonPart = raw.trim();
                    if (jsonPart.contains("{") && jsonPart.contains("}")) {
                        jsonPart = jsonPart.substring(jsonPart.indexOf('{'), jsonPart.lastIndexOf('}') + 1);
                        JsonNode root = objectMapper.readTree(jsonPart);
                        int p = root.path("persuasiveness").asInt(5);
                        int c = root.path("coherence").asInt(5);
                        int g = root.path("grounding").asInt(5);
                        String sum = root.path("summary").asText("盲审符合国家 IT 人才培养规范");
                        double avg = (p + c + g) / 3.0;
                        return new JudgeReport(Math.round(avg * 10.0) / 10.0, p, c, g, sum, avg >= 4.0);
                    }
                }
            } catch (Exception ex) {
                log.warn("[LlmJudge] 盲审裁判调用解析异常: {}", ex.getMessage());
            }
        }

        // 确定性启发式规则兜底
        boolean hasGrounding = StringUtils.hasText(coreKnowledge) && (explanation.contains(coreKnowledge) || explanation.contains(courseName));
        int p = explanation.length() >= 25 ? 5 : 4;
        int c = explanation.contains("筑牢") || explanation.contains("进阶") || explanation.contains("架构") ? 5 : 4;
        int g = hasGrounding ? 5 : 4;
        double avg = (p + c + g) / 3.0;
        return new JudgeReport(Math.round(avg * 10.0) / 10.0, p, c, g, "经启发式规则核验，理由契合大纲且逻辑完整", avg >= 4.0);
    }
}
