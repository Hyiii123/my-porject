package com.share.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.share.customer.service.support.ResumeScoringEngine;
import com.share.customer.service.support.ResumeTextExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 候选人简历档案与诊断业务实现类
 */
@Slf4j
@Service
public class UserResumeServiceImpl implements IUserResumeService {

    private static final long CONFIG_ID = 1L;

    private final UserResumeMapper resumeMapper;
    private final CustomerAiConfigMapper aiConfigMapper;
    private final CustomerAiClient aiClient;
    private final CustomerAiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final ResumeTextExtractor textExtractor;
    private final ResumeScoringEngine scoringEngine;

    public UserResumeServiceImpl(
            UserResumeMapper resumeMapper,
            CustomerAiConfigMapper aiConfigMapper,
            CustomerAiClient aiClient,
            CustomerAiProperties aiProperties,
            ObjectMapper objectMapper,
            ResumeTextExtractor textExtractor,
            ResumeScoringEngine scoringEngine) {
        this.resumeMapper = resumeMapper;
        this.aiConfigMapper = aiConfigMapper;
        this.aiClient = aiClient;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.textExtractor = textExtractor;
        this.scoringEngine = scoringEngine;
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
        return scoringEngine.toVO(resume);
    }

    @Override
    @Transactional
    public ResumeAnalysisVO saveResume(ResumeSaveRequest request) {
        Long userId = currentUserId();
        String userName = currentUserName();
        LocalDateTime now = LocalDateTime.now();

        if (textExtractor.isRawBinary(request.getRawContent())) {
            throw new ServiceException("简历内容包含无法识别的二进制格式（如原始 PDF 字节码），请上传有效文档或直接粘贴文本");
        }

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

        // BUG-26: 保存简历时立即萃取技术标签与启发式初评，确保面试时无需手动分析即可感知技能标签与分数
        List<String> tags = scoringEngine.extractTechTags(request.getRawContent());
        if (!tags.isEmpty()) {
            try {
                resume.setTechTags(objectMapper.writeValueAsString(tags));
            } catch (Exception ignore) {}
        }
        if (resume.getMatchScore() == null || resume.getMatchScore() == 0) {
            ResumeAnalysisRequest analysisReq = new ResumeAnalysisRequest();
            analysisReq.setResumeContent(request.getRawContent());
            analysisReq.setTargetJob(resume.getTargetJob());
            analysisReq.setCompanyTarget(resume.getTargetCompany());
            scoringEngine.applyHeuristicAnalysis(resume, analysisReq);
        }

        resume.setUpdateTime(now);

        if (resume.getId() == null) {
            resumeMapper.insert(resume);
        } else {
            resumeMapper.updateById(resume);
        }

        return scoringEngine.toVO(resume);
    }

    @Override
    @Transactional
    public ResumeAnalysisVO analyzeResume(ResumeAnalysisRequest request) {
        Long userId = currentUserId();
        String userName = currentUserName();
        LocalDateTime now = LocalDateTime.now();

        if (textExtractor.isRawBinary(request.getResumeContent())) {
            throw new ServiceException("简历内容包含无法识别的二进制格式（如原始 PDF 字节码），请上传有效文档或直接粘贴文本");
        }

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

        // 构造严谨专业的大模型诊断 Prompt
        String prompt = buildPrompt(request);
        String aiResult = callAi(prompt);

        boolean parsed = false;
        if (StringUtils.hasText(aiResult)) {
            try {
                String cleanJson = extractJson(aiResult);
                JsonNode root = objectMapper.readTree(cleanJson);
                int score = root.path("matchScore").asInt(80);
                String level = root.path("matchLevel").asText("大厂标准中高级工程师");
                JsonNode tagsNode = root.path("techTags");
                JsonNode highlightsNode = root.path("projectHighlights");
                JsonNode gapsNode = root.path("resumeGaps");
                JsonNode questionsNode = root.path("predictedQuestions");
                String starAdvice = root.path("starAdvice").asText("");

                resume.setMatchScore(Math.max(60, Math.min(score, 100)));
                resume.setMatchLevel(level);
                resume.setTechTags(tagsNode.isMissingNode() ? "[]" : tagsNode.toString());
                resume.setProjectHighlights(highlightsNode.isMissingNode() ? "[]" : highlightsNode.toString());
                resume.setResumeGaps(gapsNode.isMissingNode() ? "[]" : gapsNode.toString());
                resume.setPredictedQuestions(questionsNode.isMissingNode() ? "[]" : questionsNode.toString());
                resume.setStarAdvice(starAdvice);
                parsed = true;

                // 真实性后置校验审计：如果候选人简历完全没有技术栈且没有工程项目，坚决杜绝大模型客套幻觉
                List<String> realTags = scoringEngine.extractTechTags(request.getResumeContent());
                List<String> realProjects = scoringEngine.extractProjects(request.getResumeContent());
                if (realTags.isEmpty() && realProjects.isEmpty()) {
                    // BUG-25: 零项目且零技术栈时给予真实的未达标分数（30分），拒绝虚假及格
                    resume.setMatchScore(30);
                    resume.setMatchLevel("严重未达标 (缺乏对口技术栈与项目经历)");
                    resume.setTechTags("[]");
                    resume.setProjectHighlights(objectMapper.writeValueAsString(List.of(
                            "【无对口技术亮点】：简历未检测到与【" + request.getTargetJob() + "】相关的对口工程项目或核心技术栈，实战积累严重不足",
                            "【工程实战缺失】：履历内容未体现该技术方向所需的系统研发、架构设计或攻坚成果"
                    )));
                    resume.setPredictedQuestions(objectMapper.writeValueAsString(List.of(
                            "【背景与动机核查】：简历中未出现与【" + request.getTargetJob() + "】对口的技术研发经历，请如实说明你跨界应聘该岗位的动机与真实水平？",
                            "【实际动手能力】：针对【" + request.getTargetJob() + "】专业领域，你目前是否有可供核验的独立代码工程、开源实践或个人作品？",
                            "【大厂门槛与差距】：面对【" + request.getCompanyTarget() + "】的技术招聘门槛，你打算如何系统性补齐缺乏真实业务项目的短板？"
                    )));
                    resume.setResumeGaps(objectMapper.writeValueAsString(List.of(
                            "【技术栈脱节】：简历未出现与目标岗位【" + request.getTargetJob() + "】相关的对口编程语言与技术栈，专业储备不足",
                            "【工程项目缺失】：简历中无明确命名的技术/系统研发项目经历，大厂技术初筛通过率极低",
                            "【缺乏量化成效】：缺少软件工程相关的量化性能指标或业务研发成果"
                    )));
                    resume.setStarAdvice("当前简历内容与技术岗位【" + request.getTargetJob() + "】严重脱节，无法套用现有经历进行技术面 STAR 重塑。强烈建议：首先围绕【" + request.getTargetJob() + "】补充一个对口的完整端到端技术项目，再按 S(业务场景/技术难点) ➔ T(架构攻坚目标) ➔ A(具体技术选型与实现细节) ➔ R(性能压测或实际量化成效) 的规范标准重新撰写履历。");
                }
            } catch (Exception ex) {
                log.warn("解析大模型简历分析 JSON 失败，启用智能启发式真实文本萃取: {}", ex.getMessage());
            }
        }

        if (!parsed) {
            scoringEngine.applyHeuristicAnalysis(resume, request);
        }

        resume.setUpdateTime(now);
        if (resume.getId() == null) {
            resumeMapper.insert(resume);
        } else {
            resumeMapper.updateById(resume);
        }

        return scoringEngine.toVO(resume);
    }

    private String buildPrompt(ResumeAnalysisRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位资深互联网大厂（阿里P7+/字节2-2技术专家）招聘委员会面试官兼高级技术导师。\n");
        sb.append("请对以下候选人简历针对目标岗位【").append(request.getTargetJob())
                .append("】及目标企业【").append(request.getCompanyTarget()).append("】进行深度技术对标与简历诊断评测。\n\n");
        sb.append("【候选人真实简历内容】：\n");
        sb.append(request.getResumeContent()).append("\n\n");
        sb.append("【极度重要核心要求（求真务实，严禁幻觉与杜撰）】：\n");
        sb.append("1. 绝不瞎编！简历中没有的项目或技术，必须明确说没有！严禁替候选人凭空发明项目、架构或技能！\n");
        sb.append("2. 【简历核心高光亮点挖掘 (projectHighlights)】：\n");
        sb.append("   - 若简历中包含真实且对口的技术项目与成果，紧扣简历原文客观提炼真实亮点；\n");
        sb.append("   - 若简历中完全没有符合【").append(request.getTargetJob()).append("】的技术项目或亮点，必须直接如实输出：\n");
        sb.append("     [\"【无对口技术亮点】：简历未检测到与【").append(request.getTargetJob()).append("】相关的对口工程项目或核心技术栈，实战积累严重不足\", \"【工程实战缺失】：履历内容未体现该技术方向所需的系统研发、架构设计或攻坚成果\"]\n");
        sb.append("     绝对不要给无关经历强行编造“架构良好”、“具备通用能力”等虚假客套话！\n");
        sb.append("3. 【大厂面试官预测深挖题 (predictedQuestions)】：\n");
        sb.append("   - 只有当简历中真实写有具体项目名和具体技术时，才紧扣该真实项目和技术出深挖题；\n");
        sb.append("   - 如果简历中完全没有相关技术项目（如非技术人员或不相干简历），绝不可瞎编不存在的项目或未提及的技术（严禁向没写过Redis的人问Redis）！必须直击其痛点提问其实际背景与真实水平：\n");
        sb.append("     [\"【经历真实性核查】：简历中未出现与【").append(request.getTargetJob()).append("】对口的技术研发经历，请如实说明你跨界应聘该岗位的动机与真实水平？\", \"【实际动手能力】：针对【").append(request.getTargetJob()).append("】专业领域，你目前是否有可供核验的独立代码工程、开源实践或个人作品？\", \"【大厂门槛与差距】：面对【").append(request.getCompanyTarget()).append("】的技术招聘门槛，你打算如何系统性补齐缺乏真实业务项目的短板？\"]\n");
        sb.append("4. 【技能标签 techTags】：必须且仅能从简历原文中精确抽取真实提及的技能名词。若完全没有，返回空数组 []，严禁编造！\n");
        sb.append("5. 【打分与淘汰红线（满分100分，准入门槛基准分固定为60分）】：\n");
        sb.append("   - 若简历与目标岗位完全不相干，五大实战加分必须全部为 0 分，matchScore 必须严格等于 60 分，matchLevel 为 '初阶成长型，亟待丰富项目与产出'！\n");
        sb.append("   - matchLevel 评级严格按总分映射：\n");
        sb.append("     92~100分：\"阿里P7+/字节2-2 资深技术专家\"\n");
        sb.append("     85~91分：\"阿里P6+/字节2-1 骨干级研发标杆\"\n");
        sb.append("     75~84分：\"大厂标准中高级工程师\"\n");
        sb.append("     65~74分：\"技术扎实准入级开发者\"\n");
        sb.append("     <65分：\"初阶成长型，亟待丰富项目与产出\"\n");
        sb.append("6. 请严格按以下 JSON 格式输出深度评测报告，不要输出任何额外的包裹代码块或多余文字：\n");
        sb.append("{\n");
        sb.append("  \"matchScore\": 86,\n");
        sb.append("  \"matchLevel\": \"阿里P6+/字节2-1 骨干级研发标杆\",\n");
        sb.append("  \"techTags\": [\"从简历中提取的真实技术1\", \"真实技术2\", \"真实技术3\"],\n");
        sb.append("  \"projectHighlights\": [\n");
        sb.append("    \"结合候选人简历真实项目名与具体架构实现的亮点1\",\n");
        sb.append("    \"结合候选人简历真实业务或量化成果的亮点2\"\n");
        sb.append("  ],\n");
        sb.append("  \"resumeGaps\": [\n");
        sb.append("    \"针对候选人简历真实写法的薄弱点1（如缺少量化数据）\",\n");
        sb.append("    \"针对候选人项目容灾或深度的薄弱点2\"\n");
        sb.append("  ],\n");
        sb.append("  \"predictedQuestions\": [\n");
        sb.append("    \"结合候选人真实项目【具体项目名】及所用技术的深挖题1\",\n");
        sb.append("    \"针对候选人真实技术架构的深度技术推演题2\",\n");
        sb.append("    \"针对候选人履历中性能调优或高可用场景的连环追问题3\"\n");
        sb.append("  ],\n");
        sb.append("  \"starAdvice\": \"紧扣候选人真实项目结合目标岗位的 STAR 法则修改建议\"\n");
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String extractResumeText(MultipartFile file) {
        return textExtractor.extractResumeText(file);
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
        if (id == null || id <= 0) {
            throw new ServiceException("请先登录后再进行操作");
        }
        return id;
    }

    private String currentUserName() {
        String name = SecurityUtils.getUsername();
        return StringUtils.hasText(name) ? name : "学员";
    }
}