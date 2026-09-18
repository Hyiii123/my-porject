package com.share.education.ai.agent;

import com.share.education.ai.model.AnalyzedCourseVO;
import com.share.education.ai.model.LearningPathPlan;
import com.share.education.ai.model.PathStageVO;
import com.share.education.ai.model.UserProfileContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PathPlanningAgentTest {

    private PathPlanningAgent agent;

    @BeforeEach
    void setUp() {
        agent = new PathPlanningAgent();
    }

    @Test
    void testPlanPathPrunesUnrelatedDomainCourses() {
        UserProfileContext profile = UserProfileContext.builder()
                .userId(1001L)
                .intendedRole("Java 后端开发工程师 (日常实习/校招)")
                .preferredDifficulty(1)
                .build();

        AnalyzedCourseVO javaCourse = AnalyzedCourseVO.builder()
                .courseId(4L)
                .courseName("Java SpringBoot 核心技术精讲")
                .difficultyLevel(1)
                .coreKnowledgePoints(Arrays.asList("Java", "SpringBoot", "IoC", "AOP"))
                .prerequisiteSkills(List.of("Java基础"))
                .matchScore(95.0)
                .build();

        AnalyzedCourseVO mysqlCourse = AnalyzedCourseVO.builder()
                .courseId(8L)
                .courseName("MySQL 数据库设计与 SQL 优化")
                .difficultyLevel(1)
                .coreKnowledgePoints(Arrays.asList("MySQL", "SQL优化", "B+树索引"))
                .prerequisiteSkills(List.of("计算机基础"))
                .matchScore(92.0)
                .build();

        AnalyzedCourseVO rustCourse = AnalyzedCourseVO.builder()
                .courseId(63L)
                .courseName("Rust 语言入门与所有权机制核心解析")
                .difficultyLevel(1)
                .coreKnowledgePoints(Arrays.asList("Rust", "所有权", "生命周期"))
                .prerequisiteSkills(List.of("C语言"))
                .matchScore(88.0)
                .build();

        AnalyzedCourseVO nlpCourse = AnalyzedCourseVO.builder()
                .courseId(225L)
                .courseName("中文分词、词向量 (Word2Vec) 与情感分析实战")
                .difficultyLevel(1)
                .coreKnowledgePoints(Arrays.asList("NLP", "Word2Vec", "Python"))
                .prerequisiteSkills(List.of("Python基础"))
                .matchScore(85.0)
                .build();

        AnalyzedCourseVO dataxCourse = AnalyzedCourseVO.builder()
                .courseId(254L)
                .courseName("DataX / Sqoop 异构数据源高效全量与增量同步")
                .difficultyLevel(2)
                .coreKnowledgePoints(Arrays.asList("DataX", "Sqoop", "大数据同步"))
                .prerequisiteSkills(Arrays.asList("Java", "Linux"))
                .matchScore(82.0)
                .build();

        List<AnalyzedCourseVO> candidates = Arrays.asList(javaCourse, mysqlCourse, rustCourse, nlpCourse, dataxCourse);

        LearningPathPlan plan = agent.planPath(profile, candidates);

        assertThat(plan).isNotNull();
        List<Long> plannedIds = new ArrayList<>();
        for (PathStageVO stage : plan.getStages()) {
            for (AnalyzedCourseVO c : stage.getCourses()) {
                plannedIds.add(c.getCourseId());
            }
        }

        // 验证：严格包含 Java 和 MySQL 课程
        assertThat(plannedIds).contains(4L, 8L);
        // 验证：非对口学科孤岛课程（Rust、NLP、DataX）被全部剔除
        assertThat(plannedIds).doesNotContain(63L, 225L, 254L);
    }

    @Test
    void testPlanPathKahnDagOrdering() {
        UserProfileContext profile = UserProfileContext.builder()
                .userId(1001L)
                .intendedRole("Java 后端开发工程师")
                .build();

        AnalyzedCourseVO javaBasic = AnalyzedCourseVO.builder()
                .courseId(1L)
                .courseName("Java 核心编程基础")
                .difficultyLevel(1)
                .coreKnowledgePoints(List.of("Java基础"))
                .matchScore(90.0)
                .build();

        AnalyzedCourseVO springBoot = AnalyzedCourseVO.builder()
                .courseId(2L)
                .courseName("Spring Boot 微服务开发")
                .difficultyLevel(2)
                .coreKnowledgePoints(List.of("SpringBoot"))
                .prerequisiteSkills(List.of("Java核心编程基础"))
                .matchScore(92.0)
                .build();

        LearningPathPlan plan = agent.planPath(profile, Arrays.asList(springBoot, javaBasic));

        assertThat(plan).isNotNull();
        List<Long> orderedIds = new ArrayList<>();
        for (PathStageVO stage : plan.getStages()) {
            for (AnalyzedCourseVO c : stage.getCourses()) {
                orderedIds.add(c.getCourseId());
            }
        }

        assertThat(orderedIds).containsExactly(1L, 2L);
    }
}
