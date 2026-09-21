package com.share.education.ai.tools.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 智能体工具统一注册中心与度量大屏服务 (AgentToolRegistry)。
 * 统一管理所有具备 Function Calling 契约的标准工具，并收集在线调用频次与延迟。
 */
@Service
public class AgentToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(AgentToolRegistry.class);

    private final Map<String, ToolDescriptor> registeredTools = new LinkedHashMap<>();
    private final Map<String, AtomicLong> callCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> successCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> totalLatencyMs = new ConcurrentHashMap<>();

    public AgentToolRegistry() {
        initDefaultTools();
    }

    private void initDefaultTools() {
        register(new ToolDescriptor(
                "dragRecommendTool",
                "DRAG-KP4SR 知识图谱序列推荐算法引擎",
                "ALGORITHM",
                "基于学员时序学习轨迹与知识图谱先修依赖网络，召回拓扑合规的候选课程集",
                Map.of("type", "object", "properties", Map.of(
                        "userId", Map.of("type", "integer", "description", "学员唯一编号"),
                        "historyCourseIds", Map.of("type", "array", "items", Map.of("type", "integer")),
                        "intendedRole", Map.of("type", "string", "description", "目标技术方向")
                )),
                "SAFE"
        ));

        register(new ToolDescriptor(
                "codeSandboxTool",
                "动态多语言安全代码沙箱运行引擎",
                "EXECUTION",
                "在内存或独立安全沙箱中动态编译与运行学员或智能体生成的代码，输出控制台结果并熔断超时",
                Map.of("type", "object", "properties", Map.of(
                        "sourceCode", Map.of("type", "string", "description", "待执行源码"),
                        "language", Map.of("type", "string", "enum", List.of("java", "python", "javascript")),
                        "timeoutMs", Map.of("type", "integer", "description", "超时毫秒数")
                ), "required", List.of("sourceCode")),
                "SANDBOXED"
        ));

        register(new ToolDescriptor(
                "jobMarketRadarTool",
                "大厂招聘行情与前沿技术热度雷达",
                "MARKET",
                "实时查询 8 大技术赛道的大厂岗位 HC、热度指数、核心框架高频词与薪资分布",
                Map.of("type", "object", "properties", Map.of(
                        "roleOrKeyword", Map.of("type", "string", "description", "岗位或技术栈关键字")
                ), "required", List.of("roleOrKeyword")),
                "SAFE"
        ));

        register(new ToolDescriptor(
                "adaptiveRemediationTool",
                "知识图谱最小前置闭包自适应诊断工具",
                "REMEDIATION",
                "针对做错试题或遇到卡点的学员，广度遍历先修 DAG 图谱回溯根因缺陷并生成追溯补救微课清单",
                Map.of("type", "object", "properties", Map.of(
                        "userId", Map.of("type", "integer", "description", "学员ID"),
                        "weakConcept", Map.of("type", "string", "description", "薄弱概念或错题考点")
                ), "required", List.of("weakConcept")),
                "SAFE"
        ));

        register(new ToolDescriptor(
                "agentMemoryTool",
                "跨会话长期情景伴学记忆库",
                "MEMORY",
                "持久化存储并语义检索学员长程学习阻滞、突破时刻与导师指导建议",
                Map.of("type", "object", "properties", Map.of(
                        "userId", Map.of("type", "integer"),
                        "intendedRole", Map.of("type", "string")
                )),
                "SAFE"
        ));

        log.info("[AgentToolRegistry] 成功装配 5 组企业级智能体核心工具集 (Spring AI Function Calling)");
    }

    public void register(ToolDescriptor descriptor) {
        registeredTools.put(descriptor.getName(), descriptor);
        callCounters.putIfAbsent(descriptor.getName(), new AtomicLong(0));
        successCounters.putIfAbsent(descriptor.getName(), new AtomicLong(0));
        totalLatencyMs.putIfAbsent(descriptor.getName(), new AtomicLong(0));
    }

    public void recordInvocation(String toolName, boolean success, long latencyMs) {
        callCounters.computeIfAbsent(toolName, k -> new AtomicLong(0)).incrementAndGet();
        if (success) {
            successCounters.computeIfAbsent(toolName, k -> new AtomicLong(0)).incrementAndGet();
        }
        totalLatencyMs.computeIfAbsent(toolName, k -> new AtomicLong(0)).addAndGet(latencyMs);
    }

    public List<ToolDescriptor> listTools() {
        return new ArrayList<>(registeredTools.values());
    }

    public Map<String, Object> getTelemetry() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> metrics = new ArrayList<>();
        long totalCalls = 0;

        for (String name : registeredTools.keySet()) {
            long calls = callCounters.getOrDefault(name, new AtomicLong(0)).get();
            long succ = successCounters.getOrDefault(name, new AtomicLong(0)).get();
            long totalLat = totalLatencyMs.getOrDefault(name, new AtomicLong(0)).get();
            double avgLat = calls > 0 ? (double) totalLat / calls : 0.0;
            double succRate = calls > 0 ? ((double) succ / calls) * 100.0 : 100.0;
            totalCalls += calls;

            metrics.add(Map.of(
                    "toolName", name,
                    "displayName", registeredTools.get(name).getDisplayName(),
                    "category", registeredTools.get(name).getCategory(),
                    "totalCalls", calls,
                    "successRate", Math.round(succRate * 10.0) / 10.0,
                    "avgLatencyMs", Math.round(avgLat * 10.0) / 10.0
            ));
        }

        result.put("totalRegisteredTools", registeredTools.size());
        result.put("totalToolInvocations", totalCalls);
        result.put("toolMetrics", metrics);
        return result;
    }
}
