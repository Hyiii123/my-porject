package com.share.education.ai.evals.benchmark;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.education.ai.agent.PathCriticAgent;
import com.share.education.ai.agent.PathPlanningAgent;
import com.share.education.ai.model.AnalyzedCourseVO;
import com.share.education.ai.model.CriticReport;
import com.share.education.ai.model.LearningPathPlan;
import com.share.education.ai.model.UserProfileContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 智问学伴 50 组典型学员画像黄金基准离线自动化回归评测套件 (Golden Benchmark Regression Suite)。
 */
class GoldenBenchmarkRegressionTest {

    private PathPlanningAgent planningAgent;
    private PathCriticAgent criticAgent;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        planningAgent = new PathPlanningAgent();
        criticAgent = new PathCriticAgent();
        objectMapper = new ObjectMapper();
    }

    @Test
    void runFullGoldenBenchmarkSuite() throws Exception {
        InputStream is = getClass().getResourceAsStream("/benchmarks/golden_personas_50.json");
        assertThat(is).as("黄金基准数据集必须存在").isNotNull();

        List<Map<String, Object>> personas = objectMapper.readValue(is, new TypeReference<>() {});
        assertThat(personas).hasSize(50);

        int totalPassCount = 0;
        double sumDagRate = 0.0;
        double sumSmoothness = 0.0;
        double sumBalance = 0.0;

        for (Map<String, Object> p : personas) {
            String role = String.valueOf(p.get("role"));
            int difficulty = ((Number) p.get("preferredDifficulty")).intValue();
            int discipline = ((Number) p.get("disciplineScore")).intValue();
            double completedHours = ((Number) p.get("completedHours")).doubleValue();
            List<String> gaps = (List<String>) p.get("skillGaps");

            UserProfileContext profile = UserProfileContext.builder()
                .userId(((Number) p.get("personaId")).longValue())
                .intendedRole(role)
                .preferredDifficulty(difficulty)
                .disciplineScore(discipline)
                .completedHours(completedHours)
                .skillGaps(gaps)
                .build();

            // 构造针对该画像的多样化候选课程集合 (8~12门)
            List<AnalyzedCourseVO> candidatePool = generateMockCoursesForRole(role);

            // 执行路径规划
            LearningPathPlan plan = planningAgent.planPath(profile, candidatePool);
            assertThat(plan).isNotNull();
            assertThat(plan.getStages()).isNotEmpty();

            // 执行客观质检审计
            CriticReport report = criticAgent.auditPathPlan(plan, profile);
            assertThat(report).isNotNull();

            sumDagRate += report.getPrerequisiteScore();
            sumSmoothness += report.getSmoothnessScore();
            sumBalance += report.getBalanceScore();

            if (Boolean.TRUE.equals(report.getPassed()) && report.getOverallScore() >= 80) {
                totalPassCount++;
            }
        }

        double avgDagRate = sumDagRate / personas.size();
        double avgSmoothness = sumSmoothness / personas.size();
        double avgBalance = sumBalance / personas.size();
        double passRate = ((double) totalPassCount / personas.size()) * 100.0;

        System.out.printf("=== 50 组黄金画像基准回归审计结果 ===%n");
        System.out.printf("评估学员总数: %d%n", personas.size());
        System.out.printf("DAG 拓扑数学合规率: %.2f%%%n", avgDagRate);
        System.out.printf("布鲁姆认知平滑度: %.2f分%n", avgSmoothness);
        System.out.printf("里程碑阶段容量均衡度: %.2f分%n", avgBalance);
        System.out.printf("一次质检达标率: %.2f%%%n", passRate);

        assertThat(avgDagRate).as("DAG 合规率必须达到 100%").isEqualTo(100.0);
        assertThat(avgSmoothness).as("认知平滑度必须 >= 90分").isGreaterThanOrEqualTo(90.0);
        assertThat(avgBalance).as("阶段均衡度必须 >= 90分").isGreaterThanOrEqualTo(90.0);
        assertThat(passRate).as("黄金基准通过率必须达到 100%").isEqualTo(100.0);
    }

    private List<AnalyzedCourseVO> generateMockCoursesForRole(String role) {
        List<AnalyzedCourseVO> list = new ArrayList<>();
        String kw = role.contains("Java") ? "Java" : (role.contains("Go") ? "Go" : (role.contains("大模型") ? "AI大模型" : "核心技术"));

        // 阶段1：初级入门课
        list.add(AnalyzedCourseVO.builder()
            .courseId(101L).courseName(kw + " 核心基础与语言规范")
            .difficultyLevel(1).matchScore(90.0).prerequisiteSkills(List.of("计算机基础知识"))
            .coreKnowledgePoints(List.of(kw, "语法规范", "基础库"))
            .build());

        // 阶段2：中级进阶课
        list.add(AnalyzedCourseVO.builder()
            .courseId(102L).courseName(kw + " 企业级主流框架精讲")
            .difficultyLevel(2).matchScore(88.0).prerequisiteSkills(List.of(kw + " 核心基础与语言规范"))
            .coreKnowledgePoints(List.of("企业框架", "组件设计", "实战"))
            .build());
        list.add(AnalyzedCourseVO.builder()
            .courseId(103L).courseName(kw + " 高性能中间件与数据存储")
            .difficultyLevel(2).matchScore(86.0).prerequisiteSkills(List.of(kw + " 核心基础与语言规范"))
            .coreKnowledgePoints(List.of("缓存", "数据库", "消息队列"))
            .build());

        // 阶段3：架构实战课
        list.add(AnalyzedCourseVO.builder()
            .courseId(104L).courseName(kw + " 分布式微服务生产集群治理")
            .difficultyLevel(3).matchScore(85.0).prerequisiteSkills(List.of(kw + " 企业级主流框架精讲"))
            .coreKnowledgePoints(List.of("分布式", "微服务治理", "高可用"))
            .isCapstone(true)
            .build());

        // 阶段4：底层攻坚与内核调优课
        list.add(AnalyzedCourseVO.builder()
            .courseId(105L).courseName(kw + " 底层内核原理与高并发性能调优")
            .difficultyLevel(3).matchScore(84.0).prerequisiteSkills(List.of(kw + " 分布式微服务生产集群治理"))
            .coreKnowledgePoints(List.of("内核源码", "调优", "突破"))
            .build());

        return list;
    }
}
