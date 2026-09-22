package com.share.education.ai.composite.state;

import com.share.education.ai.composite.model.AgentDebateTurn;
import com.share.education.ai.composite.model.DebateConsensusSummary;
import com.share.education.ai.composite.model.PlanPatch;
import com.share.education.ai.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多智能体复合图（模式2 + 模式5 + 模式4）核心状态机载体：共享辩论黑板 (DebateBlackboardState)。
 * 承载从路由分发、圆桌辩论、方案补丁应用、Kahn 拓扑反思质检到最终交付的全生命周期增量状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebateBlackboardState {

    /** 会话唯一编号 */
    private String sessionId;

    /** 学员用户 ID (访客为 null) */
    private Long userId;

    /** 目标岗位方向 (如 "Java全栈架构师", "大语言模型应用工程师") */
    private String intendedRole;

    /** 动态意图路由决议 (IntentDispatcherNode 产出) */
    private String routedIntent;

    /** 冷启动探针回答字典 */
    @Builder.Default
    private Map<String, String> probeAnswers = Collections.emptyMap();

    /** 自定义人机协同微调参数 */
    @Builder.Default
    private Map<String, Object> customOverrides = Collections.emptyMap();

    /** 工作流启动毫秒时间戳 */
    private Long startTime;

    // --- 智能体输入数据与预处理资产 ---
    /** 学员多维认知画像 (PedagogyMentorAgent 产出或自适应校准) */
    private UserProfileContext userProfile;

    /** 算法初筛候选课程池 (RecommendationAgent / Python 深度模型产出) */
    @Builder.Default
    private List<CandidateCourseDTO> candidateCourses = Collections.emptyList();

    /** 布鲁姆大纲认知解构课程列表 */
    @Builder.Default
    private List<AnalyzedCourseVO> analyzedCourses = Collections.emptyList();

    // --- 动态辩论流转状态 (模式 5 核心) ---
    /** 当前由各专家智能体讨论并迭代修改的路线草案 */
    private LearningPathPlan currentDraftPlan;

    /** 圆桌辩论多轮对话发言历史总线 */
    @Builder.Default
    private List<AgentDebateTurn> dialogueTurns = new ArrayList<>();

    /** 当前辩论轮次 (1 ~ 2) */
    @Builder.Default
    private Integer currentRound = 1;

    /** 当前各方分歧度指数 (0.00: 达成完全共识 ~ 1.00: 极端冲突) */
    @Builder.Default
    private Double disagreementScore = 0.85;

    /** 是否已达成三方共识 */
    @Builder.Default
    private Boolean consensusReached = false;

    /** 审判法官下发的强制修正指令集 (模式 4) */
    @Builder.Default
    private Map<String, Object> refinementDirectives = new HashMap<>();

    /** 审判法官生成的最新质检评估报告 */
    private CriticReport criticReport;

    /** 共识达成终审摘要 */
    private DebateConsensusSummary consensusSummary;

    // --- 终局交付成果 ---
    /** 最终生成的个性化推荐必修课程集合 (带可解释性理由) */
    @Builder.Default
    private List<PersonalizedRecommendVO> finalRecommendations = Collections.emptyList();

    /** 各节点耗时监控 (ms) */
    @Builder.Default
    private Map<String, Long> nodeLatencies = new ConcurrentHashMap<>();

    /**
     * 记录一轮发言并同步更新黑板
     */
    public synchronized void recordTurn(AgentDebateTurn turn) {
        if (this.dialogueTurns == null) {
            this.dialogueTurns = new ArrayList<>();
        }
        this.dialogueTurns.add(turn);
        if (turn.getProposedPatch() != null) {
            applyPatch(turn.getProposedPatch());
        }
    }

    /**
     * 将智能体提出的方案修改补丁应用至当前草案中 (Patching)
     */
    public synchronized void applyPatch(PlanPatch patch) {
        if (patch == null || this.currentDraftPlan == null || this.currentDraftPlan.getStages() == null) {
            return;
        }
        int targetStage = patch.getTargetStageIndex() != null ? patch.getTargetStageIndex() : 2;

        List<PathStageVO> stages = this.currentDraftPlan.getStages();
        for (PathStageVO stage : stages) {
            if (Objects.equals(stage.getStageIndex(), targetStage)) {
                List<AnalyzedCourseVO> stageCourses = new ArrayList<>(stage.getCourses());

                // 1. 移除指定课程
                if (patch.getRemoveCourseIds() != null && !patch.getRemoveCourseIds().isEmpty()) {
                    stageCourses.removeIf(c -> patch.getRemoveCourseIds().contains(c.getCourseId()));
                }

                // 2. 插入新过渡课程 (全局去重：确保整套培养方案中不出现重复课程)
                if (patch.getInsertCourses() != null && !patch.getInsertCourses().isEmpty()) {
                    for (AnalyzedCourseVO ins : patch.getInsertCourses()) {
                        boolean existsInPlan = stages.stream()
                            .filter(s -> s.getCourses() != null)
                            .flatMap(s -> s.getCourses().stream())
                            .anyMatch(c -> c.getCourseId().equals(ins.getCourseId()));
                        if (!existsInPlan) {
                            stageCourses.add(ins);
                        }
                    }
                }

                // 3. 更新该阶段课程与学时
                stage.setCourses(stageCourses);
                if (patch.getHoursAdjustment() != null && stage.getEstimatedHours() != null) {
                    stage.setEstimatedHours(Math.max(15, stage.getEstimatedHours() + patch.getHoursAdjustment()));
                }
            }
        }

        // 重新核算总课程数与总学时
        int totalCourses = stages.stream().mapToInt(s -> s.getCourses() != null ? s.getCourses().size() : 0).sum();
        int totalHours = stages.stream().mapToInt(s -> s.getEstimatedHours() != null ? s.getEstimatedHours() : 30).sum();
        this.currentDraftPlan.setTotalCourses(totalCourses);
        this.currentDraftPlan.setTotalEstimatedHours(totalHours);
    }

    /**
     * 记录节点耗时
     */
    public void recordLatency(String nodeName, long latencyMs) {
        if (this.nodeLatencies == null) {
            this.nodeLatencies = new ConcurrentHashMap<>();
        }
        this.nodeLatencies.put(nodeName, latencyMs);
    }
}
