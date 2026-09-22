package com.share.education.ai.tools.market;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;

/**
 * 产业前沿岗位招聘行情与技能需求雷达工具 (JobMarketRadarTool)。
 * 为推荐智能体和技术总监辩论节点提供实时大厂岗位需求、核心框架热度与薪资指数。
 */
@Component("jobMarketRadarTool")
public class JobMarketRadarTool implements Function<JobMarketQueryRequest, JobMarketTrackInfo> {

    private static final Logger log = LoggerFactory.getLogger(JobMarketRadarTool.class);

    private static final Map<String, JobMarketTrackInfo> TRACK_DATABASE = new LinkedHashMap<>();

    static {
        TRACK_DATABASE.put("LLM", new JobMarketTrackInfo(
                "TRACK_LLM", "大语言模型应用工程师", 98, "爆发增长 (环比+42%)", "28k - 55k",
                List.of("LangChain", "Spring AI", "Qdrant", "RAG架构", "Prompt Engineering", "Actor-Critic", "LoRA微调"),
                List.of("向量相似度混合检索召回", "多智能体状态图编排", "复杂业务反思闭环", "模型评测Eval体系"),
                "创造与架构级综合", "生产级知识库问答与自治Agent系统"
        ));
        TRACK_DATABASE.put("JAVA", new JobMarketTrackInfo(
                "TRACK_JAVA", "Java高性能后端架构师", 92, "稳步上升", "25k - 45k",
                List.of("Spring Boot 3", "Spring Cloud Alibaba", "RocketMQ 5", "Netty", "Redis分布式锁", "MySQL内核与分库分表"),
                List.of("千万级秒杀高并发削峰", "分布式事务Seata/TCC", "JVM性能故障诊断调优", "DDD领域驱动设计"),
                "分析与评价", "亿级流量订单中台与实时风控系统"
        ));
        TRACK_DATABASE.put("DEVOPS", new JobMarketTrackInfo(
                "TRACK_DEVOPS", "云原生 & DevOps平台架构师", 88, "持续走高", "22k - 40k",
                List.of("Kubernetes", "Docker", "Istio服务网格", "CI/CD流水线", "Prometheus", "Terraform"),
                List.of("K8s容器化生产编排", "微服务可观测性全链路监控", "容灾备份与多活架构", "GitOps自动化交付"),
                "应用与分析", "企业级混合多云容器持续交付平台"
        ));
        TRACK_DATABASE.put("FRONTEND", new JobMarketTrackInfo(
                "TRACK_FRONTEND", "前端全栈工程专家", 85, "平稳健康", "20k - 38k",
                List.of("Vue 3", "TypeScript", "Vite", "Pinia", "Node.js", "WebAssembly", "Tailwind CSS"),
                List.of("前端工程化架构与微前端", "SSR服务端渲染性能首屏优化", "跨端跨平台响应式UI套件", "富文本与低代码引擎"),
                "应用与分析", "现代化产业级大数据可视化交互大屏"
        ));
        TRACK_DATABASE.put("BIGDATA", new JobMarketTrackInfo(
                "TRACK_BIGDATA", "大数据与实时计算工程师", 87, "平稳增长", "24k - 42k",
                List.of("Flink", "Spark", "Kafka", "ClickHouse", "Doris", "Hudi/Iceberg数据湖"),
                List.of("实时数仓构建与流批一体", "百亿级日志实时聚合分析", "复杂事件处理CEP", "实时特征工程"),
                "分析与评价", "实时风控与用户全景画像计算中台"
        ));
    }

    @Override
    public JobMarketTrackInfo apply(JobMarketQueryRequest request) {
        String kw = request != null && StringUtils.hasText(request.getRoleOrKeyword()) ? request.getRoleOrKeyword().trim().toUpperCase() : "";
        log.info("[JobMarketRadarTool] 收到智能体产业行情查询: {}", kw);

        for (Map.Entry<String, JobMarketTrackInfo> entry : TRACK_DATABASE.entrySet()) {
            if (kw.contains(entry.getKey()) || entry.getValue().getTrackName().toUpperCase().contains(kw)) {
                return entry.getValue();
            }
        }

        if (kw.contains("架构") || kw.contains("后端") || kw.contains("SPRING") || kw.contains("分布式")) {
            return TRACK_DATABASE.get("JAVA");
        }
        if (kw.contains("AI") || kw.contains("模型") || kw.contains("AGENT") || kw.contains("算法")) {
            return TRACK_DATABASE.get("LLM");
        }
        if (kw.contains("运维") || kw.contains("容器") || kw.contains("CLOUD")) {
            return TRACK_DATABASE.get("DEVOPS");
        }
        if (kw.contains("页面") || kw.contains("VUE") || kw.contains("REACT") || kw.contains("UI")) {
            return TRACK_DATABASE.get("FRONTEND");
        }

        // 默认返回综合热门岗位
        return TRACK_DATABASE.get("JAVA");
    }

    public List<JobMarketTrackInfo> getAllTracks() {
        return new ArrayList<>(TRACK_DATABASE.values());
    }
}
