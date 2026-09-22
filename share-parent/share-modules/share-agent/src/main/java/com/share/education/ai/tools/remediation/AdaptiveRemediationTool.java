package com.share.education.ai.tools.remediation;

import com.share.education.ai.rag.graph.KnowledgeGraphRagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;

/**
 * 知识图谱自适应补救诊断工具 (AdaptiveRemediationTool)。
 */
@Component("adaptiveRemediationTool")
public class AdaptiveRemediationTool implements Function<AdaptiveRemediationRequest, RemediationPlanDTO> {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveRemediationTool.class);

    private final KnowledgeGraphRagService graphService;

    public AdaptiveRemediationTool(KnowledgeGraphRagService graphService) {
        this.graphService = graphService;
    }

    @Override
    public RemediationPlanDTO apply(AdaptiveRemediationRequest request) {
        String concept = request != null && StringUtils.hasText(request.getWeakConcept()) ? request.getWeakConcept().trim() : "分布式微服务生产集群治理";
        log.info("[AdaptiveRemediationTool] 收到学员知识闭包诊断请求: userId={}, weakConcept={}",
                request != null ? request.getUserId() : null, concept);

        RemediationPlanDTO plan = new RemediationPlanDTO();
        plan.setTargetConcept(concept);

        List<String> closure = graphService.computePrerequisiteChain(concept);
        plan.setPrerequisiteClosureChain(closure);

        List<String> foundational = new ArrayList<>();
        if (!closure.isEmpty()) {
            foundational.add(closure.get(0));
            if (closure.size() > 1) {
                foundational.add(closure.get(1));
            }
        } else {
            foundational.add(concept + " 核心原理与底层基础");
        }
        plan.setMissingFoundationalConcepts(foundational);

        List<Map<String, Object>> tasks = new ArrayList<>();
        int step = 1;
        for (String c : closure) {
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("step", step++);
            task.put("concept", c);
            task.put("actionType", step == 1 ? "基础概念速通回顾 (15分钟)" : "核心源码调试与动手测验");
            task.put("targetProficiency", 80);
            tasks.add(task);
        }
        plan.setRemedialActionTasks(tasks);

        plan.setRecommendedCourseIds(List.of(101L, 102L, 103L));
        plan.setDiagnosticExplanation(String.format(
                "经知识图谱拓扑闭包分析，学员在【%s】产生认知阻滞的根因，在于前置基石【%s】的熟练度不足。已自动生成由浅入深的 %d 步针对性追溯补救路径。",
                concept, String.join("、", foundational), tasks.size()));

        return plan;
    }
}
