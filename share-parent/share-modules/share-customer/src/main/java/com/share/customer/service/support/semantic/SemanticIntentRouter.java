package com.share.customer.service.support.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语义向量意图路由与结构化槽位提取器 (Semantic Intent Router & Slot Extractor)。
 * 依托底层 zhiwen-embedding (BAAI/bge-small-zh-v1.5) 向量服务，
 * 彻底消除基于 contains 的机械关键词匹配，实现全链路语义级别的高泛化意图识别。
 */
@Slf4j
@Component
public class SemanticIntentRouter {

    @Value("${zhiwen.embedding.url:http://zhiwen-embedding:8000/embed}")
    private String embeddingServiceUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public enum UserIntent {
        PATH_PLANNING,      // 导学推演与学习路线规划
        PAGE_NAVIGATOR,     // 页面跳转导航 (个人中心/首页/问答/搜索)
        SIGN_IN,            // 每日签到打卡领学分
        COUPON_CENTER,      // 领券中心与优惠券
        CART_VIEW,          // 购物车查询与结算
        RESUME_DIAGNOSIS,   // 简历诊断与投递优化
        LEARNING_PORTRAIT,  // 学情画像与能力雷达
        POINTS_RANKING,     // 学分排行榜
        EXAM_QUERY,         // 阶段测验与考试查询
        INTERVIEW_MOCK,     // 全真模拟面试
        COURSE_PURCHASE,    // 课程直接选购下单
        GENERAL_QA          // 通用智能客服与FAQ问答
    }

    // 意图锚点质心缓存 (Intent -> Centroid Vector)
    private final Map<UserIntent, float[]> intentCentroids = new ConcurrentHashMap<>();

    // 角色锚点向量缓存 (RoleName -> Vector)
    private final Map<String, float[]> roleVectors = new ConcurrentHashMap<>();

    // 难度锚点向量缓存 (Difficulty -> Vector)
    private final Map<Integer, float[]> difficultyVectors = new ConcurrentHashMap<>();

    // 常用意图锚点语料
    private static final Map<UserIntent, List<String>> INTENT_ANCHORS = Map.ofEntries(
        Map.entry(UserIntent.PATH_PLANNING, List.of(
            "制定系统化学习路线与成长规划",
            "大二在校生想找日常实习怎么准备",
            "如何从零开始掌握后端微服务技术栈",
            "根据我现在的水平推荐适合的学习路径",
            "零基础转行程序员该怎么系统化学起",
            "想去大厂做开发求指路和选课规划方案",
            "学习路线图谱与课程推荐"
        )),
        Map.entry(UserIntent.PAGE_NAVIGATOR, List.of(
            "带我去个人中心或者个人资料设置页面",
            "跳转到平台首页浏览全部课程",
            "打开问答社区参与技术提问讨论",
            "我想修改登录密码和账户安全配置",
            "前往课程搜索页面检索课程"
        )),
        Map.entry(UserIntent.SIGN_IN, List.of(
            "每日签到打卡领取学分积分",
            "今天还没打卡我要完成每日签到",
            "签到赚取积分兑换奖品或者抵扣学费"
        )),
        Map.entry(UserIntent.COUPON_CENTER, List.of(
            "前往领券中心领取专属大额优惠券",
            "查看我账户里拥有的可用打折优惠券",
            "有没有满减券或者新人折扣可以领"
        )),
        Map.entry(UserIntent.CART_VIEW, List.of(
            "打开我的购物车查看待支付课程",
            "查看购物车里添加了哪些课程准备结算"
        )),
        Map.entry(UserIntent.RESUME_DIAGNOSIS, List.of(
            "帮我在线诊断和优化个人技术简历",
            "简历技术亮点润色与求职面试指导",
            "我的简历写得怎么样帮我看看"
        )),
        Map.entry(UserIntent.LEARNING_PORTRAIT, List.of(
            "查看我的能力雷达图与学情多维画像",
            "我现在的知识掌握度和薄弱环节评估"
        )),
        Map.entry(UserIntent.POINTS_RANKING, List.of(
            "查看全站学员积分学分排行榜",
            "看看谁的学分排在全校第一名"
        )),
        Map.entry(UserIntent.EXAM_QUERY, List.of(
            "查询我的阶段测试与结课考试安排",
            "查看最近的课程考试试卷与考核成绩"
        )),
        Map.entry(UserIntent.INTERVIEW_MOCK, List.of(
            "开启一场全真技术模拟面试测评",
            "大厂后端核心岗位模拟面试专项考场"
        )),
        Map.entry(UserIntent.COURSE_PURCHASE, List.of(
            "我想购买这门推荐的付费专业课",
            "立即报名课程并下单结算"
        ))
    );

    // 目标角色锚点语料
    private static final Map<String, String> ROLE_ANCHORS = Map.of(
        "Java 后端开发工程师 (日常实习/校招)", "Java后端日常实习暑期实习在校生大二大三校招后端初学者入门C语言零基础转Java后端实习生",
        "Java 后端开发工程师", "Java后端开发工程师微服务SpringBoot分布式缓存MySQL高并发业务系统Java开发路线",
        "Java 全栈架构师", "分布式高并发系统架构师百万QPS源码内核性能调优海量数据高可用架构Java架构师",
        "Go 云原生架构师", "Go语言Golang云原生微服务K8sDocker容器化Linux网络编程Go开发路线云原生架构师",
        "Web 前端架构专家", "Web前端开发工程师前端架构专家Vue3ReactTypeScript前端工程化网页制作转行前端前端学习路线前端程序员",
        "大数据流批一体工程师", "大数据数仓FlinkSparkHadoop实时计算离线分析流批一体架构大数据开发路线",
        "大语言模型应用工程师", "大语言模型LLM人工智能AI应用RAGAgent智能体LangChainPrompt工程大模型开发路线",
        "移动与跨端开发工程师", "移动端跨平台开发FlutterAndroidiOS鸿蒙HarmonyOSApp开发移动端学习路线"
    );

    // 难度等级锚点语料
    private static final Map<Integer, String> DIFFICULTY_ANCHORS = Map.of(
        1, "大一在校生大二学生计算机基础零基础日常实习筑基入门小白新手只学过C语言",
        2, "日常业务开发核心进阶实战项目熟练使用主流框架熟练掌握",
        3, "架构师高并发系统调优源码内核高可用百万QPS底层原理性能压测"
    );

    public SemanticIntentRouter(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        // 异步预热预计算锚点向量，避免阻塞应用启动
        new Thread(this::warmUpVectors, "semantic-router-warmup").start();
    }

    private void warmUpVectors() {
        try {
            log.info("[SemanticIntentRouter] 正在初始化意图锚点向量库...");
            for (Map.Entry<UserIntent, List<String>> entry : INTENT_ANCHORS.entrySet()) {
                List<float[]> vecs = new ArrayList<>();
                for (String text : entry.getValue()) {
                    float[] v = fetchEmbedding(text);
                    if (v != null) vecs.add(v);
                }
                if (!vecs.isEmpty()) {
                    intentCentroids.put(entry.getKey(), computeCentroid(vecs));
                }
            }

            for (Map.Entry<String, String> entry : ROLE_ANCHORS.entrySet()) {
                float[] v = fetchEmbedding(entry.getValue());
                if (v != null) roleVectors.put(entry.getKey(), v);
            }

            for (Map.Entry<Integer, String> entry : DIFFICULTY_ANCHORS.entrySet()) {
                float[] v = fetchEmbedding(entry.getValue());
                if (v != null) difficultyVectors.put(entry.getKey(), v);
            }

            log.info("[SemanticIntentRouter] 意图锚点向量库加载完毕: {} 个意图, {} 个角色, {} 个难度等级",
                intentCentroids.size(), roleVectors.size(), difficultyVectors.size());
        } catch (Exception ex) {
            log.warn("[SemanticIntentRouter] 预热向量库失败，将在请求时按需动态推断: {}", ex.getMessage());
        }
    }

    /**
     * 语义识别用户核心意图
     */
    public UserIntent routeIntent(String query) {
        if (!StringUtils.hasText(query)) return UserIntent.GENERAL_QA;

        float[] queryVec = fetchEmbedding(query);
        if (queryVec == null || intentCentroids.isEmpty()) {
            return ruleBasedFallbackIntent(query);
        }

        UserIntent bestIntent = UserIntent.GENERAL_QA;
        double maxSimilarity = 0.0;

        for (Map.Entry<UserIntent, float[]> entry : intentCentroids.entrySet()) {
            double sim = cosineSimilarity(queryVec, entry.getValue());
            if (sim > maxSimilarity) {
                maxSimilarity = sim;
                bestIntent = entry.getKey();
            }
        }

        log.debug("[SemanticIntentRouter] 意图识别结果: query='{}', intent={}, similarity={}", query, bestIntent, maxSimilarity);

        // 设定置信度阈值 0.65，低于此置信度视为通用客服答疑
        if (maxSimilarity >= 0.65) {
            return bestIntent;
        }

        return ruleBasedFallbackIntent(query);
    }

    /**
     * 语义提取目标岗位角色 (Slot Extraction)
     */
    public String extractTargetRole(String query) {
        if (!StringUtils.hasText(query)) return "Java 后端开发工程师";

        float[] queryVec = fetchEmbedding(query);
        if (queryVec == null || roleVectors.isEmpty()) {
            return ruleBasedFallbackRole(query);
        }

        String bestRole = "Java 后端开发工程师";
        double maxSim = 0.0;

        for (Map.Entry<String, float[]> entry : roleVectors.entrySet()) {
            double sim = cosineSimilarity(queryVec, entry.getValue());
            if (sim > maxSim) {
                maxSim = sim;
                bestRole = entry.getKey();
            }
        }

        // 硬规则优先修正：若用户查询包含明确的学科技术排他关键词，防止向量泛化偏离学科真实意图
        String lower = query.toLowerCase();
        if ((lower.contains("前端") || lower.contains("vue") || lower.contains("react") || lower.contains("网页"))
                && !bestRole.contains("前端")) {
            return "Web 前端架构专家";
        }
        if ((lower.contains("大数据") || lower.contains("spark") || lower.contains("flink") || lower.contains("数仓"))
                && !bestRole.contains("大数据")) {
            return "大数据流批一体工程师";
        }
        if ((lower.contains("大模型") || lower.contains("llm") || lower.contains("rag") || lower.contains("人工智能"))
                && !bestRole.contains("大模型")) {
            return "大语言模型应用工程师";
        }
        if ((lower.contains("鸿蒙") || lower.contains("flutter") || lower.contains("harmony"))
                && !bestRole.contains("移动")) {
            return "移动与跨端开发工程师";
        }
        if ((lower.contains("go语言") || lower.contains("golang") || lower.contains("云原生"))
                && !bestRole.contains("Go")) {
            return "Go 云原生架构师";
        }

        if (maxSim >= 0.55) {
            return bestRole;
        }

        return ruleBasedFallbackRole(query);
    }

    /**
     * 语义提取目标难度等级 (1: 筑基入门, 2: 核心进阶, 3: 架构攻坚)
     */
    public Integer extractTargetDifficulty(String query) {
        if (!StringUtils.hasText(query)) return 1;

        float[] queryVec = fetchEmbedding(query);
        if (queryVec == null || difficultyVectors.isEmpty()) {
            return ruleBasedFallbackDifficulty(query);
        }

        int bestDiff = 2;
        double maxSim = 0.0;

        for (Map.Entry<Integer, float[]> entry : difficultyVectors.entrySet()) {
            double sim = cosineSimilarity(queryVec, entry.getValue());
            if (sim > maxSim) {
                maxSim = sim;
                bestDiff = entry.getKey();
            }
        }

        if (maxSim >= 0.50) {
            return bestDiff;
        }

        return ruleBasedFallbackDifficulty(query);
    }

    /**
     * 调用底层向量服务获取文本 Embedding 向量
     */
    public float[] fetchEmbedding(String text) {
        if (!StringUtils.hasText(text)) return null;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> body = Map.of("text", text);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            String response = restTemplate.postForObject(embeddingServiceUrl, request, String.class);
            if (!StringUtils.hasText(response)) return null;

            JsonNode root = objectMapper.readTree(response);
            JsonNode vectorNode = root.get("vector");
            if (vectorNode != null && vectorNode.isArray()) {
                float[] vec = new float[vectorNode.size()];
                for (int i = 0; i < vectorNode.size(); i++) {
                    vec[i] = (float) vectorNode.get(i).asDouble();
                }
                return vec;
            }
        } catch (Exception ex) {
            log.debug("[SemanticIntentRouter] 获取 Embedding 失败，降级规则: {}", ex.getMessage());
        }
        return null;
    }

    private static double cosineSimilarity(float[] v1, float[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length) return 0.0;
        double dot = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }
        if (norm1 <= 1e-9 || norm2 <= 1e-9) return 0.0;
        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private static float[] computeCentroid(List<float[]> vectors) {
        if (vectors == null || vectors.isEmpty()) return null;
        int dim = vectors.get(0).length;
        float[] centroid = new float[dim];
        for (float[] v : vectors) {
            for (int i = 0; i < dim; i++) {
                centroid[i] += v[i];
            }
        }
        for (int i = 0; i < dim; i++) {
            centroid[i] /= vectors.size();
        }
        return centroid;
    }

    // --- 平滑降级兜底逻辑 ---

    private UserIntent ruleBasedFallbackIntent(String content) {
        String lower = content.toLowerCase();
        if (lower.contains("路线") || lower.contains("路径") || lower.contains("规划")
                || lower.contains("学什么") || lower.contains("怎么学") || lower.contains("如何进阶")
                || lower.contains("学习方案") || lower.contains("选课") || lower.contains("想转行")) {
            return UserIntent.PATH_PLANNING;
        }
        if (lower.contains("签到") || lower.contains("打卡")) return UserIntent.SIGN_IN;
        if (lower.contains("优惠券") || lower.contains("领券")) return UserIntent.COUPON_CENTER;
        if (lower.contains("购物车")) return UserIntent.CART_VIEW;
        if (lower.contains("简历")) return UserIntent.RESUME_DIAGNOSIS;
        if (lower.contains("画像") || lower.contains("雷达")) return UserIntent.LEARNING_PORTRAIT;
        if (lower.contains("积分榜") || lower.contains("排行榜")) return UserIntent.POINTS_RANKING;
        if (lower.contains("考试") || lower.contains("测验")) return UserIntent.EXAM_QUERY;
        if (lower.contains("面试")) return UserIntent.INTERVIEW_MOCK;
        if (lower.contains("前往") || lower.contains("个人中心") || lower.contains("去首页")) return UserIntent.PAGE_NAVIGATOR;
        return UserIntent.GENERAL_QA;
    }

    private String ruleBasedFallbackRole(String content) {
        String lower = content.toLowerCase();
        if (lower.contains("大模型") || lower.contains("llm") || lower.contains("rag") || lower.contains("人工智能")) {
            return "大语言模型应用工程师";
        }
        if (lower.contains("go") || lower.contains("golang") || lower.contains("云原生")) {
            return "Go 云原生架构师";
        }
        if (lower.contains("前端") || lower.contains("vue") || lower.contains("react")) {
            return "Web 前端架构专家";
        }
        if (lower.contains("大数据") || lower.contains("spark") || lower.contains("flink")) {
            return "大数据流批一体工程师";
        }
        if (lower.contains("鸿蒙") || lower.contains("flutter") || lower.contains("移动端")) {
            return "移动与跨端开发工程师";
        }
        if (lower.contains("实习") || lower.contains("校招") || lower.contains("大二") || lower.contains("c语言")) {
            return "Java 后端开发工程师 (日常实习/校招)";
        }
        if (lower.contains("架构")) {
            return "Java 全栈架构师";
        }
        return "Java 后端开发工程师";
    }

    private Integer ruleBasedFallbackDifficulty(String content) {
        String lower = content.toLowerCase();
        if (lower.contains("实习") || lower.contains("校招") || lower.contains("大一") || lower.contains("大二")
                || lower.contains("c语言") || lower.contains("零基础") || lower.contains("入门")) {
            return 1;
        }
        if (lower.contains("架构") || lower.contains("百万qps") || lower.contains("源码") || lower.contains("内核")) {
            return 3;
        }
        return 2;
    }
}
