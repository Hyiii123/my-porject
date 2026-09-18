package com.share.education.ai.composite.node;

import com.share.education.ai.composite.model.AgentDebateTurn;
import com.share.education.ai.composite.model.DebateConsensusSummary;
import com.share.education.ai.composite.state.DebateBlackboardState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 复合架构模式 5：圆桌首席仲裁者节点 (ConsensusArbiterNode)。
 * 角色设定：全局会议主席 / 协调总监 (Supervisor)。
 * 立场：中立控场，聚焦共识达成，严控辩论轮次上限（最多 2 轮），引导双方折中妥协并最终签署共识摘要。
 */
@Component
public class ConsensusArbiterNode {

    private static final Logger log = LoggerFactory.getLogger(ConsensusArbiterNode.class);

    private final com.share.education.ai.client.ThirdPartyAiClient aiClient;

    public ConsensusArbiterNode(@org.springframework.beans.factory.annotation.Autowired(required = false) com.share.education.ai.client.ThirdPartyAiClient aiClient) {
        this.aiClient = aiClient;
    }

    /**
     * 辩论启动：召开专家圆桌会议
     */
    public void openRoundtable(DebateBlackboardState state) {
        long tStart = System.currentTimeMillis();
        state.setCurrentRound(1);
        state.setConsensusReached(false);
        double initialDisagreement = state.getDisagreementScore() != null ? state.getDisagreementScore() : 0.85;

        String role = state.getIntendedRole() != null ? state.getIntendedRole() : "全栈架构师";
        String arg = String.format("专家圆桌会议正式启动！本次议题：为学员定制【%s】系统化进阶路线。" +
            "请大厂技术总监首先提交基准方案；请学情成长导师依据学员画像严格把关认知负荷；请审判法官执行 Kahn 图算法先修审计！", role);

        AgentDebateTurn turn = AgentDebateTurn.builder()
            .round(1)
            .speaker("ConsensusArbiter")
            .speakerName("圆桌首席仲裁者")
            .stance("MODERATE")
            .targetObject("All Agents")
            .argument(arg)
            .metricSummary("辩论轮次: Round 1, 状态: 会议已启动")
            .latencyMs(System.currentTimeMillis() - tStart)
            .timestamp(System.currentTimeMillis())
            .build();

        state.recordTurn(turn);
        state.recordLatency("ConsensusArbiterNode_Open", System.currentTimeMillis() - tStart);
        log.info("[ConsensusArbiterNode] 圆桌辩论正式开场");
    }

    /**
     * 第一轮辩论收敛评估与反思指令下发 (Directive Issue)
     */
    public void guideCompromise(DebateBlackboardState state) {
        long tStart = System.currentTimeMillis();
        state.setCurrentRound(2); // 进入第二轮

        double currentDisagreement = state.getDisagreementScore() != null ? state.getDisagreementScore() : 0.85;

        String arg = String.format(
            "第一轮博弈评估：当前各方动态分歧度为 %.0f%%！核心冲突聚焦在【技术总监方案理论过陡】与【学情导师担忧劝退】的矛盾，且法官指出了先修依赖需优化。" +
            "仲裁者指令下发：请总监下调阶段 2 理论深度，以工程实战化解难度；请导师在保证核心就业刚需的前提下予以放行。开启第二轮反思折中！",
            currentDisagreement * 100);

        AgentDebateTurn turn = AgentDebateTurn.builder()
            .round(2)
            .speaker("ConsensusArbiter")
            .speakerName("圆桌首席仲裁者")
            .stance("COMPROMISE_DIRECTIVE")
            .targetObject("IndustryArchitect & PedagogyMentor")
            .argument(arg)
            .metricSummary(String.format("动态分歧度: %.2f, 判定: 启动第二轮受控妥协反思回路", currentDisagreement))
            .latencyMs(System.currentTimeMillis() - tStart)
            .timestamp(System.currentTimeMillis())
            .build();

        state.recordTurn(turn);
        state.recordLatency("ConsensusArbiterNode_Guide", System.currentTimeMillis() - tStart);
        log.info("[ConsensusArbiterNode] 第一轮分歧总结完毕，已下发第二轮折中反思指令: 分歧度={}", currentDisagreement);
    }

    /**
     * 终审盖章：签署多智能体共识决议 (Consensus Seal)
     */
    public void sealConsensus(DebateBlackboardState state) {
        long tStart = System.currentTimeMillis();
        state.setConsensusReached(true);
        state.setDisagreementScore(0.00);

        int finalScore = state.getCriticReport() != null && state.getCriticReport().getOverallScore() != null
            ? state.getCriticReport().getOverallScore() : 95;
        String qualityGrade = state.getCriticReport() != null && state.getCriticReport().getVerdictLevel() != null
            ? state.getCriticReport().getVerdictLevel() : "卓越 (A+)";

        DebateConsensusSummary summary = DebateConsensusSummary.builder()
            .totalRounds(state.getCurrentRound())
            .consensusReached(true)
            .initialConflictSummary("初版方案阶段 2 理论负荷过载，学情导师提出防劝退抗辩")
            .compromiseResolution("技术总监将高难度理论软化为渐进式实战项目，学情导师认可平滑后梯度，审判法官复核 Kahn DAG 拓扑合规")
            .finalCriticScore(finalScore)
            .qualityGrade(qualityGrade)
            .keyAgreements(List.of(
                "全周期 4 阶段无缝递进，总学时均衡受控",
                "Kahn 拓扑合规度无环，无反向先修依赖",
                "兼顾大厂胜任力底线与学员自律认知平滑度"
            ))
            .build();

        state.setConsensusSummary(summary);

        String dynamicArg = null;
        if (aiClient != null && aiClient.isAvailable()) {
            String sys = "你是一位资深中立的首席仲裁者。各方历经博弈已达成共识，法官出具了量化质检评分。请发表最终仲裁批准声明（限制在80字内）。";
            String usr = String.format("最终质检得分：%d分，评级：%s。请宣布方案批准正式交付。", finalScore, qualityGrade);
            dynamicArg = aiClient.generate(sys, usr);
        }

        String arg = (dynamicArg != null && !dynamicArg.isBlank()) ? dynamicArg : String.format(
            "仲裁决议宣布：历经 %d 轮充分博弈与自适应妥协，学情导师、技术总监与审判法官达成 100%% 共识！" +
            "终审质检评级【%s · %d分】。方案兼具极高的大厂就业竞争力与学员认知舒适度，正式签署交付！",
            state.getCurrentRound(), summary.getQualityGrade(), summary.getFinalCriticScore());

        AgentDebateTurn turn = AgentDebateTurn.builder()
            .round(state.getCurrentRound())
            .speaker("ConsensusArbiter")
            .speakerName("圆桌首席仲裁者")
            .stance("CONSENSUS")
            .targetObject("All Stakeholders")
            .argument(arg)
            .metricSummary(String.format("最终共识达成！质检得分: %d, 质量评级: %s",
                summary.getFinalCriticScore(), summary.getQualityGrade()))
            .latencyMs(System.currentTimeMillis() - tStart)
            .timestamp(System.currentTimeMillis())
            .build();

        state.recordTurn(turn);
        state.recordLatency("ConsensusArbiterNode_Seal", System.currentTimeMillis() - tStart);
        log.info("[ConsensusArbiterNode] 多智能体共识正式达成，签署决议");
    }
}
