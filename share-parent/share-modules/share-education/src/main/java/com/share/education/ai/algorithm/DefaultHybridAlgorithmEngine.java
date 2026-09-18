package com.share.education.ai.algorithm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.education.ai.model.AlgorithmCandidateDTO;
import com.share.education.ai.model.UserProfileContext;
import com.share.education.domain.EduCourse;
import com.share.education.mapper.EduCourseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 默认混合基线推荐算法引擎 (用于在用户实验中的自研算法嵌入前提供高可用、高精度的多维特征召回)。
 *
 * <p>核心特征计算：
 * 1. 50 维技术图谱向量空间余弦相似度 (Cosine Vector Similarity)
 * 2. 离散技术栈图谱重叠度与关键词覆盖率
 * 3. 目标岗位契合加权与先修难度亲和度
 * 4. 学习热度对数平滑惩罚 (平衡冷门好课与全站爆款)
 * </p>
 */
@Component
public class DefaultHybridAlgorithmEngine implements IRecommendAlgorithmEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultHybridAlgorithmEngine.class);

    private final EduCourseMapper courseMapper;

    // 预置主流技术图谱向量维度 (与教育知识库对齐的 50 维技术特征空间)
    private static final String[] TECH_FEATURES = {
        "Java", "Spring", "SpringBoot", "SpringCloud", "MyBatis", "MySQL", "Redis", "Kafka",
        "RabbitMQ", "JVM", "Linux", "Docker", "Kubernetes", "Vue", "Vue3", "React", "TypeScript",
        "JavaScript", "Node.js", "Python", "Go", "Golang", "Rust", "C++", "PyTorch", "TensorFlow",
        "大语言模型", "LLM", "RAG", "LangChain", "Flink", "Spark", "Hadoop", "ClickHouse", "Elasticsearch",
        "微服务", "高并发", "分布式", "中间件", "性能优化", "设计模式", "网络编程", "数据结构与算法",
        "系统架构", "安全渗透", "Web安全", "Flutter", "鸿蒙", "HarmonyOS", "区块链"
    };

    public DefaultHybridAlgorithmEngine(EduCourseMapper courseMapper) {
        this.courseMapper = courseMapper;
    }

    @Override
    public List<AlgorithmCandidateDTO> recallCandidates(Long userId, UserProfileContext profile, int topK) {
        int safeLimit = Math.max(1, Math.min(topK, 50));

        // 1. 查询所有有效上架课程
        List<EduCourse> activeCourses = courseMapper.selectList(
            new LambdaQueryWrapper<EduCourse>()
                .eq(EduCourse::getStatus, 1)
                .orderByDesc(EduCourse::getLearnerCount)
        );

        if (activeCourses == null || activeCourses.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> enrolledSet = (profile != null && profile.getEnrolledCourseIds() != null)
                ? profile.getEnrolledCourseIds() : Collections.emptySet();

        Map<String, Integer> userSkills = (profile != null && profile.getSkillWeights() != null)
                ? profile.getSkillWeights() : Collections.emptyMap();
        String intendedRole = (profile != null && profile.getIntendedRole() != null)
                ? profile.getIntendedRole() : "";
        int preferredDifficulty = (profile != null && profile.getPreferredDifficulty() != null)
                ? profile.getPreferredDifficulty() : 2;

        // 1.1 学科领域硬隔离过滤器 (Category / Tag Boundary Discipline Domain Filtering)
        DisciplineDomain domain = resolveDomain(intendedRole);
        List<EduCourse> domainFilteredCourses = activeCourses.stream()
            .filter(c -> isCourseAllowedForDomain(c, domain))
            .collect(Collectors.toList());
        if (domainFilteredCourses.isEmpty()) {
            log.warn("[DomainBoundaryFilter] 学科硬隔离过滤后候选集为空，平滑降级为全量课程: intendedRole={}, domain={}", intendedRole, domain);
            domainFilteredCourses = activeCourses;
        }

        // 2. 构建学员技能特征向量
        double[] userVector = buildFeatureVector(userSkills);

        List<AlgorithmCandidateDTO> candidates = new ArrayList<>();

        for (EduCourse c : domainFilteredCourses) {
            // 过滤已购买或已加入学习计划的课程
            if (enrolledSet.contains(c.getId())) {
                continue;
            }

            double baseScore = 40.0;
            Map<String, Object> featureMap = new LinkedHashMap<>();

            // 2.1 密集技术向量余弦相似度 (0 ~ 35 分)
            double[] courseVector = buildCourseVector(c);
            double cosSim = computeCosineSimilarity(userVector, courseVector);
            double vectorScore = cosSim * 35.0;
            featureMap.put("vectorSimilarity", String.format("%.4f", cosSim));
            featureMap.put("vectorScore", String.format("%.2f", vectorScore));

            // 2.2 离散技术标签重叠得分 (0 ~ 35 分)
            double discreteScore = 0.0;
            List<String> matchedSkills = new ArrayList<>();
            if (StringUtils.hasText(c.getSkills()) && !userSkills.isEmpty()) {
                String[] skills = c.getSkills().split("[,，、 ]+");
                for (String s : skills) {
                    String trimmed = s.trim();
                    if (userSkills.containsKey(trimmed)) {
                        int w = userSkills.get(trimmed);
                        discreteScore += w * 0.35;
                        matchedSkills.add(trimmed);
                    }
                }
            }
            double skillScore = Math.min(35.0, discreteScore);
            featureMap.put("matchedSkills", matchedSkills);

            // 融合向量与离散得分 (取其长者并提供适度协同提升)
            double techMatchScore = Math.max(vectorScore, skillScore);
            baseScore += techMatchScore;

            // 2.3 目标职业岗位对齐度 (0 ~ 15 分)
            boolean roleMatched = false;
            if (StringUtils.hasText(intendedRole) && StringUtils.hasText(c.getTargetRole())) {
                if (c.getTargetRole().contains(intendedRole) || intendedRole.contains(c.getTargetRole())) {
                    baseScore += 15.0;
                    roleMatched = true;
                } else if ((intendedRole.contains("Java") || intendedRole.contains("后端") || intendedRole.contains("服务端"))
                        && (c.getTargetRole().contains("Java") || c.getTargetRole().contains("后端") || c.getTargetRole().contains("开发") || c.getTargetRole().contains("架构师"))) {
                    baseScore += 12.0;
                    roleMatched = true;
                }
            }
            featureMap.put("roleMatched", roleMatched);

            // 2.4 难度适配平滑度 (0 ~ 10 分)
            int courseDiff = c.getDifficultyLevel() != null ? c.getDifficultyLevel() : 2;
            if (courseDiff == preferredDifficulty) {
                baseScore += 10.0;
            } else if (courseDiff == preferredDifficulty + 1) {
                // 适度挑战进阶，给予 7 分
                baseScore += 7.0;
            } else {
                baseScore += 3.0;
            }
            featureMap.put("difficultyAlignment", courseDiff);

            // 2.5 学习者热度与评分对数平滑奖励 (0 ~ 5 分)
            int learners = c.getLearnerCount() != null ? c.getLearnerCount() : 0;
            double popularityBonus = Math.min(5.0, Math.log10(Math.max(1, learners)) * 1.2);
            baseScore += popularityBonus;
            featureMap.put("popularityBonus", String.format("%.2f", popularityBonus));

            // 归一化总得分到 0 ~ 100
            double finalScore = Math.min(99.5, Math.max(30.0, baseScore));

            // 标签标注
            String matchTag = (roleMatched && techMatchScore > 20) ? "岗位强契合"
                : (techMatchScore > 25 ? "核心技术对齐"
                : (courseDiff > preferredDifficulty ? "架构跃升突破"
                : (learners > 15000 ? "全站爆款好课" : "精选进阶")));

            candidates.add(AlgorithmCandidateDTO.builder()
                .courseId(c.getId())
                .score(finalScore)
                .matchTag(matchTag)
                .featureMap(featureMap)
                .build());
        }

        // 按得分降序排序，取 Top K
        candidates.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return candidates.stream().limit(safeLimit).toList();
    }

    @Override
    public String getEngineName() {
        return "DefaultHybridBaseline-50D-Cosine-Graph";
    }

    private static boolean containsFeature(String text, String feature) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(feature)) {
            return false;
        }
        boolean isChinese = feature.chars().anyMatch(ch -> Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN);
        if (isChinese) {
            return text.toLowerCase().contains(feature.toLowerCase());
        }
        String escaped = java.util.regex.Pattern.quote(feature.toLowerCase());
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?<![a-zA-Z0-9_])" + escaped + "(?![a-zA-Z0-9_])");
        return pattern.matcher(text.toLowerCase()).find();
    }

    private double[] buildFeatureVector(Map<String, Integer> skills) {
        double[] vec = new double[TECH_FEATURES.length];
        if (skills == null || skills.isEmpty()) {
            return vec;
        }
        for (int i = 0; i < TECH_FEATURES.length; i++) {
            String f = TECH_FEATURES[i];
            for (Map.Entry<String, Integer> entry : skills.entrySet()) {
                String skillKey = entry.getKey();
                if (skillKey.equalsIgnoreCase(f) || containsFeature(skillKey, f)) {
                    vec[i] = Math.max(vec[i], entry.getValue() / 100.0);
                }
            }
        }
        return vec;
    }

    private double[] buildCourseVector(EduCourse c) {
        double[] vec = new double[TECH_FEATURES.length];
        String combined = ((c.getCourseName() != null ? c.getCourseName() : "") + " "
                + (c.getSkills() != null ? c.getSkills() : "") + " "
                + (c.getTargetRole() != null ? c.getTargetRole() : "") + " "
                + (c.getDescription() != null ? c.getDescription() : ""));
        for (int i = 0; i < TECH_FEATURES.length; i++) {
            String f = TECH_FEATURES[i];
            if (containsFeature(combined, f)) {
                vec[i] = 1.0;
            }
        }
        return vec;
    }

    private double computeCosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public enum DisciplineDomain {
        JAVA_BACKEND,
        FRONTEND,
        BIG_DATA,
        AI_LLM,
        GO_CLOUD_NATIVE,
        MOBILE,
        GENERAL
    }

    public static DisciplineDomain resolveDomain(String intendedRole) {
        if (!StringUtils.hasText(intendedRole)) {
            return DisciplineDomain.JAVA_BACKEND;
        }
        String lower = intendedRole.toLowerCase();
        if (lower.contains("go") || lower.contains("golang") || lower.contains("云原生")) {
            return DisciplineDomain.GO_CLOUD_NATIVE;
        }
        if (lower.contains("前端") || lower.contains("web") || lower.contains("vue") || lower.contains("react")) {
            return DisciplineDomain.FRONTEND;
        }
        if (lower.contains("大数据") || lower.contains("数仓") || lower.contains("spark")
                || lower.contains("flink") || lower.contains("数据开发")) {
            return DisciplineDomain.BIG_DATA;
        }
        if (lower.contains("移动") || lower.contains("flutter") || lower.contains("安卓")
                || lower.contains("android") || lower.contains("ios") || lower.contains("鸿蒙")) {
            return DisciplineDomain.MOBILE;
        }
        if (lower.contains("大模型") || lower.contains("大语言模型") || lower.contains("语言模型")
                || lower.contains("llm") || lower.contains("ai大模型") || lower.contains("人工智能")
                || lower.contains("nlp") || lower.contains("算法工程")) {
            return DisciplineDomain.AI_LLM;
        }
        if (lower.contains("java") || lower.contains("后端") || lower.contains("服务端") || lower.contains("后台")
                || lower.contains("spring") || lower.contains("架构")) {
            return DisciplineDomain.JAVA_BACKEND;
        }
        return DisciplineDomain.JAVA_BACKEND;
    }

    public static boolean isCourseAllowedForDomain(EduCourse c, DisciplineDomain domain) {
        if (c == null || domain == DisciplineDomain.GENERAL) {
            return true;
        }
        Long catId = c.getCategoryId();
        String cName = c.getCourseName() != null ? c.getCourseName().toLowerCase() : "";
        String skills = c.getSkills() != null ? c.getSkills().toLowerCase() : "";
        String targetRole = c.getTargetRole() != null ? c.getTargetRole().toLowerCase() : "";
        String desc = c.getDescription() != null ? c.getDescription().toLowerCase() : "";
        String combined = cName + " " + skills + " " + targetRole + " " + desc;

        switch (domain) {
            case JAVA_BACKEND:
                // 白名单门类：2 (后端开发), 4 (数据库)
                if (catId == null || (catId != 2L && catId != 4L)) {
                    return false;
                }
                // 硬隔离黑名单排除：强制过滤掉 Go、Rust、大数据/数仓、Python/NLP、移动端、前端、云原生/K8s、区块链等
                if (combined.contains("go 语言") || combined.contains("go语言") || combined.contains("golang")
                        || combined.contains("goroutine") || combined.contains("kratos") || combined.contains("geecache")
                        || combined.contains("go后端") || combined.contains("go web") || combined.contains("gin框架")
                        || combined.contains("gin ") || combined.contains("grpc") || combined.contains("protobuf")
                        || combined.contains("rust")
                        || combined.contains("kubernetes") || combined.contains("k8s") || combined.contains("terraform")
                        || combined.contains("istio") || combined.contains("service mesh") || combined.contains("iac")
                        || combined.contains("大数据") || combined.contains("hadoop") || combined.contains("flink")
                        || combined.contains("spark") || combined.contains("datax") || combined.contains("sqoop")
                        || combined.contains("clickhouse") || combined.contains("hive") || combined.contains("hbase")
                        || combined.contains("数仓") || combined.contains("离线计算") || combined.contains("流批一体")
                        || combined.contains("doris") || targetRole.contains("数据开发") || targetRole.contains("大数据")
                        || combined.contains("python") || combined.contains("django") || combined.contains("fastapi")
                        || combined.contains("flask") || combined.contains("nlp") || combined.contains("word2vec")
                        || combined.contains("大模型") || combined.contains("llm") || combined.contains("rag")
                        || combined.contains("langchain") || combined.contains("pytorch") || combined.contains("tensorflow")
                        || combined.contains("深度学习") || combined.contains("机器学习")
                        || targetRole.contains("nlp") || targetRole.contains("算法工程")
                        || combined.contains("vue") || combined.contains("react") || combined.contains("typescript")
                        || combined.contains("javascript") || combined.contains("html/css") || targetRole.contains("前端")
                        || combined.contains("flutter") || combined.contains("android") || combined.contains("ios")
                        || combined.contains("鸿蒙") || combined.contains("harmonyos") || combined.contains("harmony")
                        || combined.contains("安卓") || targetRole.contains("移动")
                        || combined.contains("c++20") || combined.contains("c++") || combined.contains("cpp")
                        || combined.contains("node.js") || combined.contains("nodejs")
                        || combined.contains("express") || combined.contains("koa")
                        || combined.contains("区块链") || combined.contains("solidity") || combined.contains("游戏开发")
                        || combined.contains("unity") || combined.contains("unreal") || combined.contains("渗透测试")) {
                    return false;
                }
                // 白名单技能/方向要求 (至少满足一项 Java/后端/数据库/计算机基础 核心关键词)
                boolean matchesJavaBackend = combined.contains("java") || combined.contains("spring")
                        || combined.contains("springboot") || combined.contains("springcloud") || combined.contains("mybatis")
                        || combined.contains("mysql") || combined.contains("redis") || combined.contains("kafka")
                        || combined.contains("rabbitmq") || combined.contains("rocketmq") || combined.contains("jvm")
                        || combined.contains("linux") || combined.contains("数据库") || combined.contains("sql")
                        || combined.contains("微服务") || combined.contains("高并发") || combined.contains("分布式")
                        || combined.contains("中间件") || combined.contains("网络编程") || combined.contains("数据结构")
                        || combined.contains("算法") || combined.contains("操作系统") || combined.contains("计算机网络")
                        || combined.contains("设计模式") || combined.contains("netty") || combined.contains("juc")
                        || combined.contains("seata") || combined.contains("ddd") || combined.contains("maven")
                        || combined.contains("tomcat") || combined.contains("sharding") || combined.contains("后端");
                return matchesJavaBackend;

            case FRONTEND:
                if (catId == null || catId != 1L) return false;
                if (combined.contains("java") || combined.contains("spring") || combined.contains("rust")
                        || combined.contains("golang") || combined.contains("大数据") || combined.contains("hadoop")
                        || combined.contains("flutter") || combined.contains("鸿蒙")) {
                    return false;
                }
                return combined.contains("vue") || combined.contains("react") || combined.contains("typescript")
                        || combined.contains("javascript") || combined.contains("前端") || combined.contains("html")
                        || combined.contains("css") || combined.contains("web");

            case BIG_DATA:
                if (catId == null || (catId != 7L && catId != 4L)) return false;
                if (combined.contains("vue") || combined.contains("react") || combined.contains("前端")
                        || combined.contains("flutter") || combined.contains("ios") || combined.contains("安卓")) {
                    return false;
                }
                return combined.contains("大数据") || combined.contains("hadoop") || combined.contains("flink")
                        || combined.contains("spark") || combined.contains("datax") || combined.contains("sqoop")
                        || combined.contains("clickhouse") || combined.contains("hive") || combined.contains("hbase")
                        || combined.contains("数仓") || combined.contains("数据开发") || combined.contains("mysql");

            case AI_LLM:
                if (catId == null || catId != 6L) return false;
                if (combined.contains("vue") || combined.contains("react") || combined.contains("前端")
                        || combined.contains("flutter") || combined.contains("安卓") || combined.contains("区块链")) {
                    return false;
                }
                return combined.contains("ai") || combined.contains("人工智能") || combined.contains("大模型")
                        || combined.contains("llm") || combined.contains("rag") || combined.contains("langchain")
                        || combined.contains("nlp") || combined.contains("word2vec") || combined.contains("pytorch")
                        || combined.contains("tensorflow") || combined.contains("深度学习") || combined.contains("机器学习");

            case GO_CLOUD_NATIVE:
                if (catId == null || (catId != 2L && catId != 5L && catId != 4L)) return false;
                if (combined.contains("vue") || combined.contains("react") || combined.contains("前端")
                        || combined.contains("rust") || combined.contains("python") || combined.contains("大数据")) {
                    return false;
                }
                return combined.contains("go") || combined.contains("golang") || combined.contains("goroutine")
                        || combined.contains("k8s") || combined.contains("docker") || combined.contains("云原生");

            case MOBILE:
                if (catId == null || catId != 3L) return false;
                return combined.contains("flutter") || combined.contains("android") || combined.contains("ios")
                        || combined.contains("鸿蒙") || combined.contains("harmonyos") || combined.contains("安卓")
                        || combined.contains("移动端");

            default:
                return true;
        }
    }
}
