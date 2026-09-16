package com.share.customer.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.customer.config.CustomerAiProperties;
import com.share.customer.domain.CustomerAiConfig;
import com.share.customer.domain.CustomerKnowledge;
import com.share.customer.domain.interview.InterviewSession;
import com.share.customer.domain.interview.InterviewTurn;
import com.share.customer.mapper.CustomerAiConfigMapper;
import com.share.customer.mapper.CustomerKnowledgeMapper;
import com.share.customer.service.CustomerAiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 模拟面试智能出题与三阶段状态机引擎
 */
@Slf4j
@Component
public class InterviewQuestionEngine {

    private static final long CONFIG_ID = 1L;

    public enum JobTrack {
        BACKEND_JAVA,
        SYSTEMS_HIGH_PERF,
        FRONTEND_MOBILE,
        AI_LLM,
        BIG_DATA,
        DATABASE_STORAGE,
        CLOUD_NATIVE_SRE,
        QA_SECURITY
    }

    public record XiaolinQuestionResult(
            String dimension,
            String question,
            String standardReference,
            Long knowledgeId,
            int depthLevel
    ) {}

    public record ProjectDrillResult(
            String dimension,
            String question,
            String standardReference
    ) {}

    private final CustomerKnowledgeMapper knowledgeMapper;
    private final CustomerAiConfigMapper aiConfigMapper;
    private final CustomerAiClient aiClient;
    private final CustomerAiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public InterviewQuestionEngine(
            CustomerKnowledgeMapper knowledgeMapper,
            CustomerAiConfigMapper aiConfigMapper,
            CustomerAiClient aiClient,
            CustomerAiProperties aiProperties,
            ObjectMapper objectMapper) {
        this.knowledgeMapper = knowledgeMapper;
        this.aiConfigMapper = aiConfigMapper;
        this.aiClient = aiClient;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    public static int resolveStage(int turnNum, int totalTurns) {
        if (turnNum <= 1) {
            return 1;
        }
        int stage2End = totalTurns <= 10 ? Math.max(2, totalTurns / 2) : 11;
        if (turnNum <= stage2End) {
            return 2;
        }
        return 3;
    }

    public static String resolveStageName(int stage) {
        return switch (stage) {
            case 1 -> "环节一：自我介绍";
            case 2 -> "环节二：基础八股文";
            case 3 -> "环节三：简历项目追问";
            default -> "综合技术考查";
        };
    }

    public String generateSelfIntroQuestion(InterviewSession session) {
        String stylePersona = getStylePersona(session.getInterviewerStyle());
        StringBuilder sb = new StringBuilder();
        sb.append("你现在是").append(stylePersona).append("。候选人正在面试【").append(session.getTargetJob()).append("】岗位。\n");
        sb.append("当前是面试的第一环节：【自我介绍】。\n");
        if (StringUtils.hasText(session.getResumeSummary())) {
            sb.append("【候选人绑定的个人简历信息】：\n").append(session.getResumeSummary()).append("\n");
        }
        sb.append("请向候选人提出自我介绍要求。\n");
        sb.append("要求：\n");
        sb.append("1. 符合你的面试官风格，专业严谨、真实自然；\n");
        sb.append("2. 明确要求候选人用2~3分钟做一个简要的自我介绍，重点结合其技术栈背景、过往主导的核心项目以及自身核心技术专长进行阐述；\n");
        sb.append("3. 严禁废话客套，直接输出提问正文，120字以内。");

        String fallback = StringUtils.hasText(session.getResumeSummary())
                ? "你好，欢迎参加本次【" + session.getTargetJob() + "】技术面试！我是今天的技术面试官。请你先用2~3分钟做一个简要的自我介绍，可以重点结合你的技术背景、简历中主导的核心业务项目以及你最擅长的技术专长进行阐述。"
                : "你好，欢迎参加本次【" + session.getTargetJob() + "】技术面试！我是今天的技术面试官。请你先用2~3分钟做一个简要的自我介绍，重点介绍一下你的技术栈、过往项目经验以及你对该岗位的核心技术优势。";

        String aiResult = callAi(sb.toString(), fallback);
        return cleanAiText(aiResult);
    }

    public String generateSelfIntroStandardRef(InterviewSession session) {
        String job = StringUtils.hasText(session.getTargetJob()) ? session.getTargetJob() : "高级开发工程师";
        return "【标杆满分自我介绍示范 (STAR+技术矩阵法)】\n"
                + "1. 基本定位与职业背景：面试官您好，非常高兴参加本次【" + job + "】技术面试。我有一线互联网大厂核心业务研发经验，主导负责高并发分布式架构设计与高可用系统治理。\n"
                + "2. 核心技术栈与专长：熟练掌握Java多线程与JMM底层模型、AQS并发编程，深入理解MySQL InnoDB索引、事务隔离与MVCC并发控制机制；在Redis分布式缓存设计、高并发消息中间件（Kafka/RocketMQ）可靠投递以及微服务高可用熔断治理方面有深度生产实战积累。\n"
                + "3. 核心项目与量化产出：曾主导过核心业务系统从0到1架构重构与大促抗峰，针对突发峰值流量与慢SQL瓶颈，通过链路异步化、分库分表与多级缓存优化，将系统支撑QPS提升数倍，P99接口耗时显著下降，保障全链路零故障运行。\n"
                + "4. 总结与期许：期待今天能与您就系统底层机制、核心架构选型以及极端生产排障展开深入交流！";
    }

    public int resolveDrillDepth(int projectIndex, Integer lastScore) {
        if (projectIndex <= 2) {
            return (lastScore != null && lastScore >= 85) ? 2 : 1;
        } else if (projectIndex <= 6) {
            if (lastScore != null && lastScore >= 90) return 3;
            if (lastScore != null && lastScore < 60) return 1;
            return 2;
        } else {
            return 3;
        }
    }

    public XiaolinQuestionResult resolveXiaolinFundamentalQuestion(
            InterviewSession session,
            JobTrack track,
            int fundamentalIndex,
            List<InterviewTurn> existTurns,
            Integer previousScore) {
        String category = resolveXiaolinCategory(track, fundamentalIndex, session.getResumeSummary());
        try {
            Set<Long> usedKnowledgeIds = new HashSet<>();
            Set<String> usedQuestions = new HashSet<>();
            if (existTurns != null) {
                for (InterviewTurn et : existTurns) {
                    if (et.getMatchedKnowledgeId() != null) {
                        usedKnowledgeIds.add(et.getMatchedKnowledgeId());
                    }
                    if (StringUtils.hasText(et.getQuestion())) {
                        usedQuestions.add(et.getQuestion().trim());
                    }
                }
            }

            LambdaQueryWrapper<CustomerKnowledge> wrapper = new LambdaQueryWrapper<CustomerKnowledge>()
                    .likeRight(CustomerKnowledge::getLegacyId, "xl-")
                    .eq(CustomerKnowledge::getCategory, category)
                    .eq(CustomerKnowledge::getStatus, 1);
            if (!usedKnowledgeIds.isEmpty()) {
                wrapper.notIn(CustomerKnowledge::getId, usedKnowledgeIds);
            }
            List<CustomerKnowledge> candidateList = knowledgeMapper.selectList(wrapper);

            if (candidateList != null && !candidateList.isEmpty()) {
                CustomerKnowledge selected = filterXiaolinQuestionByResume(candidateList, session.getResumeSummary(), previousScore, usedQuestions);
                if (selected != null) {
                    String prefix = (fundamentalIndex == 1)
                            ? "很好，对你的背景有了初步了解。接下来我们进入第二环节【核心基础八股与底层技术考查】。首先深入聊聊："
                            : "";
                    String question = prefix + selected.getQuestion();
                    String stdRef = "【小林coding官方高分示范答案】\n" + selected.getAnswer();
                    int depth = (selected.getAnswer() != null && selected.getAnswer().length() > 500) ? 2 : 1;
                    String dimension = "【环节二·" + category + "】" + (selected.getKeywords() != null ? selected.getKeywords() : category);
                    return new XiaolinQuestionResult(safeTruncate(dimension, 64), safeTruncate(question, 1000), stdRef, selected.getId(), depth);
                }
            }
        } catch (Exception ex) {
            log.warn("查询小林coding基础八股题库异常，启用高可用兜底: {}", ex.getMessage());
        }

        String fallbackQ = resolveFundamentalFallback(track, fundamentalIndex);
        if (fundamentalIndex == 1) {
            fallbackQ = "很好，对你的背景有了初步了解。接下来我们进入第二环节【核心基础八股与底层技术考查】。首先：" + fallbackQ;
        }
        String stdRef = resolveFundamentalFallbackRef(track, fundamentalIndex);
        return new XiaolinQuestionResult(safeTruncate("【环节二·" + category + "】核心八股考查", 64), safeTruncate(fallbackQ, 1000), stdRef, null, 2);
    }

    public ProjectDrillResult generateProjectDrillQuestion(
            InterviewSession session,
            JobTrack track,
            int projectIndex,
            int nextDepth,
            InterviewTurn previousTurn,
            Integer previousScore,
            String previousFeedback) {
        String stylePersona = getStylePersona(session.getInterviewerStyle());
        StringBuilder sb = new StringBuilder();
        sb.append("你现在是").append(stylePersona).append("。候选人正在面试【").append(session.getTargetJob()).append("】岗位。\n");
        sb.append("当前进行到第三环节：【简历项目追问】第 ").append(projectIndex).append(" 题，考查深度：L").append(nextDepth).append("。\n\n");

        String resumeSummary = session.getResumeSummary();
        if (StringUtils.hasText(resumeSummary)) {
            String trimmedResume = resumeSummary.length() > 1500 ? resumeSummary.substring(0, 1500) + "..." : resumeSummary;
            sb.append("【候选人绑定的真实简历信息（项目经历/技术栈/薄弱点）】：\n").append(trimmedResume).append("\n\n");
        } else {
            sb.append("【候选人目标岗位核心技术栈】：").append(session.getTargetJob()).append("。请结合千万级高并发真实生产业务场景出题。\n\n");
        }

        if (previousTurn != null && StringUtils.hasText(previousTurn.getUserAnswer())) {
            sb.append("【上一题考查维度与题目】：").append(previousTurn.getDimension()).append(" - ").append(previousTurn.getQuestion()).append("\n");
            sb.append("【上一题候选人真实回答】：").append(previousTurn.getUserAnswer()).append("\n");
            if (previousScore != null) {
                sb.append("【上一题AI考官评审诊断】：得分 ").append(previousScore).append("分；").append(previousFeedback).append("\n");
            }
            sb.append("\n");
        }

        sb.append("【出题与过滤铁律】（必须绝对遵守）：\n");
        sb.append("1. 【必须基于简历】：所有出题必须严格基于候选人简历中真实写明的具体项目经历、系统架构、业务模块或技术栈。直接点名候选人简历中的项目名称或核心组件！\n");
        sb.append("2. 【严格过滤无关考题】：绝对禁止抛出任何与候选人简历不相干的技术、组件或无关业务领域！若简历未提及某技术（如未写过NoSQL/C++/大数据），严禁凭空提问未提及的技术！\n");
        sb.append("3. 【上下文感知与剥洋葱追问】：根据候选人上一轮回答中的技术漏洞、细节或亮点，展开更深一层的追问（L1架构选型与权衡 -> L2底层数据流与性能瓶颈 -> L3生产极限容灾/CPU 100%/故障止血SOP）；\n");
        sb.append("4. 【同时生成标杆示范答案】：必须为本道题生成高水准的大厂架构师级满分示范答题思路，要求条理分明（分点阐述：原理/架构实现/极端故障应对）；\n");
        sb.append("5. 请严格按以下 JSON 格式输出，不要输出任何额外的包裹代码块或多余文字：\n");
        sb.append("{\n");
        sb.append("  \"dimension\": \"项目架构选型与高并发抗峰\",\n");
        sb.append("  \"question\": \"出题正文，150字以内，面试官口吻直接提问\",\n");
        sb.append("  \"standardReference\": \"【标杆示范满分答案 (项目深度实战)】\\n1. 核心原理...\\n2. 架构落地...\\n3. 生产排障...\"\n");
        sb.append("}");

        String prompt = sb.toString();
        String aiResult = callAi(prompt, null);

        if (StringUtils.hasText(aiResult)) {
            try {
                String cleanJson = extractJson(aiResult);
                JsonNode root = objectMapper.readTree(cleanJson);
                String q = root.path("question").asText("");
                String dim = root.path("dimension").asText("核心项目架构深挖");
                String ref = root.path("standardReference").asText("");
                if (StringUtils.hasText(q)) {
                    if (projectIndex == 1) {
                        q = "基础八股考查告一段落。接下来进入第三环节【简历项目与生产实战深度追问】。" + q;
                    }
                    if (!StringUtils.hasText(ref)) {
                        ref = generateDefaultProjectRef(session, dim, q);
                    }
                    return new ProjectDrillResult(safeTruncate(dim, 64), safeTruncate(cleanAiText(q), 1000), ref);
                }
            } catch (Exception ex) {
                log.warn("解析项目深挖出题 JSON 失败，启用启发式保底: {}", ex.getMessage());
            }
        }

        return resolveContextualProjectFallback(session, track, projectIndex, nextDepth);
    }

    public ProjectDrillResult resolveContextualProjectFallback(InterviewSession session, JobTrack track, int projectIndex, int depth) {
        String dimension = resolveProjectDrillDimension(track, projectIndex);
        String question = resolveProjectDrillFallback(session, dimension, track, projectIndex);
        if (projectIndex == 1) {
            question = "基础八股考查告一段落。接下来进入第三环节【简历项目与生产实战深度追问】。" + question;
        }
        String ref = generateDefaultProjectRef(session, dimension, question);
        return new ProjectDrillResult(dimension, question, ref);
    }

    public String resolveCodingProblem(JobTrack track) {
        return switch (track) {
            case SYSTEMS_HIGH_PERF -> "【代码实战题】：请在右侧代码沙箱中实现一个『高并发无锁环形队列 (Lock-Free Ring Buffer) 或基于原子操作的协程池』，需保证并发安全并分析时空复杂度。";
            case FRONTEND_MOBILE -> "【代码实战题】：请在右侧代码沙箱中手写实现一个『深度优先的虚拟 DOM Diff 算法核心逻辑』或『支持并发限制的 Promise 并发调度器』，并分析其时间与空间复杂度。";
            case AI_LLM -> "【代码实战题】：请在右侧代码沙箱中使用 Python 实现一个『自注意力机制 Self-Attention 或滑动窗口 KV-Cache 内存管理核心逻辑』，并推导其计算时空复杂度。";
            case BIG_DATA -> "【代码实战题】：请在右侧代码沙箱中实现一个『Top-K 热度统计（流式滑动窗口与最小堆结合）』或『海量数据布隆过滤器核心哈希映射』，并分析内存占用与复杂度。";
            case DATABASE_STORAGE -> "【代码实战题】：请在右侧代码沙箱中实现一个『LRU / LFU 缓存淘汰算法核心数据结构』或『跳表 SkipList 插入与查找逻辑』，需保证时间复杂度达到 O(1) 或 O(logN)。";
            case CLOUD_NATIVE_SRE -> "【代码实战题】：请在右侧代码沙箱中编写一段核心逻辑：实现一个『生产级熔断器状态机 (Closed/Open/Half-Open)』，支持滑动失败率窗口统计与自愈探活探测。";
            case QA_SECURITY -> "【代码实战题】：请在右侧代码沙箱中实现一个『支持动态 QPS 阶梯爬坡的压测速率控制器 (Rate Limiter)』，需保证微秒级调度精度与线程安全性。";
            default -> "【代码实战题】：请在右侧代码沙箱中实现一个『高并发限流器（支持滑动窗口计数或令牌桶算法）』，需保证多线程安全并分析时空复杂度。";
        };
    }

    public String resolveCodingStandardRef(JobTrack track) {
        return switch (track) {
            case SYSTEMS_HIGH_PERF -> "【标杆示范满分答案 (无锁环形队列 Lock-Free Ring Buffer)】\n"
                    + "```java\n"
                    + "public class LockFreeRingBuffer<T> {\n"
                    + "    private final Object[] buffer;\n"
                    + "    private final int capacity;\n"
                    + "    private final AtomicLong head = new AtomicLong(0);\n"
                    + "    private final AtomicLong tail = new AtomicLong(0);\n"
                    + "    public LockFreeRingBuffer(int capacity) {\n"
                    + "        this.capacity = capacity;\n"
                    + "        this.buffer = new Object[capacity];\n"
                    + "    }\n"
                    + "    public boolean offer(T item) {\n"
                    + "        long currentTail;\n"
                    + "        do {\n"
                    + "            currentTail = tail.get();\n"
                    + "            if (currentTail - head.get() >= capacity) return false;\n"
                    + "        } while (!tail.compareAndSet(currentTail, currentTail + 1));\n"
                    + "        buffer[(int) (currentTail % capacity)] = item;\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "    @SuppressWarnings(\"unchecked\")\n"
                    + "    public T poll() {\n"
                    + "        long currentHead;\n"
                    + "        do {\n"
                    + "            currentHead = head.get();\n"
                    + "            if (currentHead >= tail.get()) return null;\n"
                    + "        } while (!head.compareAndSet(currentHead, currentHead + 1));\n"
                    + "        return (T) buffer[(int) (currentHead % capacity)];\n"
                    + "    }\n"
                    + "}\n"
                    + "```\n"
                    + "【复杂度分析】：入队出队时间复杂度均为 O(1)，无锁CAS保证极高并发吞吐；空间复杂度 O(N)。";
            case FRONTEND_MOBILE -> "【标杆示范满分答案 (Promise并发调度器 Scheduler)】\n"
                    + "```javascript\n"
                    + "class PromiseScheduler {\n"
                    + "  constructor(maxConcurrent = 2) {\n"
                    + "    this.maxConcurrent = maxConcurrent;\n"
                    + "    this.runningCount = 0;\n"
                    + "    this.queue = [];\n"
                    + "  }\n"
                    + "  add(promiseCreator) {\n"
                    + "    return new Promise((resolve, reject) => {\n"
                    + "      this.queue.push({ promiseCreator, resolve, reject });\n"
                    + "      this.runNext();\n"
                    + "    });\n"
                    + "  }\n"
                    + "  runNext() {\n"
                    + "    if (this.runningCount >= this.maxConcurrent || this.queue.length === 0) return;\n"
                    + "    const { promiseCreator, resolve, reject } = this.queue.shift();\n"
                    + "    this.runningCount++;\n"
                    + "    promiseCreator().then(resolve).catch(reject).finally(() => {\n"
                    + "      this.runningCount--;\n"
                    + "      this.runNext();\n"
                    + "    });\n"
                    + "  }\n"
                    + "}\n"
                    + "```\n"
                    + "【复杂度分析】：时间复杂度 O(1) 调度入队出队，空间复杂度 O(N) 队列排队任务数。";
            default -> "【标杆示范满分答案 (高并发滑动窗口限流器 SlidingWindowRateLimiter)】\n"
                    + "```java\n"
                    + "public class SlidingWindowRateLimiter {\n"
                    + "    private final int limit;\n"
                    + "    private final long windowSizeMs;\n"
                    + "    private final ConcurrentLinkedQueue<Long> timestamps = new ConcurrentLinkedQueue<>();\n"
                    + "    private final Object lock = new Object();\n"
                    + "    public SlidingWindowRateLimiter(int limit, long windowSizeMs) {\n"
                    + "        this.limit = limit;\n"
                    + "        this.windowSizeMs = windowSizeMs;\n"
                    + "    }\n"
                    + "    public boolean tryAcquire() {\n"
                    + "        long now = System.currentTimeMillis();\n"
                    + "        long boundary = now - windowSizeMs;\n"
                    + "        synchronized (lock) {\n"
                    + "            while (!timestamps.isEmpty() && timestamps.peek() <= boundary) {\n"
                    + "                timestamps.poll();\n"
                    + "            }\n"
                    + "            if (timestamps.size() < limit) {\n"
                    + "                timestamps.offer(now);\n"
                    + "                return true;\n"
                    + "            }\n"
                    + "            return false;\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n"
                    + "```\n"
                    + "【复杂度分析】：平摊时间复杂度 O(1)，空间复杂度 O(Limit) 存储窗口内时间戳，完美解决固定窗口临界突刺问题。";
        };
    }

    public JobTrack detectJobTrack(String job) {
        if (!StringUtils.hasText(job)) {
            return JobTrack.BACKEND_JAVA;
        }
        String s = job.toLowerCase();
        if (s.contains("大模型") || s.contains("llm") || s.contains("rag") || s.contains("agent") || s.contains("nlp") || s.contains("算法") || s.contains("视觉") || s.contains("cv") || s.contains("深度学习") || s.contains("推荐系统")) {
            return JobTrack.AI_LLM;
        }
        if (s.contains("大数据") || s.contains("spark") || s.contains("hadoop") || s.contains("flink") || s.contains("数据仓库") || s.contains("湖仓一体")) {
            return JobTrack.BIG_DATA;
        }
        if (s.contains("mysql") || s.contains("dba") || s.contains("数据库") || s.contains("分布式存储") || s.contains("消息中间件") || s.contains("rocketmq")) {
            return JobTrack.DATABASE_STORAGE;
        }
        if (s.contains("kubernetes") || s.contains("k8s") || s.contains("云原生") || s.contains("devops") || s.contains("sre") || s.contains("稳定性")) {
            return JobTrack.CLOUD_NATIVE_SRE;
        }
        if (s.contains("前端") || s.contains("vue") || s.contains("react") || s.contains("全栈") || s.contains("ios") || s.contains("android")) {
            return JobTrack.FRONTEND_MOBILE;
        }
        if (s.contains("测试") || s.contains("sdet") || s.contains("压测") || s.contains("安全") || s.contains("渗透")) {
            return JobTrack.QA_SECURITY;
        }
        if (s.contains("go") || s.contains("c++") || s.contains("rust") || s.contains("python") || s.contains("底层系统")) {
            return JobTrack.SYSTEMS_HIGH_PERF;
        }
        return JobTrack.BACKEND_JAVA;
    }

    public String getStylePersona(String style) {
        if ("bytedance_tech".equalsIgnoreCase(style)) {
            return "【字节跳动二面技术专家】风格，极其务实、追求极致工程效率与细节边界，强调手撕算法、并发模型与数据结构底层细节";
        }
        if ("gentle_hr".equalsIgnoreCase(style)) {
            return "【资深大厂HRBP】风格，考察自驱力、商业意识、高压沟通协作与冲突化解，深挖过往项目的技术决策与成长复盘";
        }
        if ("standard".equalsIgnoreCase(style)) {
            return "【大厂技术评审委员会】风格，客观标准、全面考察计算机基础、设计模式与架构权衡";
        }
        return "【阿里巴巴P7+资深技术架构师】风格，极其犀利严苛、直击 JVM/OS 汇编底层、高并发线上真实踩坑与千万级流量极限排障";
    }

    public String cleanAiText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String s = text.replaceAll("^[\"'“‘]|[\"'”’]$", "").trim();
        s = s.replaceAll("^(面试官|考官|Q|提问|问题)[:：]\\s*", "").trim();
        return s.replaceAll("^[\"'“‘]|[\"'”’]$", "").trim();
    }

    public static String safeTruncate(String str, int maxLen) {
        if (str == null) {
            return "";
        }
        String t = str.trim();
        return t.length() > maxLen ? t.substring(0, maxLen) : t;
    }

    private String resolveXiaolinCategory(JobTrack track, int fundamentalIndex, String resumeSummary) {
        int idx = ((Math.max(1, fundamentalIndex) - 1) % 10) + 1;
        String lowerResume = StringUtils.hasText(resumeSummary) ? resumeSummary.toLowerCase() : "";
        boolean isCpp = lowerResume.contains("c++") || track == JobTrack.SYSTEMS_HIGH_PERF;
        boolean isGo = lowerResume.contains("go") || lowerResume.contains("golang");
        boolean isQa = track == JobTrack.QA_SECURITY || lowerResume.contains("测试");

        if (isCpp) {
            return switch (idx) {
                case 1 -> "计算机网络与协议";
                case 2 -> "Linux操作系统与运维";
                case 3 -> "C++底层高性能";
                case 4 -> "数据结构与算法";
                case 5 -> "MySQL与数据存储";
                case 6 -> "Redis与高性能缓存";
                case 7 -> "架构与设计模式";
                case 8 -> "软件工程与架构规范";
                case 9 -> "消息队列与中间件";
                default -> "JVM虚拟机与调优";
            };
        } else if (isGo) {
            return switch (idx) {
                case 1 -> "计算机网络与协议";
                case 2 -> "Linux操作系统与运维";
                case 3 -> "Go语言与微服务";
                case 4 -> "云原生与K8s";
                case 5 -> "MySQL与数据存储";
                case 6 -> "Redis与高性能缓存";
                case 7 -> "数据结构与算法";
                case 8 -> "消息队列与中间件";
                case 9 -> "架构与设计模式";
                default -> "软件工程与架构规范";
            };
        } else if (isQa) {
            return switch (idx) {
                case 1 -> "自动化测试与质量";
                case 2 -> "计算机网络与协议";
                case 3 -> "Linux操作系统与运维";
                case 4 -> "MySQL与数据存储";
                case 5 -> "Redis与高性能缓存";
                case 6 -> "Java核心与并发";
                case 7 -> "软件工程与架构规范";
                case 8 -> "消息队列与中间件";
                case 9 -> "数据结构与算法";
                default -> "架构与设计模式";
            };
        } else {
            return switch (idx) {
                case 1 -> "计算机网络与协议";
                case 2 -> "Linux操作系统与运维";
                case 3 -> "MySQL与数据存储";
                case 4 -> "Redis与高性能缓存";
                case 5 -> "Java核心与并发";
                case 6 -> "JVM虚拟机与调优";
                case 7 -> "Spring源码机制";
                case 8 -> "消息队列与中间件";
                case 9 -> "数据结构与算法";
                default -> "架构与设计模式";
            };
        }
    }

    private CustomerKnowledge filterXiaolinQuestionByResume(List<CustomerKnowledge> list, String resumeSummary, Integer previousScore, Set<String> usedQuestions) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        String lowerResume = StringUtils.hasText(resumeSummary) ? resumeSummary.toLowerCase() : "";
        boolean requireDeep = previousScore != null && previousScore >= 80;

        CustomerKnowledge best = null;
        int bestScore = -1;

        for (CustomerKnowledge item : list) {
            String q = item.getQuestion() != null ? item.getQuestion().trim() : "";
            if (usedQuestions != null && usedQuestions.contains(q)) {
                continue;
            }
            int score = 0;
            String qLower = q.toLowerCase();
            String kw = item.getKeywords() != null ? item.getKeywords().toLowerCase() : "";

            if (StringUtils.hasText(lowerResume)) {
                String[] techTokens = {"mysql", "redis", "kafka", "rocketmq", "rabbitmq", "spring", "jvm", "mybatis",
                        "dubbo", "netty", "docker", "k8s", "linux", "分库分表", "分布式锁", "缓存", "索引", "线程池", "aqs",
                        "gc", "三次握手", "四次挥手", "tcp", "http", "mvcc", "跳表", "微服务"};
                for (String token : techTokens) {
                    if (lowerResume.contains(token) && (qLower.contains(token) || kw.contains(token))) {
                        score += 15;
                    }
                }
            }

            boolean isHard = qLower.contains("原理") || qLower.contains("底层") || qLower.contains("源码") || qLower.contains("调优") || qLower.contains("排查") || qLower.contains("机制");
            if (requireDeep && isHard) {
                score += 10;
            } else if (!requireDeep && !isHard) {
                score += 5;
            }

            if (q.length() >= 8 && q.length() <= 45) {
                score += 5;
            }

            if (score > bestScore) {
                bestScore = score;
                best = item;
            }
        }
        return (best != null) ? best : list.get(0);
    }

    private String resolveFundamentalFallback(JobTrack track, int index) {
        return switch (track) {
            case SYSTEMS_HIGH_PERF -> switch (index) {
                case 1 -> "请深入阐述 Linux 虚拟内存管理中，Page Cache 缓存页与直接 I/O (Direct I/O) 的核心区别与性能权衡？";
                case 2 -> "在多核 CPU 高并发编程中，什么是伪共享（False Sharing）？底层硬件层面是如何通过 Cache Line 对齐进行消除的？";
                case 3 -> "在高性能长连接服务中，Epoll 的水平触发（LT）与边缘触发（ET）在底层就绪通知机制上有何本质差异？";
                case 4 -> "深入拆解协程调度器（如 Go GMP 模型或 C++20 协程）。当发生密集系统调用阻塞时，调度器是如何进行 M 与 P 的解绑让渡的？";
                case 5 -> "对比标准 glibc ptmalloc，Jemalloc 与 TCMalloc 是如何通过 Thread Cache 与 Arena 机制避免多线程内存分配锁竞争的？";
                case 6 -> "请剖析 Linux 零拷贝技术中 mmap 与 sendfile 的系统调用全流程，它们各自减少了几次上下文切换与 CPU 数据拷贝？";
                case 7 -> "在 Raft 分布式共识算法中，Leader 选举机制是如何避免脑裂的？日志复制出现不一致时 Leader 如何强制 Follower 对齐？";
                case 8 -> "设计超低延迟 RPC 协议时，序列化格式（Protobuf/FlatBuffers）在内存反序列化开销与数据包体积上有何深度取舍？";
                case 9 -> "请阐述 C++ 内存模型中的 std::memory_order_relaxed、acquire 与 release 在汇编层面是如何对应 CPU 内存屏障指令的？";
                default -> "当线上高吞吐服务出现微秒级长尾延迟（P999）抖动时，你通常如何使用 Perf、eBPF 或火焰图进行无侵入内核级定位？";
            };
            case FRONTEND_MOBILE -> switch (index) {
                case 1 -> "请从浏览器主线程事件循环出发，详细拆解宏任务（MacroTask）、微任务（MicroTask）与 requestAnimationFrame 渲染帧的精确执行时机。";
                case 2 -> "请详细剖析浏览器渲染引擎从解析 HTML 到像素绘制上屏的关键渲染路径。在何种场景下会触发重排（Reflow）与重绘（Repaint），如何利用合成层提升性能？";
                case 3 -> "请深度对比 Vue3 基于 ES6 Proxy 的响应式依赖收集系统与 React Fiber 架构基于单向链表的时间切片（Time Slicing）机制。";
                case 4 -> "请深入阐述 Vue3 快速 Diff 算法（最长递增子序列）与 Vue2 双端 Diff 算法的核心区别？为什么在列表渲染中绝对不能使用 index 作为 Key？";
                case 5 -> "在现代大型前端工程化构建中，Vite 基于 ESBuild 的依赖预构建与 Rollup 的生产 Tree-shaking 在 AST 语法树标记上有何底层工作原理？";
                case 6 -> "针对 Google 核心性能指标 Core Web Vitals（LCP、INP、CLS），在面对复杂 SPA 应用时，你的全链路诊断手段与前置资源优化手段是什么？";
                case 7 -> "请深度解析 HTTP/2 的多路复用与头部压缩（HPACK）原理。相比 HTTP/2，HTTP/3 为什么转而基于 QUIC（UDP）构建？";
                case 8 -> "在大型 Web 应用中，存储在 Cookie 中的敏感凭据如何防御 XSS 窃取与 CSRF 伪造攻击？CORS 预检请求（OPTIONS）触发的精确条件是什么？";
                case 9 -> "在 Hybrid 跨端混合开发中，JSBridge 的通信原理（URL Schema 拦截与注入 API）是什么？离线缓存与资源版本更新是如何做到秒级生效的？";
                default -> "在微前端架构（如基于 qiankun / Module Federation）设计中，主子应用之间是如何实现 JS 全局沙箱隔离与 CSS 样式污染隔离的？";
            };
            case AI_LLM -> switch (index) {
                case 1 -> "在 Transformer 架构中，为什么 Multi-Head Attention 需要进行缩放点积（Scaling by sqrt(d_k)）？位置编码 RoPE（旋转位置编码）的数学本质是什么？";
                case 2 -> "为什么当前主流大语言模型（如 LLaMA / GPT-4）均采用 Decoder-Only 架构？在自回归生成中，KV-Cache 的显存占用计算公式是什么？";
                case 3 -> "在大模型微调技术中，LoRA 是如何在冻结预训练权重的前提下，通过低秩矩阵分解 A 和 B 实现高效参数微调的？其前向传播计算公式是什么？";
                case 4 -> "对比 RLHF（基于人类反馈的强化学习）中的 PPO 算法与 DPO（直接偏好优化），DPO 是如何通过隐式奖励函数推导摆脱独立 Reward Model 训练的？";
                case 5 -> "在企业级 RAG 检索增强架构中，向量数据库（如 Qdrant / Milvus）的 HNSW 图索引算法是如何平衡召回率与检索时延的？Chunk 切分如何避免语义截断？";
                case 6 -> "在 RAG 系统的高阶重排阶段，为什么需要将稠密向量检索初筛结果输入给 Cross-Encoder 重排模型进行二次打分？它的时延与精度权衡是怎样的？";
                case 7 -> "请深入拆解 AI Agent 的核心决策循环（ReAct 范式：Thought -> Action -> Observation）。当 Agent 出现多步规划死循环或工具调用参数幻觉时，如何设计反思回路自纠错？";
                case 8 -> "在 vLLM 推理加速引擎中，PagedAttention 技术是如何借鉴操作系统虚拟内存分页机制彻底消除显存内部与外部碎片浪费的？";
                case 9 -> "当给大模型喂入 128k 超长上下文时，注意力机制的显存复杂度是如何通过 FlashAttention 的分块计算（Tiling）与重计算策略降至线性复杂度的？";
                default -> "如何构建严密的大模型自动化评测 Benchmark 体系？在面对 Prompt 注入攻防与越狱对抗时，企业级应用网关应如何设计多层防御防线？";
            };
            case BIG_DATA -> switch (index) {
                case 1 -> "在 Apache Flink 状态一致性管理中，基于 Chandy-Lamport 算法的 Checkpoint Barrier 是如何在流数据中穿插流转并触发异步快照的？";
                case 2 -> "请详细拆解 Flink 与外部存储（如 Kafka、MySQL）对接时，两阶段提交协议（2PC）是如何协同保障端到端 Exactly-Once 精确一致性语义的？";
                case 3 -> "在 Spark 计算引擎中，窄依赖（Narrow Dependency）与宽依赖（Shuffle Dependency）是如何划分 Stage 的？Spark 内存管理机制中 Execution 内存与 Storage 内存是如何动态借用的？";
                case 4 -> "在处理千万级海量数据倾斜（Data Skew）时，由于热点 Key 导致单个 Task 耗时过长或 OOM，你常用的全链路调优方案（如随机加盐两阶段聚合、广播 Join）有哪些？";
                case 5 -> "对比传统 Hive 数仓，新一代数据湖仓一体存储格式（Apache Iceberg / Hudi）是如何基于元数据快照树实现 ACID 事务、行级更新与时间旅行（Time Travel）的？";
                case 6 -> "在分布式消息队列 Kafka 中，ISR（In-Sync Replicas）副本集合是如何动态维护的？Kafka 的零拷贝技术与 PageCache 是如何保证单机数十万 QPS 极速写入的？";
                case 7 -> "在企业级数仓体系中，从 ODS、DWD、DWS 到 ADS 的分层规范是怎样的？在维度建模中，缓慢变化维（SCD Type 1/2/3）在业务场景中是如何落地的？";
                case 8 -> "在实时分析型数据库 ClickHouse 中，MergeTree 存储引擎的稀疏索引与数据按列压缩是怎样设计的？为什么向量化执行引擎（SIMD）能大幅超越传统行存查询性能？";
                case 9 -> "在大型数据中台建设中，如何基于 AST 解析 SQL 自动化构建全链路字段级数据血缘关系？当下游业务指标异常时，血缘图谱如何辅助快速根因定位？";
                default -> "当 Flink 线上作业出现严重的反压（Backpressure）告警时，你的全链路排查排障步骤是什么？网络缓冲区（Network Buffer）与下游算子瓶颈如何针对性调优？";
            };
            case DATABASE_STORAGE -> switch (index) {
                case 1 -> "请深入剖析 MySQL InnoDB 引擎的 Compact / Dynamic 行记录格式。变长字段列表与 NULL 值列表在物理磁盘上是如何排列的？单行数据超过页大小时如何处理溢出页？";
                case 2 -> "请从底层读写模型深入对比 B+Tree 与 LSM-Tree（Log-Structured Merge-Tree）的性能优劣。为什么 LSM-Tree 更适合写入密集型业务场景，其写放大与读放大如何治理？";
                case 3 -> "在 MySQL 事务执行过程中，Redo Log 的 WAL（Write-Ahead Logging）机制与两阶段提交（prepare 与 commit 状态）是如何与 Binlog 协同保障数据不丢与主从一致性的？";
                case 4 -> "请深入拆解 MySQL InnoDB 在可重复读（RR）隔离级别下，MVCC 机制的 ReadView 构建时机。当前读（Current Read）与快照读（Snapshot Read）有何本质区别？";
                case 5 -> "如果一条包含多表关联且数据量过千万的复杂 SQL 执行缓慢，在 Explain 执行计划中，你重点关注哪些列（type, key_len, ref, rows, Extra）？如何消除 Using filesort 与 Using temporary？";
                case 6 -> "在海量数据水平分库分表实战中，如何根据业务主键与分片键（Sharding Key）设计路由算法？在面对跨分片聚合（GROUP BY）与全局分页查询时，架构上如何优雅解决？";
                case 7 -> "请深度对比分布式事务 2PC、TCC 与本地消息表事务最终一致性方案。在电商下单扣库存、交易支付等金融级场景中，你的技术选型依据是什么？";
                case 8 -> "MySQL 高可用主从架构中，异步复制与半同步复制（Semi-Sync Replication）在数据安全性上有何差异？如何彻底解决主从同步延迟导致读写分离读取脏数据的问题？";
                case 9 -> "在 MHA 或 Orchestrator 高可用集群故障转移时，如何防止网络分区引发的“双主脑裂”？虚拟 IP（VIP）漂移与元数据仲裁机制是如何协同规避故障的？";
                default -> "面对核心交易库冷数据迅速膨胀导致存储成本飙升的痛点，如何设计在线不停机、业务无感的冷热数据归档方案？历史海量归档数据如何支撑低频快速检索？";
            };
            case CLOUD_NATIVE_SRE -> switch (index) {
                case 1 -> "请从 Linux 内核底层剖析容器的本质。Namespace（UTS, IPC, PID, Mount, Network, User）与 Cgroups 是如何协同实现进程间视角隔离与 CPU/内存资源硬性配额限制的？";
                case 2 -> "在 Kubernetes 容器网络模型中，Flannel（VXLAN / Host-GW）与 Calico（BGP / IPIP）在数据包跨节点封装与路由转发上有何本质性能差异？";
                case 3 -> "请详细拆解从客户端执行 kubectl apply 到 Pod 最终在 Worker 节点上被拉起，APIServer、Etcd、Controller Manager、Scheduler 与 Kubelet 之间的全链路通信与状态同步机制。";
                case 4 -> "在 Kubernetes 调度器中，PreFilter、Filter、Score 等调度插件扩展点是如何评估 Pod 最佳放置节点的？自定义 Operator 控制器的 Informer 缓存与工作队列是如何保障最终一致性的？";
                case 5 -> "在 Istio 服务网格中，Envoy Sidecar 是如何通过 iptables 规则透明拦截容器入站与出站网络流量的？控制面 Istiod 与数据面 Envoy 的动态配置下发（xDS 协议）机制是怎样的？";
                case 6 -> "在构建企业级可观测性平台时，OpenTelemetry 是如何标准化聚合 Metrics、Traces 与 Logs 的？当面对每秒数百万次链路追踪时，如何设计自适应尾采样策略降低存储开销？";
                case 7 -> "在生产环境中，Horizontal Pod Autoscaler (HPA) 基于 CPU 使用率扩缩容可能存在滞后抖动。如何结合自定义业务指标（如 QPS / 消息队列堆积深度）实现更灵敏的预见性弹性伸缩？";
                case 8 -> "在设计跨可用区或异地多活业务系统时，如果发生跨机房专线光纤中断，DNS 流量调度与数据库多活同步机制如何做到分钟级流量无损切换？";
                case 9 -> "在生产环境开展混沌工程（Chaos Engineering）故障注入演练时，如何界定爆炸半径（Blast Radius）？若注入网络延迟故障引发连锁级联反应，如何设计秒级一键止血回滚机制？";
                default -> "作为稳定性负责人，如何科学制定核心业务的 SLA/SLO 指标与错误预算（Error Budget）？当线上发生 P0 级重大事故时，标准的一线排障止血 SOP 与复盘流程是什么？";
            };
            case QA_SECURITY -> switch (index) {
                case 1 -> "在主导千万级全链路压测时，如何设计流量标记（如 HTTP Header 注入压测标）？压测标在跨微服务 RPC 调用与异步消息队列传递过程中如何做到零丢失透传？";
                case 2 -> "在生产环境进行全链路压测时，压测流量落入影子数据库（Shadow DB）的动态数据源路由规则是如何实现的？如何绝对保证影子数据与真实生产数据严格隔离？";
                case 3 -> "在现代 CI/CD 质量门禁体系中，如何将单元测试覆盖率（JaCoCo）、静态代码异味扫描（SonarQube）与自动化接口回归测试联动？质量红线不达标时如何阻断发布？";
                case 4 -> "线上真实流量录制与回放技术（如 基于 JVM-Sandbox 或 Go-Replay）在大型微服务重构测试中，是如何解决外部写接口调用副作用（如支付、发短信）的 Mock 与校验的？";
                case 5 -> "请深入阐述针对 OWASP Top 10 中 SQL 注入、SSRF（服务端请求伪造）与命令执行漏洞的底层挖掘思路。在代码审计中，黑名单过滤与白名单预编译参数化有何防御效果差异？";
                case 6 -> "在开放 API 接口安全防护中，基于公私钥签名的防篡改机制（timestamp + nonce + sign）是如何有效防御重放攻击与中间人劫持的？";
                case 7 -> "在核心秒杀或优惠券领取接口，面对黑灰产薅羊毛与机器爬虫刷单，如何基于滑动时间窗口、令牌桶算法与设备指纹实现精准的风控限流与拦截？";
                case 8 -> "面对数据安全法与合规审计，核心业务中用户的身份证、手机号、银行卡等敏感字段在落盘加密（如国密 SM4）与日志打印动态脱敏上，系统架构是如何分层落地的？";
                case 9 -> "混沌测试与传统自动化测试有何本质区别？在面对依赖服务不可用、网络丢包抖动、磁盘读写夯死等极限场景时，如何通过混沌演练验证系统的自愈与容错弹性？";
                default -> "当发生重大线上生产 Bug 造成资损告警时，你如何带领测试开发团队进行事故根因定位（5 Whys 追问法），并在全链路流程上建立防再发生的长效防御机制？";
            };
            default -> switch (index) {
                case 1 -> "请深入阐述 Java 中 volatile 关键字在 JMM 内存模型中的实现原理？它是如何通过内存屏障禁止指令重排并保障可见性的？";
                case 2 -> "请剖析 AQS（AbstractQueuedSynchronizer）的核心工作机制。以 ReentrantLock 为例，底层是如何基于 CAS 与双向同步队列实现公平锁与非公平锁的？";
                case 3 -> "在生产高并发场景下，ThreadPoolExecutor 的核心参数（corePoolSize, maximumPoolSize, workQueue, Handler）是如何协同运转的？遇到流量突增时哪种阻塞队列选型最合理？";
                case 4 -> "请详细拆解 JVM 类加载的双亲委派机制。在何种工业级场景下需要破坏双亲委派机制（例如 Tomcat / JDBC / 热部署）？如何实现自定义类加载器？";
                case 5 -> "请深度对比 JVM G1 垃圾收集器与 ZGC 的核心区别。在你的生产排障经历中，什么场景下会触发 Full GC，线上排查定位工具链有哪些？";
                case 6 -> "请深入剖析 MySQL InnoDB 引擎中，聚簇索引与二级索引的 B+Tree 结构差异。为什么复合索引必须遵循最左前缀匹配原则？索引下推（ICP）机制是如何优化查询的？";
                case 7 -> "MySQL InnoDB 是如何基于 UndoLog 和 ReadView 实现多版本并发控制（MVCC）的？在可重复读（RR）隔离级别下，间隙锁（Gap Lock）是如何协同防止幻读的？";
                case 8 -> "请深入剖析 Redis 中跳表（SkipList）与压缩列表（ziplist/quicklist）的底层数据结构。为什么 ZSet 在元素较多时选择跳表而不是红黑树或平衡二叉树？";
                case 9 -> "在千万级高并发系统中，如何彻底解决 Redis 缓存穿透、击穿与雪崩？高并发更新时，Cache-Aside 模式下如何保障 Redis 缓存与数据库的双写最终一致性？";
                default -> "请深度阐述 Netty 高性能网络编程的 Reactor 线程模型与零拷贝机制。在微服务架构中，Spring 是如何基于三级缓存机制解决单例 Bean 的属性循环依赖的？";
            };
        };
    }

    private String resolveFundamentalFallbackRef(JobTrack track, int index) {
        return "【标杆示范满分答题思路】\n"
                + "1. 核心定义与设计意图：准确给出该机制的核心概念与为什么要在系统/内核中如此设计；\n"
                + "2. 底层工作机理与时空权衡：深入拆解其内存布局、关键数据结构或源码调用链（如锁竞争、缓存行、刷盘时机）；\n"
                + "3. 生产落地与高频排障实战：结合线上千万级并发场景，说明常见故障陷阱及成熟的调优/避坑方案。";
    }

    private String resolveProjectDrillDimension(JobTrack track, int index) {
        return switch (index) {
            case 1 -> "核心项目全链路架构设计与技术选型权衡";
            case 2 -> "业务大促高并发抗峰与核心链路容量评估";
            case 3 -> "分布式数据一致性保证与复杂事务兜底方案";
            case 4 -> "复杂业务演进中的难点攻坚与关键决策反思";
            case 5 -> "生产环境慢SQL与海量存储分库分表调优";
            case 6 -> "线上重大故障排查实战(CPU 100%/OOM内存泄露)";
            case 7 -> "高可用容灾、全链路熔断限流与服务雪崩防御";
            case 8 -> "系统性能极致优化(RT降低/吞吐量数倍提升实战)";
            default -> "压轴终局复杂系统架构设计与演进思考";
        };
    }

    private String generateDefaultProjectRef(InterviewSession session, String dimension, String question) {
        return "【标杆示范满分答案 (大厂架构师深度复盘)】\n"
                + "1. 架构选型与核心考量：针对" + dimension + "，首先明确业务场景与吞吐量量级，分析单点瓶颈与CAP权衡，选定最匹配的技术方案。\n"
                + "2. 底层机制与数据流转：深入到组件底层机制（如索引树分裂、内存CacheLine对齐、批量刷盘或零拷贝传输），确保链路在高并发下低开销低抖动。\n"
                + "3. 极端生产场景与止血SOP：制定完备的降级熔断防线，针对网络抖动、慢查询及CPU飙高，通过指标打标、动态限流与灰度分流实现毫秒级止血。";
    }

    private String resolveProjectDrillFallback(InterviewSession session, String dimension, JobTrack track, int index) {
        boolean hasResume = StringUtils.hasText(session.getResumeSummary());
        return switch (index) {
            case 1 -> hasResume
                    ? "请结合你简历中最具代表性的核心项目，深入阐述该系统的全链路架构设计。在核心链路的技术选型中，你权衡了哪些备选方案，为什么最终选定当前架构？"
                    : "请结合你主导过的核心业务系统，详细剖析其全链路架构设计与关键模块划分。面对高并发高可用诉求，你们在技术选型上有何得失权衡？";
            case 2 -> hasResume
                    ? "在你的简历项目中，面对大促活动或极端高并发峰值流量冲击时，你们是如何做系统容量评估与压测摸高的？核心链路采取了哪些限流、降级与熔断保障？"
                    : "在面对数万至数十万 QPS 的大促高峰流量时，核心微服务链路的容量评估是如何计算的？当并发流量超出承载极限时，你们采取了哪些多级限流与优雅降级手段？";
            case 3 -> "在跨微服务或者跨数据源的复杂分布式业务链路中，你们是如何保障数据最终一致性的？若外部异步消息丢失或下游调用超时，系统具备怎样的自动对账与幂等补偿兜底机制？";
            case 4 -> "在项目的持续架构演进与复杂业务迭代过程中，你遇到过的最大技术难点是什么？当时的技术瓶颈在哪里，你最终是如何攻坚并拿到量化业务结果的？";
            case 5 -> "若线上核心业务表数据量突破千万且单条 SQL 查询耗时出现毛刺，你的全链路诊断调优步骤是什么？如何设计并实施不停机平滑分库分表迁移与数据校验？";
            case 6 -> "如果生产环境突然触发监控告警：CPU 飙升至 100% 或微服务实例突发内存泄露频繁 Full GC，请详细阐述你在生产环境上的完整排查、定位、止血与根治步骤。";
            case 7 -> "面对上游依赖服务大面积超时或网络抖动引发的雪崩隐患，你们是如何设计系统高可用与容灾防线的？Sentinel/Hystrix 熔断降级阈值是如何科学配置的？";
            case 8 -> "请分享一次你在实际项目中主导的系统性能极致优化实操：优化前的系统吞吐量与响应时间（RT）指标是多少，通过哪些具体的架构与代码级手段实现了数倍的性能飞跃？";
            default -> "面对业务未来 3~5 年可能出现的技术瓶颈与业务形态变更，如果由你主导重构，你会从哪些维度重新规划该系统的底层架构演进路线？";
        };
    }

    private String callAi(String prompt, String fallback) {
        try {
            CustomerAiConfig config = effectiveAiConfig();
            String reply = aiClient.askRaw(config, "你是一位顶尖大厂技术专家兼资深面试官。", prompt);
            if (StringUtils.hasText(reply)) {
                return reply.trim();
            }
        } catch (Exception ex) {
            log.warn("AI 接口调用异常: {}", ex.getMessage());
        }
        return fallback;
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
        String s = text.trim();
        if (s.startsWith("```")) {
            int firstLine = s.indexOf('\n');
            if (firstLine != -1) {
                s = s.substring(firstLine + 1);
            }
            int lastFence = s.lastIndexOf("```");
            if (lastFence != -1) {
                s = s.substring(0, lastFence);
            }
            s = s.trim();
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return s.trim();
    }
}
