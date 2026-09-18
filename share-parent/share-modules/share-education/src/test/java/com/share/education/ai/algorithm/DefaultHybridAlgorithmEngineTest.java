package com.share.education.ai.algorithm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.education.ai.model.AlgorithmCandidateDTO;
import com.share.education.ai.model.UserProfileContext;
import com.share.education.domain.EduCourse;
import com.share.education.mapper.EduCourseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DefaultHybridAlgorithmEngineTest {

    private EduCourseMapper courseMapper;
    private DefaultHybridAlgorithmEngine engine;

    @BeforeEach
    void setUp() {
        courseMapper = Mockito.mock(EduCourseMapper.class);
        engine = new DefaultHybridAlgorithmEngine(courseMapper);
    }

    @Test
    void testResolveDomain() {
        assertThat(DefaultHybridAlgorithmEngine.resolveDomain("Java 后端开发工程师 (日常实习/校招)"))
                .isEqualTo(DefaultHybridAlgorithmEngine.DisciplineDomain.JAVA_BACKEND);
        assertThat(DefaultHybridAlgorithmEngine.resolveDomain("Java 后端开发工程师"))
                .isEqualTo(DefaultHybridAlgorithmEngine.DisciplineDomain.JAVA_BACKEND);
        assertThat(DefaultHybridAlgorithmEngine.resolveDomain("Java 全栈架构师"))
                .isEqualTo(DefaultHybridAlgorithmEngine.DisciplineDomain.JAVA_BACKEND);
        assertThat(DefaultHybridAlgorithmEngine.resolveDomain("Web 前端架构专家"))
                .isEqualTo(DefaultHybridAlgorithmEngine.DisciplineDomain.FRONTEND);
        assertThat(DefaultHybridAlgorithmEngine.resolveDomain("大数据流批一体工程师"))
                .isEqualTo(DefaultHybridAlgorithmEngine.DisciplineDomain.BIG_DATA);
        assertThat(DefaultHybridAlgorithmEngine.resolveDomain("大语言模型应用工程师"))
                .isEqualTo(DefaultHybridAlgorithmEngine.DisciplineDomain.AI_LLM);
        assertThat(DefaultHybridAlgorithmEngine.resolveDomain("Go 云原生架构师"))
                .isEqualTo(DefaultHybridAlgorithmEngine.DisciplineDomain.GO_CLOUD_NATIVE);
        assertThat(DefaultHybridAlgorithmEngine.resolveDomain("移动与跨端开发工程师"))
                .isEqualTo(DefaultHybridAlgorithmEngine.DisciplineDomain.MOBILE);
    }

    @Test
    void testIsCourseAllowedForDomain_JavaBackend() {
        DefaultHybridAlgorithmEngine.DisciplineDomain domain = DefaultHybridAlgorithmEngine.DisciplineDomain.JAVA_BACKEND;

        // 1. 合法 Java 后端课程
        EduCourse javaCourse = new EduCourse();
        javaCourse.setId(4L);
        javaCourse.setCategoryId(2L);
        javaCourse.setCourseName("Java SpringBoot 实战");
        javaCourse.setSkills("Java,SpringBoot,微服务,MyBatis");
        javaCourse.setTargetRole("Java架构师");
        assertThat(DefaultHybridAlgorithmEngine.isCourseAllowedForDomain(javaCourse, domain)).isTrue();

        // 2. 合法数据库课程
        EduCourse dbCourse = new EduCourse();
        dbCourse.setId(8L);
        dbCourse.setCategoryId(4L);
        dbCourse.setCourseName("MySQL 数据库优化");
        dbCourse.setSkills("MySQL,Redis,索引优化");
        dbCourse.setTargetRole("数据库开发工程师");
        assertThat(DefaultHybridAlgorithmEngine.isCourseAllowedForDomain(dbCourse, domain)).isTrue();

        // 3. 强制过滤 Rust 课程 (即便分类为 2 后端开发)
        EduCourse rustCourse = new EduCourse();
        rustCourse.setId(63L);
        rustCourse.setCategoryId(2L);
        rustCourse.setCourseName("Rust 语言入门与所有权机制核心解析");
        rustCourse.setSkills("Rust,系统编程,内存安全");
        rustCourse.setTargetRole("Rust开发工程师");
        assertThat(DefaultHybridAlgorithmEngine.isCourseAllowedForDomain(rustCourse, domain)).isFalse();

        // 4. 强制过滤 大数据 DataX / Sqoop (分类 7)
        EduCourse dataxCourse = new EduCourse();
        dataxCourse.setId(254L);
        dataxCourse.setCategoryId(7L);
        dataxCourse.setCourseName("DataX / Sqoop 异构数据源高效全量与增量同步");
        dataxCourse.setSkills("Java,MySQL,Linux");
        dataxCourse.setTargetRole("数据开发工程师");
        assertThat(DefaultHybridAlgorithmEngine.isCourseAllowedForDomain(dataxCourse, domain)).isFalse();

        // 5. 强制过滤 NLP Word2Vec (分类 6)
        EduCourse nlpCourse = new EduCourse();
        nlpCourse.setId(225L);
        nlpCourse.setCategoryId(6L);
        nlpCourse.setCourseName("中文分词、词向量 (Word2Vec) 与情感分析实战");
        nlpCourse.setSkills("NLP,Python,机器学习");
        nlpCourse.setTargetRole("NLP工程师");
        assertThat(DefaultHybridAlgorithmEngine.isCourseAllowedForDomain(nlpCourse, domain)).isFalse();

        // 6. 强制过滤 Vue3 前端课程 (分类 1)
        EduCourse vueCourse = new EduCourse();
        vueCourse.setId(21L);
        vueCourse.setCategoryId(1L);
        vueCourse.setCourseName("Vue3 企业级高可用中后台管理系统实战");
        vueCourse.setSkills("Vue3,TypeScript,Pinia");
        vueCourse.setTargetRole("前端开发工程师");
        assertThat(DefaultHybridAlgorithmEngine.isCourseAllowedForDomain(vueCourse, domain)).isFalse();

        // 7. 强制过滤 Go 语言课程 (即便分类为 2)
        EduCourse goCourse = new EduCourse();
        goCourse.setId(71L);
        goCourse.setCategoryId(2L);
        goCourse.setCourseName("Go 语言 gRPC + Protobuf 高性能 RPC 框架实战");
        goCourse.setSkills("Go,gRPC,Protobuf,微服务");
        goCourse.setTargetRole("Go后端工程师");
        assertThat(DefaultHybridAlgorithmEngine.isCourseAllowedForDomain(goCourse, domain)).isFalse();

        // 8. 强制过滤 HarmonyOS 课程 (分类 3)
        EduCourse harmonyCourse = new EduCourse();
        harmonyCourse.setId(120L);
        harmonyCourse.setCategoryId(3L);
        harmonyCourse.setCourseName("HarmonyOS NEXT 数据库访问与分布式数据对象");
        harmonyCourse.setSkills("HarmonyOS,ArkTS,分布式数据库");
        harmonyCourse.setTargetRole("鸿蒙开发工程师");
        assertThat(DefaultHybridAlgorithmEngine.isCourseAllowedForDomain(harmonyCourse, domain)).isFalse();

        // 9. 强制过滤 Kubernetes DevOps 课程 (分类 5)
        EduCourse k8sCourse = new EduCourse();
        k8sCourse.setId(186L);
        k8sCourse.setCategoryId(5L);
        k8sCourse.setCourseName("Kubernetes RBAC 权限控制与多租户资源配额实践");
        k8sCourse.setSkills("Kubernetes,Docker,DevOps");
        k8sCourse.setTargetRole("云原生运维开发");
        assertThat(DefaultHybridAlgorithmEngine.isCourseAllowedForDomain(k8sCourse, domain)).isFalse();
    }

    @Test
    void testRecallCandidatesStrictDisciplineIsolation() {
        EduCourse c1 = new EduCourse();
        c1.setId(4L);
        c1.setCategoryId(2L);
        c1.setCourseName("Java SpringBoot 实战");
        c1.setSkills("Java,SpringBoot,MySQL");
        c1.setTargetRole("Java开发工程师");
        c1.setLearnerCount(15000);
        c1.setDifficultyLevel(1);

        EduCourse c2 = new EduCourse();
        c2.setId(8L);
        c2.setCategoryId(4L);
        c2.setCourseName("MySQL 数据库优化");
        c2.setSkills("MySQL,SQL优化");
        c2.setTargetRole("数据库开发工程师");
        c2.setLearnerCount(12000);
        c2.setDifficultyLevel(1);

        EduCourse c3 = new EduCourse();
        c3.setId(63L);
        c3.setCategoryId(2L);
        c3.setCourseName("Rust 语言入门与所有权机制核心解析");
        c3.setSkills("Rust,系统编程");
        c3.setTargetRole("Rust开发工程师");
        c3.setLearnerCount(36000); // 即使学习人数极高
        c3.setDifficultyLevel(1);

        EduCourse c4 = new EduCourse();
        c4.setId(225L);
        c4.setCategoryId(6L);
        c4.setCourseName("中文分词、词向量 (Word2Vec) 与情感分析实战");
        c4.setSkills("NLP,Python");
        c4.setTargetRole("NLP工程师");
        c4.setLearnerCount(35000);
        c4.setDifficultyLevel(1);

        EduCourse c5 = new EduCourse();
        c5.setId(254L);
        c5.setCategoryId(7L);
        c5.setCourseName("DataX / Sqoop 异构数据源高效全量与增量同步");
        c5.setSkills("Java,MySQL,Linux");
        c5.setTargetRole("数据开发工程师");
        c5.setLearnerCount(20000);
        c5.setDifficultyLevel(1);

        when(courseMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(c1, c2, c3, c4, c5));

        UserProfileContext profile = UserProfileContext.builder()
                .userId(1001L)
                .intendedRole("Java 后端开发工程师 (日常实习/校招)")
                .preferredDifficulty(1)
                .skillWeights(Map.of("C", 80))
                .build();

        List<AlgorithmCandidateDTO> recalled = engine.recallCandidates(1001L, profile, 4);

        assertThat(recalled).isNotEmpty();
        List<Long> recalledIds = recalled.stream().map(AlgorithmCandidateDTO::getCourseId).toList();
        assertThat(recalledIds).contains(4L, 8L);
        assertThat(recalledIds).doesNotContain(63L, 225L, 254L);
    }
}
