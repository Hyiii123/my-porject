package com.share.education.ai.composite.engine;

import com.share.education.ai.composite.node.IntentDispatcherNode;
import com.share.education.ai.composite.state.DebateBlackboardState;
import com.share.education.ai.evals.AgentEvalMetricsVO;
import com.share.education.ai.evals.AgentEvaluationService;
import com.share.education.ai.model.*;
import com.share.education.ai.workflow.AgentWorkflowContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class CompositeMultiAgentGraphEngineTest {

    private IntentDispatcherNode dispatcherNode;
    private AgentEvaluationService evalService;

    @BeforeEach
    void setUp() {
        dispatcherNode = new IntentDispatcherNode();
        evalService = new AgentEvaluationService();
    }

    @Test
    void testIntentDispatcherRoutesCorrectly() {
        DebateBlackboardState state = DebateBlackboardState.builder()
            .intendedRole("全栈开发工程师")
            .build();

        // 1. 模拟面试意图
        String routeInterview = dispatcherNode.dispatch("我想进行一次Java模拟面试考场", state);
        assertThat(routeInterview).isEqualTo(IntentDispatcherNode.ROUTE_MOCK_INTERVIEW);

        // 2. 事务卡片意图
        String routeCard = dispatcherNode.dispatch("帮我看看购物车有哪些优惠券可以领", state);
        assertThat(routeCard).isEqualTo(IntentDispatcherNode.ROUTE_ACTION_CARD);

        // 3. 规划意图与实体提取
        String routeDebate = dispatcherNode.dispatch("我想转行成为大模型工程师，请为我规划学习路线", state);
        assertThat(routeDebate).isEqualTo(IntentDispatcherNode.ROUTE_MULTI_AGENT_DEBATE);
        assertThat(state.getIntendedRole()).isEqualTo("大语言模型应用工程师");
    }

    @Test
    void testAgentEvaluationMetricsCalculation() {
        // 构造有效工作流上下文
        CriticReport report = CriticReport.builder()
            .passed(true)
            .overallScore(95)
            .prerequisiteScore(100)
            .smoothnessScore(94)
            .balanceScore(92)
            .verdictLevel("卓越 (A+)")
            .build();

        LearningPathPlan plan = LearningPathPlan.builder()
            .intendedRole("Java全栈架构师")
            .totalCourses(4)
            .totalEstimatedHours(100)
            .stages(List.of(
                PathStageVO.builder().stageIndex(1).stageName("阶段一").courses(List.of()).build()
            ))
            .build();

        PersonalizedRecommendVO rec = PersonalizedRecommendVO.builder()
            .id(101L)
            .title("Spring Cloud 微服务架构深度实战")
            .recommendReason("系统化深入掌握 Spring Cloud 微服务治理与高并发架构")
            .skillGapFilled("Spring Cloud 微服务")
            .build();

        AnalyzedCourseVO analyzed = AnalyzedCourseVO.builder()
            .courseId(101L)
            .courseName("Spring Cloud 微服务架构深度实战")
            .coreKnowledgePoints(List.of("Spring Cloud", "Nacos", "微服务"))
            .build();

        UserProfileContext profile = UserProfileContext.builder()
            .skillGaps(List.of("微服务", "Spring Cloud"))
            .build();

        AgentWorkflowContext ctx = AgentWorkflowContext.builder()
            .sessionId(UUID.randomUUID().toString())
            .startTime(System.currentTimeMillis() - 50)
            .userProfile(profile)
            .learningPathPlan(plan)
            .criticReport(report)
            .passedCritic(true)
            .recommendations(List.of(rec))
            .analyzedCourses(List.of(analyzed))
            .agentLatencies(Map.of("IndustryArchitectNode", 15L, "PathCriticNode", 10L))
            .build();

        evalService.recordPipelineExecution(ctx);

        AgentEvalMetricsVO metrics = evalService.getMetricsSnapshot();
        assertThat(metrics.getTotalPipelinesRun()).isEqualTo(1L);
        assertThat(metrics.getDagValidityRate()).isEqualTo(100.0);
        assertThat(metrics.getFaithfulnessScore()).isGreaterThanOrEqualTo(90.0);
        assertThat(metrics.getIntentAlignmentScore()).isGreaterThanOrEqualTo(90.0);
        assertThat(metrics.getRemedySuccessRate()).isEqualTo(100.0);
        assertThat(metrics.getDisagreementConvergenceRate()).isGreaterThanOrEqualTo(80.0);
        assertThat(metrics.getOverallHealthGrade()).contains("AAA");
    }
}
