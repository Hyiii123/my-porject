package com.share.customer.service.support;

import com.share.customer.service.support.action.CustomerActionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerActionCardAssemblerTest {

    private CustomerSecurityShield securityShield;
    private CustomerActionFactory actionFactory;
    private CustomerActionCardAssembler assembler;

    @BeforeEach
    void setUp() {
        securityShield = Mockito.mock(CustomerSecurityShield.class);
        actionFactory = Mockito.mock(CustomerActionFactory.class);
        assembler = new CustomerActionCardAssembler(securityShield, actionFactory);
    }

    @Test
    void testIsAgentDeliberationIntent() {
        String query = "老师，我是计算机大二的，目前只学过 C 语言，想明年暑假找一份大厂后端日常实习，但我自律性一般，怕学不会被劝退，能帮我制定一个学习路线吗？";
        assertThat(assembler.isAgentDeliberationIntent(query)).isTrue();

        String refundQuery = "我想退款，课程不合适";
        assertThat(assembler.isAgentDeliberationIntent(refundQuery)).isFalse();
    }

    @Test
    void testExtractTargetRoleAndDifficultyForSophomoreInternship() {
        String query = "老师，我是计算机大二的，目前只学过 C 语言，想明年暑假找一份大厂后端日常实习，但我自律性一般，怕学不会被劝退，能帮我制定一个学习路线吗？";

        String role = assembler.extractTargetRole(query);
        assertThat(role).isEqualTo("Java 后端开发工程师 (日常实习/校招)");

        Integer difficulty = assembler.extractTargetDifficulty(query);
        assertThat(difficulty).isEqualTo(1);
    }

    @Test
    void testExtractOtherRolesAndDifficulties() {
        assertThat(assembler.extractTargetRole("想学Vue3和前端架构"))
                .isEqualTo("Web 前端架构专家");
        assertThat(assembler.extractTargetRole("想转行大数据数仓与Flink开发"))
                .isEqualTo("大数据流批一体工程师");
        assertThat(assembler.extractTargetRole("想深入大语言模型应用和RAG知识库"))
                .isEqualTo("大语言模型应用工程师");
        assertThat(assembler.extractTargetRole("Go云原生与K8s实践"))
                .isEqualTo("Go 云原生架构师");

        assertThat(assembler.extractTargetDifficulty("Java百万QPS高并发架构师源码剖析"))
                .isEqualTo(3);
    }
}
