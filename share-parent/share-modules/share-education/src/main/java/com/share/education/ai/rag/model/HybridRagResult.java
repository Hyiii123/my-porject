package com.share.education.ai.rag.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 混合检索与图检索增强结果 (Hybrid Graph RAG Result)。
 */
@Data
public class HybridRagResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 检索原始查询词 */
    private String query;

    /** 融合后的课程列表 (带 RRF 得分与综合排序) */
    private List<Map<String, Object>> fusedCourses;

    /** BM25 稀疏精确命中列表 */
    private List<Map<String, Object>> bm25Hits;

    /** Dense 语义向量命中列表 */
    private List<Map<String, Object>> denseHits;

    /** Graph RAG 拓扑前驱依赖路径 */
    private List<String> graphPrerequisiteChain;

    /** 针对学员识别出的先修知识断层 (Gaps) */
    private List<String> prerequisiteGaps;

    /** 注入给大模型的结构化图谱证据上下文 */
    private String graphEvidenceContext;

    /** 行业权威岗位胜任力标准切片 (Career Benchmark) */
    private String careerBenchmarkText;

    /** 混合召回耗时毫秒数 */
    private Long latencyMs;

    public HybridRagResult() {}
}
