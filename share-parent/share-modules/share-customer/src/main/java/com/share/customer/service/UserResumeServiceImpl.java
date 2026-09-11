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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class UserResumeServiceImpl implements IUserResumeService {

    private static final long CONFIG_ID = 1L;

    private static final List<String> TECH_DICTIONARY = List.of(
            // 编程语言
            "Java", "Python", "Go", "Golang", "C++", "Rust", "Node.js", "TypeScript", "JavaScript",
            // 基础框架与微服务
            "Spring Boot", "Spring Cloud", "Spring Security", "MyBatis-Plus", "MyBatis", "Dubbo", "Netty", "gRPC", "GraalVM",
            // 中间件与消息队列
            "Redis", "Kafka", "RocketMQ", "RabbitMQ", "Elasticsearch", "Nacos", "Sentinel", "Canal", "Seata", "Zookeeper",
            // 数据库与存储
            "MySQL", "PostgreSQL", "Oracle", "MongoDB", "ClickHouse", "TiDB", "ShardingSphere",
            // 云原生与基础设施
            "Docker", "Kubernetes", "K8s", "Linux", "Nginx", "CI/CD", "Jenkins", "Prometheus", "Grafana", "SkyWalking",
            // 大数据与人工智能
            "Flink", "Spark", "Hadoop", "Hive", "PyTorch", "TensorFlow", "LangChain", "RAG", "Agent", "大模型", "LLM",
            // 前端
            "Vue3", "Vue", "React", "Element-Plus",
            // 架构能力与核心概念
            "分布式锁", "分库分表", "读写分离", "多级缓存", "消息幂等", "DDD", "领域驱动设计", "JVM调优", "SQL优化", "高可用架构"
    );

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

        if (isRawBinary(request.getRawContent())) {
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

        if (isRawBinary(request.getResumeContent())) {
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

        // 构造严谨专业、紧扣真实项目的大模型诊断 Prompt
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位资深互联网大厂（阿里P7+/字节2-2技术专家）招聘委员会面试官兼高级技术导师。\n");
        sb.append("请对以下候选人简历针对目标岗位【").append(request.getTargetJob())
                .append("】及目标企业【").append(request.getCompanyTarget()).append("】进行深度技术对标与简历诊断评测。\n\n");
        sb.append("【候选人真实简历内容】：\n");
        sb.append(request.getResumeContent()).append("\n\n");
        sb.append("【极度重要核心要求】：\n");
        sb.append("1. 严禁杜撰虚构！必须紧密围绕候选人简历中【真实提及的项目经历、技术栈、职责与成果】进行客观技术评价。\n");
        sb.append("2. 在 projectHighlights、resumeGaps 和 predictedQuestions 中，必须明确提及候选人简历中的具体真实项目名称或实际业务场景，严禁套用虚构通用模板！\n");
        sb.append("3. 技能标签 techTags 必须严格从简历中真实出现的技术、框架和工具中提取。\n");
        sb.append("4. 【严格打分规则（满分100分，基准及格分固定为60分）】：\n");
        sb.append("   - matchScore = 60分基准及格分 + 五大维度加分（范围 60 ~ 100 分）：\n");
        sb.append("     ① 核心技术栈广度与深度 (0~10分)\n");
        sb.append("     ② 项目经历与系统复杂度 (0~10分)\n");
        sb.append("     ③ STAR法则生产量化成效 (0~10分)\n");
        sb.append("     ④ 高可用工程规范与容灾防护 (0~5分)\n");
        sb.append("     ⑤ 目标岗位与名企契合度 (0~5分)\n");
        sb.append("   - matchLevel 评级严格按总分映射：\n");
        sb.append("     92~100分：\"阿里P7+/字节2-2 资深技术专家\"\n");
        sb.append("     85~91分：\"阿里P6+/字节2-1 骨干级研发标杆\"\n");
        sb.append("     75~84分：\"大厂标准中高级工程师\"\n");
        sb.append("     65~74分：\"技术扎实准入级开发者\"\n");
        sb.append("     <65分：\"初阶成长型，亟待丰富项目与产出\"\n");
        sb.append("5. 请严格按以下 JSON 格式输出深度评测报告，不要输出任何额外的包裹代码块或多余文字：\n");
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

        String prompt = sb.toString();
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
            } catch (Exception ex) {
                log.warn("解析大模型简历分析 JSON 失败，启用智能启发式真实文本萃取: {}", ex.getMessage());
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
            throw new ServiceException("上传简历文件为空，请选择有效文件");
        }
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "resume.pdf";
        String lowerName = originalFilename.toLowerCase();

        try {
            byte[] bytes = file.getBytes();
            if (bytes == null || bytes.length == 0) {
                throw new ServiceException("上传文件内容为空");
            }

            // 1. 判断是否为 PDF（根据后缀或魔数 %PDF）
            boolean isPdf = lowerName.endsWith(".pdf") || (bytes.length >= 4 && bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46);
            if (isPdf) {
                return extractTextFromPdf(bytes);
            }

            // 2. 判断是否为 DOCX（根据后缀或 ZIP 头 PK\x03\x04）
            boolean isZipOrDocx = lowerName.endsWith(".docx") || (bytes.length >= 4 && bytes[0] == 0x50 && bytes[1] == 0x4B && bytes[2] == 0x03 && bytes[3] == 0x04);
            if (isZipOrDocx) {
                try {
                    String docxText = extractTextFromDocx(bytes);
                    if (StringUtils.hasText(docxText)) {
                        return cleanExtractedText(docxText);
                    }
                } catch (Exception docxEx) {
                    log.warn("DOCX 文本提取失败: {}", docxEx.getMessage());
                }
            }

            // 3. 文本类（TXT, MD 等）
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (isRawBinary(text)) {
                throw new ServiceException("检测到上传文件为二进制格式，无法直接作为纯文本解析，请转换为标准 PDF/Word 文档或复制文本录入");
            }

            String cleaned = cleanExtractedText(text);
            if (StringUtils.hasText(cleaned)) {
                return cleaned;
            }

            return "【已解析简历文件: " + originalFilename + "】\n请在右侧编辑区补充或完善您的核心项目、技能点与履历细节。";
        } catch (ServiceException se) {
            throw se;
        } catch (Exception ex) {
            log.error("简历文件解析异常: {}", ex.getMessage(), ex);
            throw new ServiceException("解析简历文件失败: " + ex.getMessage() + "，建议直接复制简历文本粘贴");
        }
    }

    private String extractTextFromPdf(byte[] bytes) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(bytes))) {
            if (document.isEncrypted()) {
                throw new ServiceException("该 PDF 简历已被加密保护，请先解除密码后再上传");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            String cleaned = cleanExtractedText(text);
            if (!StringUtils.hasText(cleaned)) {
                throw new ServiceException("该 PDF 简历未包含可提取的文字层（可能为图片扫描件），请直接复制简历文字粘贴录入");
            }
            return cleaned;
        } catch (ServiceException se) {
            throw se;
        } catch (Exception ex) {
            log.error("PDFBox 提取 PDF 失败: {}", ex.getMessage(), ex);
            throw new ServiceException("PDF 文件解析失败，请检查文件是否损坏或直接粘贴文字");
        }
    }

    private String extractTextFromDocx(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equalsIgnoreCase(entry.getName())) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zis.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    String xml = baos.toString(StandardCharsets.UTF_8);
                    // 将段落标签 </w:p> 和换行符 <w:br/> 替换为真正的换行
                    xml = xml.replaceAll("</w:p>", "\n");
                    xml = xml.replaceAll("<w:br[^>]*/>", "\n");
                    xml = xml.replaceAll("<w:tab[^>]*/>", "\t");
                    // 剥离其余所有 XML 标签
                    xml = xml.replaceAll("<[^>]+>", "");
                    // 反转义常见实体
                    xml = xml.replace("&amp;", "&")
                             .replace("&lt;", "<")
                             .replace("&gt;", ">")
                             .replace("&quot;", "\"")
                             .replace("&apos;", "'");
                    sb.append(xml);
                    break;
                }
            }
        } catch (Exception ex) {
            log.warn("解析 Word document.xml 异常: {}", ex.getMessage());
        }
        return sb.toString();
    }

    private String cleanExtractedText(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        // 过滤 NULL 字节与不合法的控制字符
        String cleaned = raw.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        cleaned = cleaned.replace("\r\n", "\n").replace("\r", "\n");
        // 压缩过多的连续空行
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");
        cleaned = cleaned.trim();
        // 限制最大字数，避免超出数据库列容量
        if (cleaned.length() > 15000) {
            cleaned = cleaned.substring(0, 15000) + "\n\n【系统提示：已自动保留简历前 15,000 字核心履历】";
        }
        return cleaned;
    }

    private boolean isRawBinary(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        if (text.startsWith("%PDF-") || text.startsWith("PK\u0003\u0004")) {
            return true;
        }
        int unprintable = 0;
        int checkLen = Math.min(text.length(), 300);
        for (int i = 0; i < checkLen; i++) {
            char c = text.charAt(i);
            if (c != '\n' && c != '\r' && c != '\t' && c < 0x20) {
                unprintable++;
            }
        }
        return unprintable > 5;
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
        String text = request.getResumeContent() != null ? request.getResumeContent() : "";
        String targetJob = StringUtils.hasText(request.getTargetJob()) ? request.getTargetJob() : "后端高级开发工程师";
        String targetCompany = StringUtils.hasText(request.getCompanyTarget()) ? request.getCompanyTarget() : "一线互联网大厂";

        List<String> detectedTags = extractTechTags(text);
        if (detectedTags.isEmpty()) {
            detectedTags.addAll(List.of("Java", "Spring Boot", "MySQL", "Redis", "高并发架构"));
        }

        List<String> detectedProjects = extractProjects(text);
        List<String> detectedMetrics = extractMetrics(text);

        // 基础分严格设定为 60 分准入门槛，结合五大核心工程维度综合量化
        int baseScore = 60;
        int d1 = calcTechStackScore(detectedTags);
        int d2 = calcProjectScore(detectedProjects, text);
        int d3 = calcMetricsScore(detectedMetrics, text);
        int d4 = calcEngineeringScore(text);
        int d5 = calcFitScore(targetJob, targetCompany, detectedTags, text);

        int score = baseScore + d1 + d2 + d3 + d4 + d5;
        score = Math.max(60, Math.min(score, 100));

        String level;
        if (score >= 92) {
            level = "阿里P7+/字节2-2 资深技术专家";
        } else if (score >= 85) {
            level = "阿里P6+/字节2-1 骨干级研发标杆";
        } else if (score >= 75) {
            level = "大厂标准中高级工程师";
        } else if (score >= 65) {
            level = "技术扎实准入级开发者";
        } else {
            level = "初阶成长型，亟待丰富项目与产出";
        }

        // 1. 生成基于候选人真实项目和技能的高光亮点
        List<String> highlights = new ArrayList<>();
        String tagSummary = String.join("、", detectedTags.subList(0, Math.min(4, detectedTags.size())));
        if (!detectedProjects.isEmpty()) {
            String p1 = detectedProjects.get(0);
            highlights.add("深度参与/主导【" + p1 + "】核心模块研发，技术体系涵盖 " + tagSummary + "，具备扎实的工程落地经验");
        } else {
            highlights.add("掌握 " + tagSummary + " 等主流技术栈，具备规范的模块分层与业务实现能力");
        }

        if (!detectedMetrics.isEmpty()) {
            highlights.add("履历体现清晰的技术产出量化意识（如：" + String.join("、", detectedMetrics.subList(0, Math.min(2, detectedMetrics.size()))) + "），架构攻坚成效显著");
        } else if (detectedTags.contains("Redis") || detectedTags.contains("MySQL") || detectedTags.contains("Kafka")) {
            highlights.add("具备核心中间件（如数据持久化、多级缓存与高可用消息流）的应用与线上调优意识");
        } else {
            highlights.add("项目工程规范良好，具备良好的系统化开发与工程协同经验");
        }

        if (detectedProjects.size() >= 2) {
            highlights.add("在【" + detectedProjects.get(1) + "】中展现出良好的业务抽象与系统演进能力，与目标岗位【" + targetJob + "】方向契合");
        }

        // 2. 生成基于真实内容的薄弱项与改进点
        List<String> gaps = new ArrayList<>();
        if (detectedMetrics.isEmpty()) {
            gaps.add("核心项目职责中缺少量化业务收益（如核心接口QPS吞吐提升比、慢SQL耗时压降比例、故障率降幅）");
        } else {
            gaps.add("对简历中提及的量化指标（" + detectedMetrics.get(0) + "）建议在面试前准备详尽的压测火焰图或监控基准数据，增强面试答辩可信度");
        }

        if (!detectedProjects.isEmpty()) {
            gaps.add("在【" + detectedProjects.get(0) + "】中针对极端网络抖动、服务雪崩及分布式一致性异常的容灾兜底策略阐述相对精简");
        } else {
            gaps.add("建议在简历中以独立大标题明确【项目名称】与【个人职责】，突出主导模块的技术攻坚深度");
        }

        // 3. 生成基于真实项目和技术的预测深挖考题
        List<String> questions = new ArrayList<>();
        String primaryTag = detectedTags.get(0);
        if (!detectedProjects.isEmpty()) {
            questions.add("结合你在【" + detectedProjects.get(0) + "】中使用 " + primaryTag + " 的场景，推演当突发流量超出峰值时，你们的全链路限流熔断与降级保活机制是如何落地的？");
        } else {
            questions.add("结合你掌握的 " + primaryTag + "，请详细说明在高并发场景下如何做多级缓存设计与缓存一致性保障？");
        }

        if (detectedProjects.size() >= 2) {
            questions.add("针对【" + detectedProjects.get(1) + "】的数据存储架构，单表数据量达千万级时你们是如何权衡分库分表与跨分片分页查询的？");
        } else if (detectedTags.contains("MySQL") || detectedTags.contains("Redis")) {
            questions.add("在核心业务链路中，如何通过聚簇索引与覆盖索引优化深分页？遭遇缓存穿透与大Key淘汰时如何保证数据库不被击穿？");
        } else {
            questions.add("在分布式架构下，跨服务调用若发生网络分区或超时，你如何设计防重幂等与柔性事务最终一致性补偿？");
        }

        if (!detectedMetrics.isEmpty()) {
            questions.add("你在简历中提到【" + detectedMetrics.get(0) + "】的优化成效，请复盘从指标监控、抓取瓶颈火焰图到最终技术落地的完整方法论？");
        } else {
            questions.add("如果现有系统的并发量与数据体量翻 10 倍，你认为当前架构中最先崩溃的单点是什么？你将如何重构？");
        }

        // 4. STAR 法则指导建议
        String pFocus = !detectedProjects.isEmpty() ? "【" + detectedProjects.get(0) + "】" : "核心项目";
        String star = "针对目标岗位【" + targetJob + "】与目标企业【" + targetCompany + "】，建议紧扣 STAR 原则重构" + pFocus + "描述：明确 S(面临的业务洪峰/瓶颈痛点) ➔ T(架构攻坚与稳定性目标) ➔ A(采用的具体底层机制与重构方案) ➔ R(以量化数据呈现吞吐量与可用性提升)，大厂招聘委员会非常看重量化结果。";

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

    private List<String> extractTechTags(String text) {
        List<String> found = new ArrayList<>();
        if (!StringUtils.hasText(text)) {
            return found;
        }
        for (String tag : TECH_DICTIONARY) {
            Pattern p;
            if (tag.matches("^[A-Za-z0-9_+#.-]+$")) {
                p = Pattern.compile("(?i)(?<=^|[^A-Za-z0-9_])" + Pattern.quote(tag) + "(?=[^A-Za-z0-9_]|$)");
            } else {
                p = Pattern.compile(Pattern.quote(tag), Pattern.CASE_INSENSITIVE);
            }
            if (p.matcher(text).find()) {
                found.add(tag);
            }
        }
        return found;
    }

    private List<String> extractProjects(String text) {
        List<String> projects = new ArrayList<>();
        if (!StringUtils.hasText(text)) {
            return projects;
        }

        // 1. 显式前缀匹配：如 "项目名称：智能教育系统", "项目经历：分布式电商平台"
        Pattern p1 = Pattern.compile("(?:项目名称|项目经历|项目经验|核心项目|项目)[：:\\s]+([\\u4e00-\\u9fa5A-Za-z0-9_（）()\\-—]{3,30})");
        Matcher m1 = p1.matcher(text);
        while (m1.find() && projects.size() < 4) {
            String pName = m1.group(1).trim();
            if (isValidProjectName(pName) && !projects.contains(pName)) {
                projects.add(pName);
            }
        }

        // 2. 书名号或括号形式：【xxx系统】、【xxx平台】
        Pattern p2 = Pattern.compile("【([\\u4e00-\\u9fa5A-Za-z0-9_（）()\\-—]{3,25}(?:系统|平台|中台|中心|工程|引擎|助手|商城|后台|架构|组件|小程序|APP))】");
        Matcher m2 = p2.matcher(text);
        while (m2.find() && projects.size() < 4) {
            String pName = m2.group(1).trim();
            if (isValidProjectName(pName) && !projects.contains(pName)) {
                projects.add(pName);
            }
        }

        // 3. 常见行扫描：以“系统/平台/中台/中心”结尾的独立短标题行
        if (projects.size() < 2) {
            String[] lines = text.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.length() >= 4 && trimmed.length() <= 28) {
                    if (trimmed.endsWith("系统") || trimmed.endsWith("平台") || trimmed.endsWith("中台")
                            || trimmed.endsWith("中心") || trimmed.endsWith("商城") || trimmed.endsWith("引擎")) {
                        if (isValidProjectName(trimmed) && !projects.contains(trimmed)) {
                            projects.add(trimmed);
                            if (projects.size() >= 4) break;
                        }
                    }
                }
            }
        }

        return projects;
    }

    private boolean isValidProjectName(String name) {
        if (!StringUtils.hasText(name)) return false;
        if (name.contains("职责") || name.contains("业绩") || name.contains("描述") || name.contains("背景")
                || name.contains("技术栈") || name.contains("环境") || name.contains("经验") || name.contains("http")) {
            return false;
        }
        return true;
    }

    private List<String> extractMetrics(String text) {
        List<String> metrics = new ArrayList<>();
        if (!StringUtils.hasText(text)) return metrics;

        Pattern p = Pattern.compile("([0-9]+[万Ww千Kk+]*\\s*(?:QPS|TPS|PV|UV|并发)|(?:降低|提升|减少|增加|压降|优化|缩减)[了为\\s]*[0-9]+(?:\\.[0-9]+)?%|(?:由|从)[0-9a-zA-Z\\.]+[降提高至压到]+[0-9]+(?:\\.[0-9]+)?(?:ms|毫秒|秒|s|%)|[0-9]+(?:\\.[0-9]+)?(?:ms|毫秒)\\s*(?:响应|延迟|耗时)|(?:千万|百万|亿)级[0-9\\u4e00-\\u9fa5]{0,6})");
        Matcher m = p.matcher(text);
        while (m.find() && metrics.size() < 4) {
            String val = m.group().trim();
            if (!metrics.contains(val)) {
                metrics.add(val);
            }
        }
        return metrics;
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
        vo.setScoreDetails(buildScoreDetails(resume));
        return vo;
    }

    private int calcTechStackScore(List<String> tags) {
        if (tags == null || tags.isEmpty()) return 2;
        boolean hasLang = false, hasMw = false, hasDb = false, hasCloud = false, hasArch = false;
        for (String tag : tags) {
            String lower = tag.toLowerCase();
            if (lower.contains("java") || lower.contains("go") || lower.contains("python") || lower.contains("c++") || lower.contains("spring")) hasLang = true;
            if (lower.contains("redis") || lower.contains("kafka") || lower.contains("rocketmq") || lower.contains("netty") || lower.contains("mq")) hasMw = true;
            if (lower.contains("mysql") || lower.contains("elasticsearch") || lower.contains("tidb") || lower.contains("clickhouse") || lower.contains("oracle")) hasDb = true;
            if (lower.contains("docker") || lower.contains("k8s") || lower.contains("kubernetes") || lower.contains("linux") || lower.contains("nginx") || lower.contains("rag") || lower.contains("大模型")) hasCloud = true;
            if (lower.contains("分布式") || lower.contains("分库分表") || lower.contains("缓存") || lower.contains("调优") || lower.contains("ddd") || lower.contains("锁")) hasArch = true;
        }
        int score = 0;
        if (hasLang) score += 2;
        if (hasMw) score += 2;
        if (hasDb) score += 2;
        if (hasCloud) score += 2;
        if (hasArch) score += 2;
        return Math.min(score, 10);
    }

    private int calcProjectScore(List<String> projects, String text) {
        int score = 0;
        if (projects != null && !projects.isEmpty()) {
            score += 4;
            if (projects.size() >= 2) {
                score += 3;
            }
        } else {
            score += 2;
        }
        if (StringUtils.hasText(text)) {
            if (text.contains("高并发") || text.contains("分布式") || text.contains("微服务") || text.contains("千万") || text.contains("亿级") || text.contains("集群") || text.contains("海量")) {
                score += 3;
            }
        }
        return Math.min(score, 10);
    }

    private int calcMetricsScore(List<String> metrics, String text) {
        int score = 0;
        if (metrics != null && !metrics.isEmpty()) {
            score += 4;
        }
        if (StringUtils.hasText(text)) {
            if (text.contains("压降") || text.contains("提升") || text.contains("降低") || text.contains("缩短") || text.contains("优化至") || text.contains("提速")) {
                score += 3;
            }
            if (text.contains("故障") || text.contains("可用性") || text.contains("资损") || text.contains("流水") || text.contains("SLA") || text.contains("99.")) {
                score += 3;
            }
        }
        return Math.min(score, 10);
    }

    private int calcEngineeringScore(String text) {
        if (!StringUtils.hasText(text)) return 1;
        int score = 0;
        if (text.contains("熔断") || text.contains("降级") || text.contains("限流") || text.contains("Sentinel") || text.contains("容灾") || text.contains("链路追踪") || text.contains("SkyWalking") || text.contains("监控") || text.contains("压测")) {
            score += 3;
        } else {
            score += 1;
        }
        if (text.contains("单元测试") || text.contains("Code Review") || text.contains("代码规范") || text.contains("CI/CD") || text.contains("重构") || text.contains("DDD") || text.contains("自动化")) {
            score += 2;
        } else {
            score += 1;
        }
        return Math.min(score, 5);
    }

    private int calcFitScore(String targetJob, String targetCompany, List<String> tags, String text) {
        int score = 0;
        if (StringUtils.hasText(text) && StringUtils.hasText(targetJob)) {
            String cleanJob = targetJob.replaceAll("工程师|开发|专家|架构师", "").trim();
            if (cleanJob.length() >= 2 && text.toLowerCase().contains(cleanJob.toLowerCase())) {
                score += 3;
            } else {
                score += 2;
            }
        } else {
            score += 2;
        }

        if (tags != null && tags.size() >= 4) {
            score += 2;
        } else {
            score += 1;
        }
        return Math.min(score, 5);
    }

    private List<ResumeAnalysisVO.ScoreDimensionItem> buildScoreDetails(UserResume resume) {
        String text = resume.getRawContent() != null ? resume.getRawContent() : "";
        List<String> tags = parseJsonList(resume.getTechTags());
        List<String> projects = extractProjects(text);
        List<String> metrics = extractMetrics(text);

        int d1 = calcTechStackScore(tags);
        int d2 = calcProjectScore(projects, text);
        int d3 = calcMetricsScore(metrics, text);
        int d4 = calcEngineeringScore(text);
        int d5 = calcFitScore(resume.getTargetJob(), resume.getTargetCompany(), tags, text);

        List<ResumeAnalysisVO.ScoreDimensionItem> list = new ArrayList<>();
        list.add(new ResumeAnalysisVO.ScoreDimensionItem(
                "基准及格门槛",
                60, 60, "合格",
                "包含规范合法履历文本，享有大厂技术准入及格起评分"
        ));
        list.add(new ResumeAnalysisVO.ScoreDimensionItem(
                "核心技术栈广度与深度",
                d1, 10, d1 >= 8 ? "卓越" : (d1 >= 6 ? "良好" : "基础"),
                "覆盖主流语言、微服务、中间件、持久化及分布式高级架构机制（已识别 " + tags.size() + " 项技能）"
        ));
        list.add(new ResumeAnalysisVO.ScoreDimensionItem(
                "项目经历与系统复杂度",
                d2, 10, d2 >= 8 ? "卓越" : (d2 >= 6 ? "良好" : "基础"),
                projects.isEmpty() ? "未提取到明确命名的项目模块，建议以独立标题规范项目描述" : "已识别【" + String.join("、", projects) + "】等核心系统，具备分布式高并发业务演进深度"
        ));
        list.add(new ResumeAnalysisVO.ScoreDimensionItem(
                "STAR量化业务成效",
                d3, 10, d3 >= 8 ? "卓越" : (d3 >= 6 ? "良好" : "偏弱"),
                metrics.isEmpty() ? "项目职责缺乏量化指标，建议补充 QPS 吞吐、耗时压降与故障降幅" : "体现清晰量化收益（如：" + String.join("、", metrics.subList(0, Math.min(2, metrics.size()))) + "），架构说服力显著"
        ));
        list.add(new ResumeAnalysisVO.ScoreDimensionItem(
                "高可用工程与容灾规范",
                d4, 5, d4 >= 4 ? "优异" : "基础",
                d4 >= 4 ? "体现限流熔断、监控告警、容灾逃生或单元测试规范" : "建议补充全链路压测、分布式事务回滚及高可用防击穿策略"
        ));
        list.add(new ResumeAnalysisVO.ScoreDimensionItem(
                "目标岗位与名企契合度",
                d5, 5, d5 >= 4 ? "高契合" : "良好",
                "深度对标【" + (StringUtils.hasText(resume.getTargetJob()) ? resume.getTargetJob() : "后端开发") + "】及【" + (StringUtils.hasText(resume.getTargetCompany()) ? resume.getTargetCompany() : "一线互联网大厂") + "】技术要求"
        ));
        return list;
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