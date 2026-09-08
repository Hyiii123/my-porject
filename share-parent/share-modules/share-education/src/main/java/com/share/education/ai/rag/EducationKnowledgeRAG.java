package com.share.education.ai.rag;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 教育胜任力与 IT 行业知识图谱 RAG 检索增强引擎。
 *
 * <p>提供权威 IT 岗位职级标准（如阿里 P6/P7、腾讯 T9/T10、字节跳动能力模型）
 * 及课程核心知识点证据切片，注入给智能体以杜绝模型幻觉，使推荐解释具备极强的行业公信力。</p>
 */
@Component
public class EducationKnowledgeRAG {

    /** 行业岗位能力模型知识切片 */
    private static final List<KnowledgeChunk> CAREER_KNOWLEDGE_BASE = new ArrayList<>();

    static {
        CAREER_KNOWLEDGE_BASE.add(new KnowledgeChunk(
            "Java 架构师",
            List.of("Java", "SpringBoot", "SpringCloud", "JVM", "MySQL", "Redis", "微服务", "高并发"),
            "【阿里巴巴/腾讯 P7 架构师标准】要求深刻理解 JVM 内存模型与性能调优，熟练掌握 Spring Cloud 微服务体系（Nacos 注册配置、Sentinel 流量防卫、Seata 分布式事务），具备千万级流量 MySQL 分库分表设计与 Redis 复杂分布式锁架构实战能力。"
        ));
        CAREER_KNOWLEDGE_BASE.add(new KnowledgeChunk(
            "前端全栈开发",
            List.of("Vue", "Vue3", "React", "TypeScript", "Node.js", "Next.js", "前端工程化"),
            "【字节跳动/美团 高级前端工程师标准】要求精通现代组件化架构（Vue3 Composition API 与 React 18 并发渲染机制），具备 TypeScript 严格类型驱动开发素养，深入理解 Next.js 14 服务端渲染 (SSR/SSG) 与 Node.js 全栈服务端接口设计。"
        ));
        CAREER_KNOWLEDGE_BASE.add(new KnowledgeChunk(
            "大模型与 AI 算法工程师",
            List.of("Python", "PyTorch", "LLM", "大语言模型", "RAG", "LangChain", "LoRA", "向量数据库"),
            "【行业前沿 AI 算法与应用专家标准】要求掌握 PyTorch 深度学习实战，精通基于开源大模型（Qwen、Llama）的 LoRA/QLoRA 监督微调技术，熟练构建基于 LangChain、Spring AI 的企业级 RAG 知识库与 Multi-Agent 智能体协同系统。"
        ));
        CAREER_KNOWLEDGE_BASE.add(new KnowledgeChunk(
            "云原生与 DevOps 架构师",
            List.of("Docker", "Kubernetes", "K8s", "CI/CD", "Linux", "Prometheus", "云原生"),
            "【腾讯云/阿里云 容器平台专家标准】要求精通 Docker 容器化封装原理，掌握 Kubernetes 生产级高可用集群调度与弹性伸缩，熟练搭建基于 GitLab CI/Jenkins 的自动化持续交付流水线与 Prometheus/Grafana 全链路可观测体系。"
        ));
        CAREER_KNOWLEDGE_BASE.add(new KnowledgeChunk(
            "大数据与数仓工程师",
            List.of("Flink", "Spark", "Kafka", "ClickHouse", "Hadoop", "数仓"),
            "【一线大厂实时数仓专家标准】要求精通 Apache Flink 实时流处理计算框架与 Exact-Once 语义保障，掌握 Apache Spark 批处理海量数据离线清洗，熟练运用 ClickHouse 亿级 OLAP 分析与 Kafka 分布式消息队列架构。"
        ));
        CAREER_KNOWLEDGE_BASE.add(new KnowledgeChunk(
            "移动端与鸿蒙跨平台工程师",
            List.of("Flutter", "HarmonyOS", "鸿蒙", "移动开发", "Android"),
            "【华为生态/跨平台技术标准】要求精通 Flutter 3.x 双端渲染原理与状态管理，掌握 HarmonyOS NEXT 鸿蒙原生应用开发与 ArkTS/ArkUI 声明式范式，具备高性能原生桥接与跨端业务落地能力。"
        ));
        CAREER_KNOWLEDGE_BASE.add(new KnowledgeChunk(
            "网络安全与渗透工程师",
            List.of("安全渗透", "Web安全", "Kali", "漏洞挖掘", "密码学"),
            "【国家信息安全与攻防专家标准】要求精通 OWASP Top 10 Web 漏洞机理与防御策略，掌握 Kali Linux 自动化渗透测试与应急响应，具备企业内网横向移动分析与代码安全静态审计能力。"
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

        // 计算知识切片匹配得分
        KnowledgeChunk bestChunk = null;
        int maxScore = -1;

        for (KnowledgeChunk chunk : CAREER_KNOWLEDGE_BASE) {
            int score = 0;
            if (StringUtils.hasText(targetRole) && (chunk.roleName.contains(targetRole) || targetRole.contains(chunk.roleName))) {
                score += 10;
            }
            if (skills != null) {
                for (String s : skills) {
                    for (String kw : chunk.keywords) {
                        if (kw.equalsIgnoreCase(s) || kw.contains(s) || s.contains(kw)) {
                            score += 2;
                        }
                    }
                }
            }
            if (score > maxScore) {
                maxScore = score;
                bestChunk = chunk;
            }
        }

        if (bestChunk != null && maxScore > 0) {
            return bestChunk.content;
        }

        return "【专业技术工程师标准】聚焦核心技术实战，夯实先修基石，遵循标准工程化链路完成知识迭代与职业破局。";
    }

    private static class KnowledgeChunk {
        String roleName;
        List<String> keywords;
        String content;

        KnowledgeChunk(String roleName, List<String> keywords, String content) {
            this.roleName = roleName;
            this.keywords = keywords;
            this.content = content;
        }
    }
}
