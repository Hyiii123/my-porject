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

                // 真实性后置校验审计：如果候选人简历完全没有技术栈且没有工程项目，坚决杜绝大模型客套幻觉
                List<String> realTags = extractTechTags(request.getResumeContent());
                List<String> realProjects = extractProjects(request.getResumeContent());
                if (realTags.isEmpty() && realProjects.isEmpty()) {
                    resume.setMatchScore(60);
                    resume.setMatchLevel("初阶成长型，亟待丰富项目与产出");
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

        // 1. 生成基于候选人真实项目和技能的高光亮点（求真务实，没有就直言没有）
        List<String> highlights = new ArrayList<>();
        if (detectedProjects.isEmpty() && detectedTags.isEmpty()) {
            highlights.add("【无对口技术亮点】：简历未检测到与【" + targetJob + "】相关的对口工程项目或核心技术栈，实战积累严重不足");
            highlights.add("【工程实战缺失】：履历内容未体现该技术方向所需的系统研发、架构设计或攻坚成果");
        } else if (!detectedProjects.isEmpty()) {
            String p1 = detectedProjects.get(0);
            String tagSummary = !detectedTags.isEmpty() ? String.join("、", detectedTags.subList(0, Math.min(4, detectedTags.size()))) : "业务核心";
            highlights.add("深度参与/主导【" + p1 + "】核心模块研发，技术体系涵盖 " + tagSummary + "，具备扎实的工程落地经验");
            if (!detectedMetrics.isEmpty()) {
                highlights.add("履历体现清晰的技术产出量化意识（如：" + String.join("、", detectedMetrics.subList(0, Math.min(2, detectedMetrics.size()))) + "），架构攻坚成效显著");
            } else if (detectedProjects.size() >= 2) {
                highlights.add("在【" + detectedProjects.get(1) + "】中展现出良好的业务抽象与系统演进能力，具备多系统开发经验");
            } else {
                highlights.add("具备【" + p1 + "】完整的业务功能交付记录，建议进一步强化底层架构与技术攻坚深度");
            }
        } else {
            String tagSummary = String.join("、", detectedTags.subList(0, Math.min(4, detectedTags.size())));
            highlights.add("具备 " + tagSummary + " 等基础技能储备，但简历中未呈现独立完整的端到端实战项目");
            highlights.add("掌握相关技术基础语法与应用，建议尽快补充对口项目落地沉淀，提升工程说服力");
        }

        // 2. 生成基于真实内容的薄弱项与改进点
        List<String> gaps = new ArrayList<>();
        if (detectedTags.isEmpty()) {
            gaps.add("【技术栈脱节】：简历未出现与目标岗位【" + targetJob + "】相关的对口编程语言与技术栈，专业储备不足");
        }
        if (detectedProjects.isEmpty()) {
            gaps.add("【工程项目缺失】：简历中无明确命名的技术/系统研发项目经历，大厂技术初筛通过率极低");
        } else {
            gaps.add("在【" + detectedProjects.get(0) + "】中针对极端网络抖动、服务雪崩及分布式一致性异常的容灾兜底策略阐述相对精简");
        }

        if (detectedMetrics.isEmpty()) {
            gaps.add("【缺乏量化成效】：项目职责以日常工作罗列为主，缺少量化业务收益（如核心接口QPS吞吐、SQL耗时压降比、系统SLA可用性）");
        } else {
            gaps.add("对简历中提及的量化指标（" + detectedMetrics.get(0) + "）建议在面试前准备详尽的压测火焰图或监控基准数据，增强面试答辩可信度");
        }

        // 3. 生成基于真实项目和技术的预测深挖考题（绝不凭空捏造候选人没写过的技术）
        List<String> questions = new ArrayList<>();
        if (detectedProjects.isEmpty() && detectedTags.isEmpty()) {
            questions.add("【经历真实性核查】：简历中未出现与【" + targetJob + "】对口的技术研发项目，请如实说明你跨界应聘该岗位的动机与真实水平？");
            questions.add("【实际动手能力】：针对【" + targetJob + "】专业领域，你目前是否有可供核验的独立代码工程、开源实践或个人作品？");
            questions.add("【大厂门槛与差距】：面对【" + targetCompany + "】的技术招聘门槛，你打算如何系统性补齐缺乏真实业务项目的短板？");
        } else if (!detectedProjects.isEmpty()) {
            String p1 = detectedProjects.get(0);
            String primaryTag = !detectedTags.isEmpty() ? detectedTags.get(0) : "核心模块";
            questions.add("结合你在【" + p1 + "】中使用 " + primaryTag + " 的真实场景，请详细说明其核心架构设计是怎样的？遇到过最棘手的线上故障或性能瓶颈是什么？");
            if (detectedProjects.size() >= 2) {
                String p2 = detectedProjects.get(1);
                questions.add("在【" + p2 + "】的设计与落地过程中，你承担的最核心技术职责是什么？与【" + p1 + "】相比在架构复杂度上有何演进？");
            } else if (detectedTags.size() >= 2) {
                String secondaryTag = detectedTags.get(1);
                questions.add("你在【" + p1 + "】中同时使用了 " + primaryTag + " 与 " + secondaryTag + "，两者的职责边界如何划分？数据一致性或网络调用超时如何保证？");
            } else {
                questions.add("针对【" + p1 + "】，如果核心业务流量未来增长 10 倍，你认为系统最先面临瓶颈的单点是什么？你将如何重构？");
            }

            if (!detectedMetrics.isEmpty()) {
                questions.add("你在简历中提到【" + detectedMetrics.get(0) + "】的量化成效，请复盘其测量基准、优化方案及最终验证方法论？");
            } else {
                questions.add("简历在【" + p1 + "】中并未列出具体吞吐与响应耗时指标，请如实复盘该系统上线后的真实流量规模与线上稳定性表现？");
            }
        } else {
            String primaryTag = detectedTags.get(0);
            questions.add("简历提及你掌握 " + primaryTag + "，请结合底层原理详细阐述其核心实现机制？你在哪些实际场景中动手应用过？");
            questions.add("当前履历中缺少明确的技术项目经历，请说明你在学习或实践 " + primaryTag + " 过程中完成过哪些实验或作品？");
            questions.add("如果面试官要求现场针对【" + targetJob + "】手撕一段生产级代码，你最有把握实现的业务或算法模块是什么？");
        }

        // 4. STAR 法则指导建议
        String star;
        if (detectedProjects.isEmpty() && detectedTags.isEmpty()) {
            star = "当前简历内容与技术岗位【" + targetJob + "】严重脱节，无法套用现有经历进行技术面 STAR 重塑。强烈建议：首先围绕【" + targetJob + "】核心技术栈动手落地至少一个完整的端到端实战项目，再按 S(业务场景/技术难点) ➔ T(架构攻坚目标) ➔ A(具体技术选型与实现细节) ➔ R(性能压测或实际量化成效) 的规范标准重新撰写履历。";
        } else {
            String pFocus = !detectedProjects.isEmpty() ? "【" + detectedProjects.get(0) + "】" : "核心项目";
            star = "针对目标岗位【" + targetJob + "】与目标企业【" + targetCompany + "】，建议紧扣 STAR 原则重构" + pFocus + "描述：明确 S(面临的业务洪峰/瓶颈痛点) ➔ T(架构攻坚与稳定性目标) ➔ A(采用的具体底层机制与重构方案) ➔ R(以量化数据呈现吞吐量与可用性提升)，大厂招聘委员会非常看重量化结果。";
        }

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

    private List<String> getJobTrackKeywords(String job) {
        List<String> kws = new ArrayList<>();
        if (!StringUtils.hasText(job)) {
            return List.of("开发", "系统", "软件", "工程");
        }
        String lower = job.toLowerCase();
        if (lower.contains("java")) {
            kws.addAll(List.of("java", "spring", "jvm", "mybatis", "netty", "tomcat", "maven", "spring boot", "微服务"));
        } else if (lower.contains("go") || lower.contains("golang")) {
            kws.addAll(List.of("go", "golang", "gin", "gorm", "协程", "channel", "grpc"));
        } else if (lower.contains("c++") || lower.contains("cpp")) {
            kws.addAll(List.of("c++", "cpp", "stl", "linux", "指针", "内存", "makefile", "cmake"));
        } else if (lower.contains("python")) {
            kws.addAll(List.of("python", "django", "fastapi", "flask", "numpy", "pandas", "pytorch"));
        } else if (lower.contains("前端") || lower.contains("web") || lower.contains("vue") || lower.contains("react")) {
            kws.addAll(List.of("vue", "react", "javascript", "typescript", "js", "ts", "css", "html", "vite", "webpack", "前端", "node"));
        } else if (lower.contains("ai") || lower.contains("大模型") || lower.contains("llm") || lower.contains("算法") || lower.contains("nlp") || lower.contains("cv") || lower.contains("rag") || lower.contains("agent")) {
            kws.addAll(List.of("llm", "大模型", "rag", "agent", "nlp", "cv", "算法", "pytorch", "tensorflow", "transformer", "embedding", "向量", "微调", "prompt"));
        } else if (lower.contains("大数据") || lower.contains("spark") || lower.contains("flink") || lower.contains("hadoop") || lower.contains("数仓")) {
            kws.addAll(List.of("spark", "flink", "hadoop", "hive", "hbase", "kafka", "数仓", "数据仓库", "clickhouse", "etl", "离线", "实时计算"));
        } else if (lower.contains("数据库") || lower.contains("dba") || lower.contains("存储")) {
            kws.addAll(List.of("mysql", "redis", "dba", "sql", "索引", "事务", "tidb", "clickhouse", "elasticsearch", "分库分表", "慢查询", "调优"));
        } else if (lower.contains("云原生") || lower.contains("sre") || lower.contains("运维") || lower.contains("k8s") || lower.contains("devops")) {
            kws.addAll(List.of("k8s", "kubernetes", "docker", "devops", "ci/cd", "prometheus", "sre", "linux", "运维", "容器", "监控", "grafana"));
        } else if (lower.contains("测试") || lower.contains("sdet") || lower.contains("安全")) {
            kws.addAll(List.of("测试", "自动化", "jmeter", "pytest", "selenium", "压测", "用例", "安全", "渗透", "漏洞", "sdet"));
        } else {
            String clean = job.replaceAll("工程师|开发|专家|架构师|高级|资深|初级|助理", "").trim();
            if (clean.length() >= 2) {
                kws.add(clean);
            }
        }
        return kws;
    }

    private int calcTechStackScore(List<String> tags) {
        if (tags == null || tags.isEmpty()) return 0;
        boolean hasLang = false, hasMw = false, hasDb = false, hasCloud = false, hasArch = false;
        for (String tag : tags) {
            String lower = tag.toLowerCase();
            if (lower.contains("java") || lower.contains("go") || lower.contains("python") || lower.contains("c++") || lower.contains("spring") || lower.contains("vue") || lower.contains("react") || lower.contains("js") || lower.contains("ts")) hasLang = true;
            if (lower.contains("redis") || lower.contains("kafka") || lower.contains("rocketmq") || lower.contains("netty") || lower.contains("mq") || lower.contains("dubbo")) hasMw = true;
            if (lower.contains("mysql") || lower.contains("elasticsearch") || lower.contains("tidb") || lower.contains("clickhouse") || lower.contains("oracle") || lower.contains("mongodb")) hasDb = true;
            if (lower.contains("docker") || lower.contains("k8s") || lower.contains("kubernetes") || lower.contains("linux") || lower.contains("nginx") || lower.contains("rag") || lower.contains("大模型") || lower.contains("ci/cd")) hasCloud = true;
            if (lower.contains("分布式") || lower.contains("分库分表") || lower.contains("缓存") || lower.contains("调优") || lower.contains("ddd") || lower.contains("锁") || lower.contains("架构")) hasArch = true;
        }
        int score = 0;
        if (hasLang) score += 2;
        if (hasMw) score += 2;
        if (hasDb) score += 2;
        if (hasCloud) score += 2;
        if (hasArch) score += 2;
        if (score == 0 && !tags.isEmpty()) {
            score = Math.min(tags.size(), 2);
        }
        return Math.min(score, 10);
    }

    private int calcProjectScore(List<String> projects, String text) {
        if (projects == null || projects.isEmpty()) {
            return 0;
        }
        int score = 3;
        if (projects.size() >= 2) {
            score += 3;
        }
        if (StringUtils.hasText(text)) {
            if (text.contains("高并发") || text.contains("分布式") || text.contains("微服务") || text.contains("千万") || text.contains("亿级") || text.contains("集群") || text.contains("海量")) {
                score += 4;
            }
        }
        return Math.min(score, 10);
    }

    private int calcMetricsScore(List<String> metrics, String text) {
        if (!StringUtils.hasText(text)) return 0;
        int score = 0;
        if (metrics != null && !metrics.isEmpty()) {
            score += 4;
            if (metrics.size() >= 2) {
                score += 2;
            }
        }
        if (text.contains("压降") || text.contains("提升") || text.contains("降低") || text.contains("缩短") || text.contains("优化至") || text.contains("提速")) {
            score += 2;
        }
        if (text.contains("故障") || text.contains("可用性") || text.contains("资损") || text.contains("SLA") || text.contains("99.")) {
            score += 2;
        }
        return Math.min(score, 10);
    }

    private int calcEngineeringScore(String text) {
        if (!StringUtils.hasText(text)) return 0;
        int score = 0;
        if (text.contains("熔断") || text.contains("降级") || text.contains("限流") || text.contains("Sentinel") || text.contains("容灾") || text.contains("链路追踪") || text.contains("SkyWalking") || text.contains("监控") || text.contains("压测")) {
            score += 3;
        }
        if (text.contains("单元测试") || text.contains("Code Review") || text.contains("代码规范") || text.contains("CI/CD") || text.contains("重构") || text.contains("DDD") || text.contains("自动化")) {
            score += 2;
        }
        return Math.min(score, 5);
    }

    private int calcFitScore(String targetJob, String targetCompany, List<String> tags, String text) {
        if (!StringUtils.hasText(text)) return 0;
        String job = StringUtils.hasText(targetJob) ? targetJob : "";
        String lowerText = text.toLowerCase();

        // 1. 提取目标岗位的核心技术特征词库
        List<String> trackKeywords = getJobTrackKeywords(job);

        // 2. 统计在简历文本与识别出的技术标签中命中对口关键词的次数
        int trackHits = 0;
        for (String kw : trackKeywords) {
            if (lowerText.contains(kw.toLowerCase())) {
                trackHits++;
            }
        }
        if (tags != null) {
            for (String tag : tags) {
                for (String kw : trackKeywords) {
                    if (tag.equalsIgnoreCase(kw)) {
                        trackHits++;
                    }
                }
            }
        }

        // 赛道技术匹配度 (0~3分)
        int trackScore = 0;
        if (trackHits >= 4) {
            trackScore = 3;
        } else if (trackHits >= 2) {
            trackScore = 2;
        } else if (trackHits >= 1) {
            trackScore = 1;
        } else {
            trackScore = 0; // 零命中，说明完全脱离该技术赛道
        }

        // 3. 名企高并发与工程体量契合度 (0~2分)
        int companyScore = 0;
        // 若赛道完全不相干（0分），大厂契合度直接为 0
        if (trackScore > 0) {
            boolean hasHighScale = text.contains("高并发") || text.contains("海量") || text.contains("千万")
                    || text.contains("亿级") || text.contains("分布式") || text.contains("QPS")
                    || text.contains("架构") || text.contains("SLA") || text.contains("99.")
                    || text.contains("调优") || text.contains("微服务") || text.contains("容灾");
            boolean hasNormalScale = text.contains("项目") || text.contains("系统") || text.contains("重构")
                    || text.contains("上线") || text.contains("核心");

            if (hasHighScale && trackScore >= 2) {
                companyScore = 2;
            } else if (hasNormalScale || hasHighScale) {
                companyScore = 1;
            }
        }

        return Math.min(trackScore + companyScore, 5);
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

        // 维度1：核心技术栈广度与深度
        String d1Rating;
        String d1Desc;
        if (d1 == 0) {
            d1Rating = "匮乏";
            d1Desc = "未检测到核心技术栈标签，缺乏专业编程语言与工程组件说明";
        } else if (d1 <= 3) {
            d1Rating = "初浅";
            d1Desc = "仅掌握基础单一技术（已识别 " + tags.size() + " 项技能），缺乏中间件与存储架构体系";
        } else if (d1 <= 6) {
            d1Rating = "基础";
            d1Desc = "掌握常用编程语言与组件（已识别 " + tags.size() + " 项技能），建议进一步拓展微服务与分布式体系";
        } else if (d1 <= 8) {
            d1Rating = "良好";
            d1Desc = "具备较完善的技术栈体系（覆盖语言、数据库与核心中间件等 " + tags.size() + " 项技能）";
        } else {
            d1Rating = "卓越";
            d1Desc = "技术栈全面且具备深度（涵盖核心语言、微服务、中间件、持久化与分布式高级架构等 " + tags.size() + " 项技能）";
        }
        list.add(new ResumeAnalysisVO.ScoreDimensionItem("核心技术栈广度与深度", d1, 10, d1Rating, d1Desc));

        // 维度2：项目经历与系统复杂度
        String d2Rating;
        String d2Desc;
        if (d2 == 0) {
            d2Rating = "匮乏";
            d2Desc = "未提取到明确命名的软件工程项目经历，建议以规范标题（如【核心项目经历】）陈述";
        } else if (d2 <= 5) {
            d2Rating = "基础";
            d2Desc = "已识别【" + String.join("、", projects) + "】，业务体量与架构深度相对初阶";
        } else if (d2 <= 8) {
            d2Rating = "良好";
            d2Desc = "已识别【" + String.join("、", projects) + "】等核心系统，具备分布式业务演进深度";
        } else {
            d2Rating = "卓越";
            d2Desc = "核心系统架构复杂度突出，具备高并发、分布式微服务或海量业务深厚实战落地沉淀";
        }
        list.add(new ResumeAnalysisVO.ScoreDimensionItem("项目经历与系统复杂度", d2, 10, d2Rating, d2Desc));

        // 维度3：STAR量化业务成效
        String d3Rating;
        String d3Desc;
        if (d3 == 0) {
            d3Rating = "偏弱";
            d3Desc = "项目职责缺乏量化指标，建议补充 QPS 吞吐、耗时压降与故障降幅等量化成果";
        } else if (d3 <= 5) {
            d3Rating = "基础";
            d3Desc = "体现部分量化描述，建议进一步按照 STAR 原则明确业务成果对比（Before vs After）";
        } else if (d3 <= 8) {
            d3Rating = "良好";
            d3Desc = "体现清晰量化收益（如：" + String.join("、", metrics.subList(0, Math.min(2, metrics.size()))) + "），说服力显著";
        } else {
            d3Rating = "卓越";
            d3Desc = "量化成果详实且突出（如：" + String.join("、", metrics.subList(0, Math.min(2, metrics.size()))) + "），架构攻坚与业务价值说服力极强";
        }
        list.add(new ResumeAnalysisVO.ScoreDimensionItem("STAR量化业务成效", d3, 10, d3Rating, d3Desc));

        // 维度4：高可用工程与容灾规范
        String d4Rating;
        String d4Desc;
        if (d4 == 0) {
            d4Rating = "缺失";
            d4Desc = "未体现高可用容灾架构或工程规范，建议补充限流熔断、监控报警及自动化测试";
        } else if (d4 <= 2) {
            d4Rating = "基础";
            d4Desc = "体现基础工程规范，建议补充全链路压测、分布式事务回滚及高可用防击穿策略";
        } else {
            d4Rating = "优异";
            d4Desc = "体现限流熔断、监控告警、容灾逃生或单元测试等成熟工程防护防线";
        }
        list.add(new ResumeAnalysisVO.ScoreDimensionItem("高可用工程与容灾规范", d4, 5, d4Rating, d4Desc));

        // 维度5：目标岗位与名企契合度
        String d5Rating;
        String d5Desc;
        String targetJobStr = StringUtils.hasText(resume.getTargetJob()) ? resume.getTargetJob() : "后端开发";
        String targetCompStr = StringUtils.hasText(resume.getTargetCompany()) ? resume.getTargetCompany() : "一线互联网大厂";
        if (d5 == 0) {
            d5Rating = "严重脱节";
            d5Desc = "简历专业领域与【" + targetJobStr + "】严重脱离，未检索到对口技术栈，跨赛道匹配度极低";
        } else if (d5 == 1) {
            d5Rating = "微弱相关";
            d5Desc = "仅具备少量边缘关联技能，缺乏【" + targetJobStr + "】核心专业沉淀";
        } else if (d5 == 2) {
            d5Rating = "偏弱匹配";
            d5Desc = "具备部分基础技术，但与【" + targetJobStr + "】主流技术要求匹配度较低，建议补充垂直对口项目";
        } else if (d5 == 3) {
            d5Rating = "基本对口";
            d5Desc = "基本掌握【" + targetJobStr + "】核心基础，但距【" + targetCompStr + "】名企岗位的技术深度与工程体量仍有差距";
        } else if (d5 == 4) {
            d5Rating = "良好契合";
            d5Desc = "技术栈与经历紧扣【" + targetJobStr + "】，符合【" + targetCompStr + "】主流用人标准";
        } else {
            d5Rating = "深度对标";
            d5Desc = "深度对标【" + targetJobStr + "】及【" + targetCompStr + "】技术要求，技术架构与业务体量高度契合";
        }
        list.add(new ResumeAnalysisVO.ScoreDimensionItem("目标岗位与名企契合度", d5, 5, d5Rating, d5Desc));

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