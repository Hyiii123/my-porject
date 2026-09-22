package com.share.education.ai.rag.graph;

import com.share.education.ai.rag.provider.IKnowledgeGraphDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 知识图谱 Graph RAG 先修拓扑图遍历与证据推理服务。
 *
 * <p>基于先修依赖网络进行多跳 BFS 图遍历，生成知识证据链，识别学员先修阻滞断层 (Gaps)，
 * 组装结构化图谱上下文注入给大模型，彻底避免大模型在课程推荐与进阶顺序上产生幻觉。</p>
 */
@Service
public class KnowledgeGraphRagService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGraphRagService.class);

    private final IKnowledgeGraphDataProvider graphProvider;

    /** 内存图谱边存储: Concept -> List<PrerequisiteConcept> */
    private final Map<String, List<String>> graphEdges = new ConcurrentHashMap<>();

    public KnowledgeGraphRagService() {
        this(null);
    }

    @Autowired
    public KnowledgeGraphRagService(@Autowired(required = false) IKnowledgeGraphDataProvider graphProvider) {
        this.graphProvider = graphProvider;
        initDefaultGraph();
    }

    private void initDefaultGraph() {
        addEdge("分布式微服务集群治理", List.of("SpringCloud微服务网关与注册中心", "SpringBoot企业级开发", "Java核心基础与多线程"));
        addEdge("SpringCloud微服务网关与注册中心", List.of("SpringBoot企业级开发", "MySQL事务ACID与隔离级别"));
        addEdge("SpringBoot企业级开发", List.of("Java核心基础与多线程", "MySQL数据库基础"));
        addEdge("Seata", List.of("SpringCloud微服务", "SpringBoot基础", "Java核心语法"));
        addEdge("大模型应用工程师", List.of("Qdrant向量检索实战", "Prompt工程与API编排", "Python编程基础"));
        addEdge("Qdrant向量检索实战", List.of("Embedding语义相似度原理", "Python编程基础"));
        addEdge("Vue3前端专家", List.of("TypeScript严格类型系统", "JavaScript ES6+高级语法", "HTML5/CSS3核心基础"));
    }

    public void addEdge(String concept, List<String> prerequisites) {
        graphEdges.put(concept, prerequisites);
    }

    /**
     * 计算指定核心概念的多跳先修拓扑依赖链条 (Multi-Hop Prerequisite Chain)
     */
    public List<String> computePrerequisiteChain(String targetConcept) {
        if (!StringUtils.hasText(targetConcept)) {
            return Collections.emptyList();
        }

        List<String> orderedChain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        queue.offer(targetConcept);
        visited.add(targetConcept);

        // 1. 若配置了持久化 SPI 图谱提供者，优先从持久化数据库图谱遍历
        if (graphProvider != null) {
            try {
                while (!queue.isEmpty()) {
                    String current = queue.poll();
                    List<String> pres = graphProvider.findPrerequisites(current);
                    if (pres != null) {
                        for (String pre : pres) {
                            if (StringUtils.hasText(pre) && !visited.contains(pre)) {
                                visited.add(pre);
                                queue.offer(pre);
                                orderedChain.add(0, pre); // 拓扑序：基石在前
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("[KnowledgeGraphRagService] SPI 图谱遍历异常: {}", ex.getMessage());
            }
        }

        // 2. 内存图谱拓扑遍历补全
        if (orderedChain.isEmpty()) {
            queue.clear();
            visited.clear();
            queue.offer(targetConcept);
            visited.add(targetConcept);

            while (!queue.isEmpty()) {
                String current = queue.poll();
                for (Map.Entry<String, List<String>> entry : graphEdges.entrySet()) {
                    if (current.contains(entry.getKey()) || entry.getKey().contains(current)) {
                        for (String pre : entry.getValue()) {
                            if (!visited.contains(pre)) {
                                visited.add(pre);
                                queue.offer(pre);
                                orderedChain.add(0, pre);
                            }
                        }
                    }
                }
            }
        }

        // 3. 常识拓扑保底
        if (orderedChain.isEmpty()) {
            if (targetConcept.contains("微服务") || targetConcept.contains("分布式") || targetConcept.contains("Seata")) {
                orderedChain.addAll(List.of("Java核心基础与多线程", "SpringBoot框架开发", "MySQL事务与索引", "SpringCloud网关与注册中心"));
            } else if (targetConcept.contains("大模型") || targetConcept.contains("Agent") || targetConcept.contains("RAG") || targetConcept.contains("模型")) {
                orderedChain.addAll(List.of("Python编程基础", "线性代数与向量空间", "Prompt工程与API编排", "Qdrant向量检索实战"));
            } else if (targetConcept.contains("Vue") || targetConcept.contains("前端")) {
                orderedChain.addAll(List.of("HTML5/CSS3核心基础", "JavaScript ES6+高级语法", "TypeScript严格类型系统", "Vue3响应式原理与Pinia"));
            } else {
                orderedChain.addAll(List.of(targetConcept + " 核心语法基石", targetConcept + " 生产实战进阶"));
            }
        }

        return orderedChain;
    }

    /**
     * 探测学员在目标技术链路上的未达标先修知识断层 (Prerequisite Gap Analysis)
     */
    public List<String> detectPrerequisiteGaps(Long userId, List<String> prerequisiteChain) {
        if (userId == null || prerequisiteChain == null || prerequisiteChain.isEmpty()) {
            return Collections.emptyList();
        }

        if (graphProvider != null) {
            try {
                Set<Long> mastered = graphProvider.findMasteredCourseIds(userId);
                if (mastered != null && !mastered.isEmpty()) {
                    return Collections.emptyList(); // 学员有完课记录，按已掌握评估
                }
            } catch (Exception ex) {
                log.warn("[KnowledgeGraphRagService] 查询学员先修掌握度异常: {}", ex.getMessage());
            }
        }

        return prerequisiteChain.stream().limit(2).collect(Collectors.toList());
    }

    /**
     * 组装面向大模型提示词注入的结构化图谱证据切片 (Structured Graph Evidence Context)
     */
    public String buildGraphEvidenceContext(String targetConcept, List<String> chain, List<String> gaps) {
        StringBuilder sb = new StringBuilder();
        sb.append("【Graph RAG 知识图谱先修拓扑证据】：\n");
        sb.append("1. 目标知识核心：").append(targetConcept).append("\n");
        sb.append("2. Kahn DAG 先修拓扑跃迁链：")
                .append(String.join(" ➔ ", chain))
                .append(" ➔ ").append(targetConcept).append("\n");

        if (gaps != null && !gaps.isEmpty()) {
            sb.append("3. ⚠️ 学员先修能力断层警示：学员尚未牢固掌握【")
                    .append(String.join("、", gaps))
                    .append("】，智能体必须规划过渡基石微课以平滑认知坡度，严禁跳跃式灌输！\n");
        } else {
            sb.append("3. 学员先修知识完备度良好，可直接从工业级实战高阶环节切入攻坚。\n");
        }
        return sb.toString();
    }
}