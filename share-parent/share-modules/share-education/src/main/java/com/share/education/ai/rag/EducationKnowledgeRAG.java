package com.share.education.ai.rag;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 教育胜任力与 IT 行业知识图谱 RAG 检索增强引擎。
 *
 * <p>提供权威 IT 岗位职级标准（如阿里 P6/P7、腾讯 T9/T10、字节跳动能力模型）
 * 及课程核心知识点证据切片，注入给智能体以杜绝模型幻觉，使推荐解释具备极强的行业公信力。</p>
 */
@Component
public class EducationKnowledgeRAG {

    public record KnowledgeChunk(String roleName, List<String> keywords, String content) {}

    /** 行业岗位能力模型知识切片 (扩展至 12 大核心 IT 专业方向) */
    private static final List<KnowledgeChunk> CAREER_KNOWLEDGE_BASE = new ArrayList<>();

    static {
        CAREER_KNOWLEDGE_BASE.add(new KnowledgeChunk(
            "Java 架构师",
            List.of("Java", "SpringBoot", "SpringCloud", "JVM", "MySQL", "Redis", "微服务", "高并发", "分布式"),
            "【阿里巴巴/腾讯 P7 架构师标准】要求深刻理解 JVM 内存模型与底层垃圾回收调优，熟练掌握 Spring Cloud 微服务体系（Nacos 注册配置中心、Sentinel 流量防卫限流、Seata 分布式事务一致性），具备千万级高并发流量下的 MySQL 分库分表与 Redis 复杂分布式锁实战能力。"
        ));
        CAREER_KNOWLEDGE_BASE.add(new KnowledgeChunk(
            "前端全栈开发",
            List.of("Vue", "Vue3", "React", "TypeScript", "Node.js", "Next.js", "前端工程化", "Webpack", "Vite"),
            "【字节跳动/美团 高级前端工程师标准】要求精通现代组件化架构（Vue3 Composition API 与 React 18 并发渲染机制），具备 TypeScript 严格类型驱动开发素养，深入理解 Next.js 14 服务端渲染 (SSR/SSG) 与 Node.js 全栈服务端接口与前端工程化脚手架设计。"
        ));
        CAREER_KNOWLEDGE_BASE.add(new KnowledgeChunk(
            "大模型与 AI 算法工程师",
            List.of("Python", "PyTorch", "LLM", "大语言模型", "RAG", "LangChain", "LoRA", "向量数据库", "Prompt"),
            "【行业前沿 AI 算法与应用专家标准】要求掌握 PyTorch 深度学习实战，精通基于开源大模型（Qwen、Llama）的 LoRA/QLoRA 监督微调技术，熟练构建基于 LangChain、Spring AI 的企业级 RAG 知识库与 Multi-Agent 多智能体协同博弈系统。"
        ));
        CAREER_KNOWLEDGE_BASE.add(new KnowledgeChunk(
            "云原生与 DevOps 架构师",
            List.of("Docker", "Kubernetes", "K8s", "CI/CD", "Linux", "Prometheus", "云原生", "Helm", "Istio"),
            "【腾讯云/阿里云 容器平台专家标准】要求精通 Docker 容器化封装原理，掌握 Kubernetes 生产级高可用集群调度与弹性伸缩，熟练搭建基于 GitLab CI/Jenkins 的自动化持续交付流水线与 Prometheus/Grafana 全链路可观测体系。"
        ));
        CAREER_KNOWLEDGE_BASE.add(new KnowledgeChunk(
            "大数据与数仓工程师",
            List.of("Flink", "Spark", "Kafka", "ClickHouse", "Hadoop", "数仓", "Hive", "HBase"),
            "【一线大厂实时数仓专家标准】要求精通 Apache Flink 实时流处理计算框架与 Exact-Once 语义保障，掌握 Apache Spark 批处理海量数据离线清洗，熟练运用 ClickHouse 亿级 OLAP 分析与 Kafka 分布式消息队列架构。"
        ));
        CAREER_KNOWLEDGE_BASE.add(new KnowledgeChunk(
            "移动端与鸿蒙跨平台工程师",
            List.of("Flutter", "HarmonyOS", "鸿蒙", "移动开发", "Android", "iOS", "ArkTS"),
            "【华为生态/跨平台技术标准】要求精通 Flutter 3.x 双端渲染原理与状态管理，掌握 HarmonyOS NEXT 鸿蒙原生应用开发与 ArkTS/ArkUI 声明式范式，具备高性能原生桥接与跨端业务落地能力。"
        ));
        CAREER_KNOWLEDGE_BASE.add(new KnowledgeChunk(
            "网络安全与渗透工程师",
            List.of("安全渗透", "Web安全", "Kali", "漏洞挖掘", "密码学", "OWASP", "内网渗透"),
            "【国家信息安全与攻防专家标准】要求精通 OWASP Top 10 Web 漏洞机理与防御策略，掌握 Kali Linux 自动化渗透测试与应急响应，具备企业内网横向移动分析与代码安全静态审计能力。"
        ));
        CAREER_KNOWLEDGE_BASE.add(new KnowledgeChunk(
            "Go 高并发微服务架构师",
            List.of("Go", "Golang", "Gin", "gRPC", "Protobuf", "Kratos", "微服务", "高并发"),
            "【字节跳动/B站 Go 核心研发标准】要求精通 Go 并发 GMP 调度模型与 Channel 内存通信原理，掌握基于 gRPC/Protobuf 的高性能 RPC 服务通信，具备基于 Gin/Kratos 的工业级微服务高并发低延迟治理经验。"
        ));
        CAREER_KNOWLEDGE_BASE.add(new KnowledgeChunk(
            "Python 全栈与自动化开发",
            List.of("Python", "Django", "FastAPI", "自动化", "爬虫", "Scrapy", "Celery"),
            "【高级 Python 全栈研发标准】要求熟练掌握 FastAPI 高性能异步 Web 框架与 Django 企业级后台架构，深入理解基于 Scrapy/Selenium 的反爬攻坚与自动化数据流水线，具备 Celery 分布式任务队列调度实战素养。"
        ));
        CAREER_KNOWLEDGE_BASE.add(new KnowledgeChunk(
            "嵌入式与物联网系统工程师",
            List.of("嵌入式", "C语言", "C++", "RTOS", "Linux内核", "ARM", "物联网", "STM32"),
            "【大厂物联网硬件与系统专家标准】要求精通 C/C++ 内存控制与驱动开发，熟练掌握 FreeRTOS/RT-Thread 等实时操作系统内核调度，具备基于 ARM 架构的系统移植、通信协议解析与工业级嵌入式软硬件协同攻坚能力。"
        ));
        CAREER_KNOWLEDGE_BASE.add(new KnowledgeChunk(
            "测试开发与质量工程专家",
            List.of("测开", "自动化测试", "Selenium", "JMeter", "性能压测", "CI/CD", "接口测试"),
            "【大厂测试开发专家标准】要求掌握基于 Python/Java 的全链路自动化测试框架研发，精通 JMeter/Gatling 亿级流量高并发性能压测与瓶颈分析，具备在 CI/CD 流水线中构建精准度量与持续质量防护网的能力。"
        ));
        CAREER_KNOWLEDGE_BASE.add(new KnowledgeChunk(
            "数据分析与商业智能专家",
            List.of("数据分析", "SQL", "Tableau", "PowerBI", "用户画像", "指标体系", "统计学"),
            "【一线大厂资深数据分析师标准】要求精通复杂 SQL 窗口函数与海量数据统计建模，具备全业务生命周期指标体系设计与归因分析能力，熟练运用 Python 数据挖掘进行用户分群与策略 A/B 实验评估。"
        ));
    }

    /**
     * 根据学员目标岗位和关涉技术标签召回匹配的行业标准证据切片
     *
     * @param targetRole 目标岗位
     * @param skills 涉及技术点
     * @return 权威行业胜任力与进阶标准摘要
     */
    public String retrieveCareerCompetencyBenchmark(String targetRole, List<String> skills) {
        if (!StringUtils.hasText(targetRole) && (skills == null || skills.isEmpty())) {
            return "【通用 IT 工程师职业进阶标准】要求建立扎实的基础理论、工程化架构能力与系统化实战经验，按由浅入深、循序渐进的梯度稳步攻坚。";
        }

        return CAREER_KNOWLEDGE_BASE.stream()
            .map(chunk -> Map.entry(chunk, scoreChunk(chunk, targetRole, skills)))
            .filter(e -> e.getValue() > 0)
            .max(Map.Entry.comparingByValue())
            .map(e -> e.getKey().content())
            .orElse("【专业技术工程师标准】聚焦核心技术实战，夯实先修基石，遵循标准工程化链路完成知识迭代与职业破局。");
    }

    private static String normalize(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\s\\-_/]+", "").toLowerCase();
    }

    private int scoreChunk(KnowledgeChunk chunk, String role, List<String> skills) {
        int score = 0;
        String normRole = normalize(role);
        String normChunkRole = normalize(chunk.roleName());
        if (StringUtils.hasText(normRole)) {
            if (normChunkRole.contains(normRole) || normRole.contains(normChunkRole)) {
                score += 15;
            } else {
                for (String kw : chunk.keywords()) {
                    String normKw = normalize(kw);
                    if (StringUtils.hasText(normKw) && normRole.contains(normKw)) {
                        score += 6;
                    }
                }
            }
        }

        if (skills != null) {
            for (String sk : skills) {
                if (!StringUtils.hasText(sk)) continue;
                String normSk = normalize(sk);
                for (String kw : chunk.keywords()) {
                    String normKw = normalize(kw);
                    if (normKw.equals(normSk) || normSk.contains(normKw) || normKw.contains(normSk)) {
                        score += 3;
                        break;
                    }
                }
            }
        }

        return score;
    }
}
