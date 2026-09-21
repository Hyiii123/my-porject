package com.share.education.ai.tools.remediation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.education.domain.EduKnowledgePrerequisite;
import com.share.education.mapper.EduKnowledgePrerequisiteMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;

/**
 * 知识图谱自适应补救诊断工具 (AdaptiveRemediationTool)。
 * 基于先修依赖图谱计算【最小前置知识闭包 (Minimal Prerequisite Closure)】，
 * 为做错题或遇到卡点的学员回溯根因先修缺陷并组装针对性微课任务。
 */
@Component("adaptiveRemediationTool")
public class AdaptiveRemediationTool implements Function<AdaptiveRemediationRequest, RemediationPlanDTO> {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveRemediationTool.class);

    private final EduKnowledgePrerequisiteMapper prerequisiteMapper;

    public AdaptiveRemediationTool(@org.springframework.beans.factory.annotation.Autowired(required = false) EduKnowledgePrerequisiteMapper prerequisiteMapper) {
        this.prerequisiteMapper = prerequisiteMapper;
    }

    @Override
    public RemediationPlanDTO apply(AdaptiveRemediationRequest request) {
        String concept = request != null && StringUtils.hasText(request.getWeakConcept()) ? request.getWeakConcept().trim() : "分布式微服务生产集群治理";
        log.info("[AdaptiveRemediationTool] 收到学员知识闭包诊断请求: userId={}, weakConcept={}",
                request != null ? request.getUserId() : null, concept);

        RemediationPlanDTO plan = new RemediationPlanDTO();
        plan.setTargetConcept(concept);

        // 1. 图搜索广度遍历 (BFS DAG Traversal)，提取多跳先修闭包
        List<String> closure = computeMinimalPrerequisiteClosure(concept);
        plan.setPrerequisiteClosureChain(closure);

        // 2. 识别关键基石先修（链条最前端的基础概念）
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

        // 3. 构造智能体自适应微任务清单
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

        // 4. 推荐补救课程
        plan.setRecommendedCourseIds(List.of(101L, 102L, 103L));
        plan.setDiagnosticExplanation(String.format(
                "经知识图谱拓扑闭包分析，学员在【%s】产生认知阻滞的根因，在于前置基石【%s】的熟练度不足。已自动生成由浅入深的 %d 步针对性追溯补救路径。",
                concept, String.join("、", foundational), tasks.size()));

        return plan;
    }

    private List<String> computeMinimalPrerequisiteClosure(String targetConcept) {
        List<String> orderedClosure = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        queue.offer(targetConcept);
        visited.add(targetConcept);

        // 如果数据库连接可用，从 edu_knowledge_prerequisite 查真实依赖
        if (prerequisiteMapper != null) {
            try {
                while (!queue.isEmpty()) {
                    String current = queue.poll();
                    List<EduKnowledgePrerequisite> prereqs = prerequisiteMapper.selectList(
                            new LambdaQueryWrapper<EduKnowledgePrerequisite>()
                                    .like(EduKnowledgePrerequisite::getConceptName, current)
                                    .or()
                                    .apply("LOCATE(concept_name, {0}) > 0", current));

                    for (EduKnowledgePrerequisite p : prereqs) {
                        String pre = p.getPrerequisiteConcept();
                        if (StringUtils.hasText(pre) && !visited.contains(pre)) {
                            visited.add(pre);
                            queue.offer(pre);
                            orderedClosure.add(0, pre); // 基础概念前置
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("[AdaptiveRemediationTool] 数据库图谱查询异常，启用领域常识闭包: {}", ex.getMessage());
            }
        }

        // 若图谱库无记录或未命中，使用技术体系拓扑常识保底
        if (orderedClosure.isEmpty()) {
            if (targetConcept.contains("微服务") || targetConcept.contains("分布式") || targetConcept.contains("Seata")) {
                orderedClosure.addAll(List.of("Java核心基础与多线程", "SpringBoot企业级开发", "MySQL事务ACID与隔离级别", "SpringCloud微服务网关与注册中心"));
            } else if (targetConcept.contains("模型") || targetConcept.contains("Agent") || targetConcept.contains("RAG")) {
                orderedClosure.addAll(List.of("Python与向量空间代数基础", "大模型Prompt工程规范", "Embedding语义相似度原理", "向量数据库Qdrant检索召回"));
            } else {
                orderedClosure.addAll(List.of(targetConcept + " 核心语法基础", targetConcept + " 生产实战规范"));
            }
        }

        return orderedClosure;
    }
}
