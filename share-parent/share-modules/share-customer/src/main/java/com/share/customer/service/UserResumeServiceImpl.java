package com.share.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.common.core.exception.ServiceException;
import com.share.common.security.utils.SecurityUtils;
import com.share.customer.config.CustomerAiProperties;
import com.share.customer.domain.CustomerAiConfig;
import com.share.customer.domain.interview.UserResume;
import com.share.customer.domain.interview.dto.ResumeAnalysisRequest;
import com.share.customer.domain.interview.dto.ResumeSaveRequest;
import com.share.customer.domain.interview.vo.ResumeAnalysisVO;
import com.share.customer.mapper.CustomerAiConfigMapper;
import com.share.customer.mapper.UserResumeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class UserResumeServiceImpl implements IUserResumeService {

    private static final long CONFIG_ID = 1L;

    private final UserResumeMapper resumeMapper;
    private final CustomerAiConfigMapper aiConfigMapper;
    private final CustomerAiClient aiClient;
    private final CustomerAiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public UserResumeServiceImpl(
            UserResumeMapper resumeMapper,
            CustomerAiConfigMapper aiConfigMapper,
            CustomerAiClient aiClient,
            CustomerAiProperties aiProperties,
            ObjectMapper objectMapper) {
        this.resumeMapper = resumeMapper;
        this.aiConfigMapper = aiConfigMapper;
        this.aiClient = aiClient;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResumeAnalysisVO getMyResume() {
        Long userId = currentUserId();
        UserResume resume = resumeMapper.selectOne(
                new LambdaQueryWrapper<UserResume>()
                        .eq(UserResume::getUserId, userId)
                        .orderByDesc(UserResume::getUpdateTime)
                        .last("LIMIT 1")
        );
        if (resume == null) {
            return null;
        }
        return toVO(resume);
    }

    @Override
    @Transactional
    public ResumeAnalysisVO saveResume(ResumeSaveRequest request) {
        Long userId = currentUserId();
        String userName = currentUserName();
        LocalDateTime now = LocalDateTime.now();

        UserResume resume = resumeMapper.selectOne(
                new LambdaQueryWrapper<UserResume>()
                        .eq(UserResume::getUserId, userId)
                        .orderByDesc(UserResume::getUpdateTime)
                        .last("LIMIT 1")
        );

        if (resume == null) {
            resume = new UserResume();
            resume.setUserId(userId);
            resume.setUserName(userName);
            resume.setCreateTime(now);
        }

        if (StringUtils.hasText(request.getFileName())) {
            resume.setFileName(request.getFileName());
        }
        resume.setRawContent(request.getRawContent());
        if (StringUtils.hasText(request.getTargetJob())) {
            resume.setTargetJob(request.getTargetJob());
        }
        if (StringUtils.hasText(request.getTargetCompany())) {
            resume.setTargetCompany(request.getTargetCompany());
        }
        resume.setUpdateTime(now);

        if (resume.getId() == null) {
            resumeMapper.insert(resume);
        } else {
            resumeMapper.updateById(resume);
        }

        return toVO(resume);
    }

    @Override
    @Transactional
    public ResumeAnalysisVO analyzeResume(ResumeAnalysisRequest request) {
        Long userId = currentUserId();
        String userName = currentUserName();
        LocalDateTime now = LocalDateTime.now();

        UserResume resume = resumeMapper.selectOne(
                new LambdaQueryWrapper<UserResume>()
                        .eq(UserResume::getUserId, userId)
                        .orderByDesc(UserResume::getUpdateTime)
                        .last("LIMIT 1")
        );

        if (resume == null) {
            resume = new UserResume();
            resume.setUserId(userId);
            resume.setUserName(userName);
            resume.setCreateTime(now);
        }

        resume.setTargetJob(request.getTargetJob());
        resume.setTargetCompany(request.getCompanyTarget());
        resume.setRawContent(request.getResumeContent());
        if (StringUtils.hasText(request.getFileName())) {
            resume.setFileName(request.getFileName());
        }

        // 构造专业级大模型简历分析 Prompt
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位资深互联网大厂（阿里P7+/字节2-2技术专家）招聘委员会面试官兼高级导师。\n");
        sb.append("请对以下候选人简历针对目标岗位【").append(request.getTargetJob())
                .append("】及目标企业【").append(request.getCompanyTarget()).append("】进行深度技术对标与简历诊断评测。\n\n");
        sb.append("【候选人简历内容】：\n");
        sb.append(request.getResumeContent()).append("\n\n");
        sb.append("请严格按以下 JSON 格式输出深度评测报告，不要输出任何额外的包裹代码块或多余文字：\n");
        sb.append("{\n");
        sb.append("  \"matchScore\": 88,\n");
        sb.append("  \"matchLevel\": \"阿里P6+/字节2-1 潜质\",\n");
        sb.append("  \"techTags\": [\"Spring Boot\", \"MySQL调优\", \"Redis集群\", \"Kafka\", \"Netty\", \"分布式架构\"],\n");
        sb.append("  \"projectHighlights\": [\n");
        sb.append("    \"主导千万级核心业务拆分与微服务落地，设计多级缓存抗住10万QPS峰值\",\n");
        sb.append("    \"针对深分页与慢SQL进行聚簇索引和覆盖索引优化，查询耗时由2.5s压降至40ms\"\n");
        sb.append("  ],\n");
        sb.append("  \"resumeGaps\": [\n");
        sb.append("    \"部分项目缺少明确业务量化收益（如并发提升百分比、故障率降幅）数据\",\n");
        sb.append("    \"对分布式事务异常场景（如网络分区、两阶段提交失败）的容灾兜底策略说明较简略\"\n");
        sb.append("  ],\n");
        sb.append("  \"predictedQuestions\": [\n");
        sb.append("    \"你在项目中用了分布式锁，请详细推演极端网络抖动导致锁超时释放时的并发安全解决方案？\",\n");
        sb.append("    \"针对简历中提到的分库分表，在分布式事务一致性与跨分片聚合分页上你们是如何权衡落地的？\",\n");
        sb.append("    \"在遭遇突发数倍峰值流量时，你们的Sentinel限流降级与Redis防击穿全链路设计是怎样的？\"\n");
        sb.append("  ],\n");
        sb.append("  \"starAdvice\": \"建议将项目职责描述由'负责XX系统开发'全面升级为STAR法则结构：S(业务痛点/QPS瓶颈) ➔ T(技术架构攻坚任务) ➔ A(采用的具体底层机制与重构方案) ➔ R(量化吞吐与可用性指标)，能大幅提升大厂初筛通过率。\"\n");
        sb.append("}");

        String prompt = sb.toString();
        String aiResult = callAi(prompt);

        boolean parsed = false;
        if (StringUtils.hasText(aiResult)) {
            try {
                String cleanJson = extractJson(aiResult);
                JsonNode root = objectMapper.readTree(cleanJson);
                int score = root.path("matchScore").asInt(85);
                String level = root.path("matchLevel").asText("大厂P6标准能力");
                JsonNode tagsNode = root.path("techTags");
                JsonNode highlightsNode = root.path("projectHighlights");
                JsonNode gapsNode = root.path("resumeGaps");
                JsonNode questionsNode = root.path("predictedQuestions");
                String starAdvice = root.path("starAdvice").asText("");

                resume.setMatchScore(Math.max(40, Math.min(score, 99)));
                resume.setMatchLevel(level);
                resume.setTechTags(tagsNode.isMissingNode() ? "[]" : tagsNode.toString());
                resume.setProjectHighlights(highlightsNode.isMissingNode() ? "[]" : highlightsNode.toString());
                resume.setResumeGaps(gapsNode.isMissingNode() ? "[]" : gapsNode.toString());
                resume.setPredictedQuestions(questionsNode.isMissingNode() ? "[]" : questionsNode.toString());
                resume.setStarAdvice(starAdvice);
                parsed = true;
            } catch (Exception ex) {
                log.warn("解析大模型简历分析 JSON 失败，启用智能启发式兜底: {}", ex.getMessage());
            }
        }

        if (!parsed) {
            applyHeuristicAnalysis(resume, request);
        }

        resume.setUpdateTime(now);
        if (resume.getId() == null) {
            resumeMapper.insert(resume);
        } else {
            resumeMapper.updateById(resume);
        }

        return toVO(resume);
    }

    @Override
    public String extractResumeText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件为空");
        }
        try {
            String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
            // 如果是文本、Markdown 或可以直接文本读取的文件
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            String content = sb.toString().trim();
            if (StringUtils.hasText(content)) {
                return content;
            }
            return "【已上传简历附件: " + originalFilename + "】\n请在下方编辑区补充或完善您的核心项目、技能点与履历细节。";
        } catch (Exception ex) {
            log.warn("提取简历文件文本失败: {}", ex.getMessage());
            throw new ServiceException("解析简历文件失败，请尝试复制文本粘贴录入");
        }
    }

    @Override
    @Transactional
    public void clearResume() {
        Long userId = currentUserId();
        resumeMapper.delete(new LambdaQueryWrapper<UserResume>().eq(UserResume::getUserId, userId));
    }

    @Override
    public UserResume getResumeEntity(Long resumeId, Long userId) {
        if (resumeId != null && resumeId > 0) {
            UserResume resume = resumeMapper.selectById(resumeId);
            if (resume != null && resume.getUserId().equals(userId)) {
                return resume;
            }
        }
        // 兜底查用户最新一份
        return resumeMapper.selectOne(
                new LambdaQueryWrapper<UserResume>()
                        .eq(UserResume::getUserId, userId)
                        .orderByDesc(UserResume::getUpdateTime)
                        .last("LIMIT 1")
        );
    }

    private void applyHeuristicAnalysis(UserResume resume, ResumeAnalysisRequest request) {
        String text = request.getResumeContent();
        List<String> detectedTags = new ArrayList<>();
        String[] candidates = {"Java", "Spring Boot", "Spring Cloud", "MySQL", "Redis", "Kafka", "RocketMQ",
                "MyBatis", "Netty", "JVM", "Docker", "Kubernetes", "Vue", "React", "Go", "Python", "Elasticsearch", "Qdrant", "Flink", "SRE"};
        for (String c : candidates) {
            if (Pattern.compile(Pattern.quote(c), Pattern.CASE_INSENSITIVE).matcher(text).find()) {
                detectedTags.add(c);
            }
        }
        if (detectedTags.isEmpty()) {
            detectedTags.addAll(List.of("核心基础", "业务架构", "工程实践"));
        }

        int score = 80 + Math.min(detectedTags.size() * 2, 12);
        String level = score >= 90 ? "阿里P7/字节2-2 潜力股" : (score >= 85 ? "阿里P6+/字节2-1 标杆" : "大厂准入级工程师");

        List<String> highlights = List.of(
                "技术栈涵盖 " + String.join("、", detectedTags.subList(0, Math.min(4, detectedTags.size()))) + "，具备良好的工程实战背景",
                "简历具备清晰的模块架构划分，有系统性业务演进和中间件调优痕迹"
        );

        List<String> gaps = List.of(
                "项目缺乏更具说服力的生产环境极限压测数据（如千万级PV、QPS提升比例）",
                "对底层源码级原理（如并发锁升级机制、内存模型屏障）的体现深度仍有提升空间"
        );

        List<String> questions = List.of(
                "结合你简历中用到的 " + detectedTags.get(0) + "，阐述线上遇到性能瓶颈或高并发雪崩时的全链路排查步骤？",
                "在分布式架构下，你如何处理数据最终一致性与跨服务调用的幂等性防重？",
                "如果系统并发从当前体量再翻 10 倍，你认为现有架构中最先崩溃的单点是什么，如何解决？"
        );

        String star = "建议紧扣 STAR 原则重构项目描述：明确背景痛点、核心技术攻坚手段与具体的指标结果量化，提升大厂技术评估命中率。";

        resume.setMatchScore(score);
        resume.setMatchLevel(level);
        try {
            resume.setTechTags(objectMapper.writeValueAsString(detectedTags));
            resume.setProjectHighlights(objectMapper.writeValueAsString(highlights));
            resume.setResumeGaps(objectMapper.writeValueAsString(gaps));
            resume.setPredictedQuestions(objectMapper.writeValueAsString(questions));
        } catch (Exception ignore) {}
        resume.setStarAdvice(star);
    }

    private ResumeAnalysisVO toVO(UserResume resume) {
        ResumeAnalysisVO vo = new ResumeAnalysisVO();
        vo.setId(resume.getId());
        vo.setFileName(resume.getFileName());
        vo.setRawContent(resume.getRawContent());
        vo.setTargetJob(resume.getTargetJob());
        vo.setTargetCompany(resume.getTargetCompany());
        vo.setMatchScore(resume.getMatchScore());
        vo.setMatchLevel(resume.getMatchLevel());
        vo.setStarAdvice(resume.getStarAdvice());
        vo.setUpdateTime(resume.getUpdateTime());

        vo.setTechTags(parseJsonList(resume.getTechTags()));
        vo.setProjectHighlights(parseJsonList(resume.getProjectHighlights()));
        vo.setResumeGaps(parseJsonList(resume.getResumeGaps()));
        vo.setPredictedQuestions(parseJsonList(resume.getPredictedQuestions()));
        return vo;
    }

    private List<String> parseJsonList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String callAi(String prompt) {
        try {
            CustomerAiConfig config = effectiveAiConfig();
            String reply = aiClient.askRaw(config, "你是一位顶尖互联网大厂技术架构专家兼资深招聘委员会面试官导师。", prompt);
            if (StringUtils.hasText(reply)) {
                return reply.trim();
            }
        } catch (Exception ex) {
            log.warn("AI 简历诊断调用异常: {}", ex.getMessage());
        }
        return null;
    }

    private CustomerAiConfig effectiveAiConfig() {
        CustomerAiConfig config = aiConfigMapper.selectById(CONFIG_ID);
        if (config != null) {
            return config;
        }
        CustomerAiConfig value = new CustomerAiConfig();
        value.setId(CONFIG_ID);
        value.setProvider("pixel");
        value.setBaseUrl(aiProperties.getBaseUrl());
        value.setEndpointPath(aiProperties.getEndpointPath());
        value.setModel(aiProperties.getModel());
        value.setEnabled(1);
        value.setTimeoutMs(aiProperties.getTimeoutMs());
        value.setMaxRetries(aiProperties.getMaxRetries());
        return value;
    }

    private String extractJson(String text) {
        if (!StringUtils.hasText(text)) {
            return "{}";
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text.trim();
    }

    private Long currentUserId() {
        Long id = SecurityUtils.getUserId();
        return id != null && id > 0 ? id : 1L;
    }

    private String currentUserName() {
        String name = SecurityUtils.getUsername();
        return StringUtils.hasText(name) ? name : "学员";
    }
}
