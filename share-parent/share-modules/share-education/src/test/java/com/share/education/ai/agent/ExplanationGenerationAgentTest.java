package com.share.education.ai.agent;

import com.share.education.ai.client.ThirdPartyAiClient;
import com.share.education.ai.model.AnalyzedCourseVO;
import com.share.education.ai.model.LearningPathPlan;
import com.share.education.ai.model.PathStageVO;
import com.share.education.ai.model.PersonalizedRecommendVO;
import com.share.education.ai.model.UserProfileContext;
import com.share.education.ai.rag.EducationKnowledgeRAG;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ExplanationGenerationAgentTest {

    private ThirdPartyAiClient aiClient;
    private EducationKnowledgeRAG knowledgeRAG;
    private ExplanationGenerationAgent agent;

    @BeforeEach
    void setUp() {
        aiClient = Mockito.mock(ThirdPartyAiClient.class);
        knowledgeRAG = Mockito.mock(EducationKnowledgeRAG.class);
        when(aiClient.isAvailable()).thenReturn(false); // 强制走本地可解释性规则引擎降级
        agent = new ExplanationGenerationAgent(aiClient, knowledgeRAG);
    }

    @Test
    void testExplanationGenerationAbolishesMechanicalProfileSkillTemplate() {
        // 用户画像：之前历史技能中遗留了 Vue3，目标岗位是 Java 后端
        UserProfileContext profile = UserProfileContext.builder()
                .userId(1001L)
                .intendedRole("Java 后端开发工程师 (日常实习/校招)")
                .topSkills(Arrays.asList("Vue3", "HTML", "CSS"))
                .build();

        AnalyzedCourseVO mysqlCourse = AnalyzedCourseVO.builder()
                .courseId(8L)
                .courseName("MySQL 数据库设计与 SQL 优化")
                .difficultyLevel(1)
                .coreKnowledgePoints(Arrays.asList("MySQL", "SQL优化", "B+树索引"))
                .matchScore(95.0)
                .build();

        PathStageVO stage1 = PathStageVO.builder()
                .stageIndex(1)
                .stageName("阶段一：核心基石与工程化筑基")
                .courses(Collections.singletonList(mysqlCourse))
                .build();

        LearningPathPlan plan = LearningPathPlan.builder()
                .intendedRole(profile.getIntendedRole())
                .stages(Collections.singletonList(stage1))
                .build();

        List<PersonalizedRecommendVO> recommends = agent.generateExplanations(profile, plan, 5);

        assertThat(recommends).hasSize(1);
        PersonalizedRecommendVO rec = recommends.get(0);
        assertThat(rec.getId()).isEqualTo(8L);

        // 核心断言：绝对不能出现 "Vue3" 机械污染！
        assertThat(rec.getRecommendReason()).doesNotContain("Vue3");
        assertThat(rec.getRecommendReason()).doesNotContain("Vue");

        // 核心断言：推荐理由基于课程自身的核心技能（MySQL、SQL优化）与课程名自洽生成
        assertThat(rec.getRecommendReason()).contains("MySQL");
        assertThat(rec.getSkillGapFilled()).isEqualTo("MySQL、SQL优化、B+树索引");
    }

    @Test
    void testExplanationWithEvidencePath() {
        UserProfileContext profile = UserProfileContext.builder()
                .userId(1001L)
                .intendedRole("Java 后端开发工程师")
                .topSkills(List.of("Java"))
                .build();

        AnalyzedCourseVO springCourse = AnalyzedCourseVO.builder()
                .courseId(4L)
                .courseName("Java SpringBoot 核心技术精讲")
                .difficultyLevel(2)
                .coreKnowledgePoints(Arrays.asList("SpringBoot", "IoC", "AOP"))
                .evidencePaths(Collections.singletonList("Java 基础 -> SpringBoot 核心"))
                .matchScore(96.0)
                .build();

        PathStageVO stage2 = PathStageVO.builder()
                .stageIndex(2)
                .stageName("阶段二：核心技术进阶与组件精通")
                .courses(Collections.singletonList(springCourse))
                .build();

        LearningPathPlan plan = LearningPathPlan.builder()
                .intendedRole(profile.getIntendedRole())
                .stages(Collections.singletonList(stage2))
                .build();

        List<PersonalizedRecommendVO> recommends = agent.generateExplanations(profile, plan, 5);

        assertThat(recommends).hasSize(1);
        PersonalizedRecommendVO rec = recommends.get(0);
        assertThat(rec.getRecommendReason()).contains("先修拓扑链路");
        assertThat(rec.getRecommendReason()).contains("SpringBoot");
    }
}
