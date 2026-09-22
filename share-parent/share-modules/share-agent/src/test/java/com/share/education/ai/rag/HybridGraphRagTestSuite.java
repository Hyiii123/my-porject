package com.share.education.ai.rag;

import com.share.education.ai.model.CourseDocItem;
import com.share.education.ai.rag.bm25.Bm25SearchEngine;
import com.share.education.ai.rag.graph.KnowledgeGraphRagService;
import com.share.education.ai.rag.model.HybridRagResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class HybridGraphRagTestSuite {

    @Test
    @DisplayName("测试1：Okapi BM25 稀疏精确倒排索引分词与打分排序")
    public void testBm25SparseSearch() {
        Bm25SearchEngine engine = new Bm25SearchEngine();

        CourseDocItem c1 = new CourseDocItem(101L, "SpringCloud 微服务架构与高并发实战", "SpringCloud, Nacos, Sentinel, Seata, 微服务", BigDecimal.valueOf(199), "");
        CourseDocItem c2 = new CourseDocItem(102L, "Vue3 组合式 API 现代前端全栈开发", "Vue3, TypeScript, Vite, Pinia, Web前端", BigDecimal.valueOf(199), "");
        CourseDocItem c3 = new CourseDocItem(103L, "大语言模型应用开发与 LoRA 微调实战", "LLM, LangChain, RAG, LoRA, Python, Prompt", BigDecimal.valueOf(299), "");

        engine.buildIndex(List.of(c1, c2, c3));

        List<Map<String, Object>> hitsMicro = engine.search("微服务 高并发", 5);
        assertFalse(hitsMicro.isEmpty());
        assertEquals(101L, hitsMicro.get(0).get("id"));
        assertTrue((Double) hitsMicro.get(0).get("bm25Score") > 0);

        List<Map<String, Object>> hitsVue = engine.search("Vue3 TypeScript", 5);
        assertFalse(hitsVue.isEmpty());
        assertEquals(102L, hitsVue.get(0).get("id"));

        List<Map<String, Object>> hitsLlm = engine.search("大模型 LoRA", 5);
        assertFalse(hitsLlm.isEmpty());
        assertEquals(103L, hitsLlm.get(0).get("id"));
    }

    @Test
    @DisplayName("测试2：Graph RAG 先修拓扑图链路推演与证据上下文组装")
    public void testGraphRagTraversalAndContext() {
        KnowledgeGraphRagService graphService = new KnowledgeGraphRagService();

        List<String> chain = graphService.computePrerequisiteChain("分布式微服务集群治理");
        assertNotNull(chain);
        assertFalse(chain.isEmpty());
        assertTrue(chain.contains("Java核心基础与多线程"));

        List<String> gaps = graphService.detectPrerequisiteGaps(1001L, chain);
        assertNotNull(gaps);

        String context = graphService.buildGraphEvidenceContext("分布式微服务集群治理", chain, gaps);
        assertNotNull(context);
        assertTrue(context.contains("Kahn DAG 先修拓扑跃迁链"));
        assertTrue(context.contains("➔"));
    }

    @Test
    @DisplayName("测试3：Dense + BM25 RRF 倒数排名融合与全流程检索")
    public void testHybridRrfSearch() {
        Bm25SearchEngine bm25 = new Bm25SearchEngine();

        CourseDocItem c1 = new CourseDocItem(201L, "企业级分布式事务 Seata 与高并发", "Seata, MySQL, 分布式锁, 高并发", BigDecimal.valueOf(199), "");
        CourseDocItem c2 = new CourseDocItem(202L, "Docker 容器化与 Kubernetes 生产集群编排", "Docker, K8s, DevOps, Linux", BigDecimal.valueOf(199), "");

        bm25.buildIndex(List.of(c1, c2));

        KnowledgeGraphRagService graph = new KnowledgeGraphRagService();
        EducationKnowledgeRAG career = new EducationKnowledgeRAG();

        HybridGraphRagEngine engine = new HybridGraphRagEngine(bm25, graph, career);
        HybridRagResult result = engine.retrieve("Seata 分布式事务", 1001L, 5);

        assertNotNull(result);
        assertEquals("Seata 分布式事务", result.getQuery());
        assertFalse(result.getFusedCourses().isEmpty());

        Map<String, Object> topCourse = result.getFusedCourses().get(0);
        assertEquals(201L, topCourse.get("id"));
        assertTrue((Double) topCourse.get("rrfScore") > 0);
        assertEquals("Hybrid (Dense + BM25 RRF)", topCourse.get("retrievalType"));

        assertFalse(result.getGraphPrerequisiteChain().isEmpty());
        assertNotNull(result.getCareerBenchmarkText());
    }
}
