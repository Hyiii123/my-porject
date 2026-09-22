package com.share.education.ai.tools;

import com.share.education.ai.tools.market.JobMarketQueryRequest;
import com.share.education.ai.tools.market.JobMarketRadarTool;
import com.share.education.ai.tools.market.JobMarketTrackInfo;
import com.share.education.ai.tools.registry.AgentToolRegistry;
import com.share.education.ai.tools.registry.ToolDescriptor;
import com.share.education.ai.tools.remediation.AdaptiveRemediationRequest;
import com.share.education.ai.tools.remediation.AdaptiveRemediationTool;
import com.share.education.ai.tools.remediation.RemediationPlanDTO;
import com.share.education.ai.tools.sandbox.CodeExecutionRequest;
import com.share.education.ai.tools.sandbox.CodeExecutionResult;
import com.share.education.ai.tools.sandbox.CodeSandboxTool;
import com.share.education.ai.tools.sandbox.JavaSandboxRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 智能体工具体系全景单元与集成回归测试套件。
 */
public class AgentToolsTestSuite {

    @Test
    @DisplayName("测试1：安全代码沙箱正常执行并捕获输出")
    public void testCodeSandboxNormalExecution() {
        CodeSandboxTool sandboxTool = new CodeSandboxTool(new JavaSandboxRunner());
        String code = "public class Solution { public static void main(String[] args) { System.out.println(\"Hello Zhiwen Sandbox!\"); } }";
        CodeExecutionResult result = sandboxTool.apply(new CodeExecutionRequest(code, "java"));

        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertTrue(result.getStdout().contains("Hello Zhiwen Sandbox!"));
        assertEquals(0, result.getExitCode());
    }

    @Test
    @DisplayName("测试2：安全代码沙箱阻断高危提权与系统进程命令")
    public void testCodeSandboxSecurityViolation() {
        CodeSandboxTool sandboxTool = new CodeSandboxTool(new JavaSandboxRunner());
        String maliciousCode = "public class Attack { public static void main(String[] args) { System.exit(0); } }";
        CodeExecutionResult result = sandboxTool.apply(new CodeExecutionRequest(maliciousCode, "java"));

        assertNotNull(result);
        assertEquals("SECURITY_VIOLATION", result.getStatus());
        assertEquals(126, result.getExitCode());
        assertTrue(result.getStderr().contains("高危代码"));
    }

    @Test
    @DisplayName("测试3：产业招聘行情雷达多赛道精准匹配")
    public void testJobMarketRadar() {
        JobMarketRadarTool radarTool = new JobMarketRadarTool();

        JobMarketTrackInfo llmTrack = radarTool.apply(new JobMarketQueryRequest("大语言模型应用工程师"));
        assertNotNull(llmTrack);
        assertEquals("TRACK_LLM", llmTrack.getTrackCode());
        assertTrue(llmTrack.getDemandIndex() >= 90);
        assertTrue(llmTrack.getHotKeywords().contains("RAG架构"));

        JobMarketTrackInfo javaTrack = radarTool.apply(new JobMarketQueryRequest("高并发架构师"));
        assertNotNull(javaTrack);
        assertEquals("TRACK_JAVA", javaTrack.getTrackCode());
        assertTrue(javaTrack.getHotKeywords().contains("RocketMQ 5"));

        List<JobMarketTrackInfo> all = radarTool.getAllTracks();
        assertTrue(all.size() >= 5);
    }

    @Test
    @DisplayName("测试4：知识图谱先修闭包与自适应补救诊断")
    public void testAdaptiveRemediation() {
        AdaptiveRemediationTool remediationTool = new AdaptiveRemediationTool(new com.share.education.ai.rag.graph.KnowledgeGraphRagService());
        RemediationPlanDTO plan = remediationTool.apply(new AdaptiveRemediationRequest(1001L, "分布式微服务生产集群治理"));

        assertNotNull(plan);
        assertEquals("分布式微服务生产集群治理", plan.getTargetConcept());
        assertFalse(plan.getPrerequisiteClosureChain().isEmpty());
        assertFalse(plan.getMissingFoundationalConcepts().isEmpty());
        assertFalse(plan.getRemedialActionTasks().isEmpty());
        assertTrue(plan.getDiagnosticExplanation().contains("针对性追溯补救路径"));
    }

    @Test
    @DisplayName("测试5：智能体工具统一注册中心与度量大屏")
    public void testAgentToolRegistryAndTelemetry() {
        AgentToolRegistry registry = new AgentToolRegistry();

        List<ToolDescriptor> tools = registry.listTools();
        assertEquals(5, tools.size());

        registry.recordInvocation("codeSandboxTool", true, 45L);
        registry.recordInvocation("codeSandboxTool", true, 55L);
        registry.recordInvocation("jobMarketRadarTool", true, 10L);

        Map<String, Object> telemetry = registry.getTelemetry();
        assertEquals(5, telemetry.get("totalRegisteredTools"));
        assertEquals(3L, telemetry.get("totalToolInvocations"));

        List<?> metrics = (List<?>) telemetry.get("toolMetrics");
        assertNotNull(metrics);
        assertEquals(5, metrics.size());
    }
}
