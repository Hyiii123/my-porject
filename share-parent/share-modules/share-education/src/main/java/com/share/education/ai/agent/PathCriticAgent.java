package com.share.education.ai.agent;

import com.share.education.ai.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 智能体 6：审判反思 Agent (PathCriticAgent)。
 *
 * <p>【核心职责与量化审判体系】：
 * 1. 独立审计 PathPlanningAgent 产出的学习路径方案；
 * 2. 严格核查 DAG 拓扑无环与先修时序无倒置；
 * 3. 评估布鲁姆认知难度阶梯平滑度与各阶段学时/课程分布均衡度；
 * 4. 产出量化 CriticReport，未达标时下发针对性修正指令，驱动编排器执行闭环自省回溯。</p>
 */
@Component
public class PathCriticAgent {

    private static final Logger log = LoggerFactory.getLogger(PathCriticAgent.class);

    private static final int PASS_THRESHOLD = 80;

    /**
     * 针对生成的学习路径执行全方位质检审判
     *
     * @param plan 学习路径规划
     * @param profile 学员画像上下文
     * @return 结构化质检评估报告
     */
    public CriticReport auditPathPlan(LearningPathPlan plan, UserProfileContext profile) {
        if (plan == null || plan.getStages() == null || plan.getStages().isEmpty()) {
            return CriticReport.builder()
                .passed(false)
                .overallScore(40)
                .prerequisiteScore(50)
                .smoothnessScore(40)
                .balanceScore(30)
                .verdictLevel("待修正 (C)")
                .critiqueNotes(List.of("规划内容为空，无可用进阶阶段"))
                .detectedAnomalies(List.of("路径阶段未有效生成"))
                .refinementDirectives(Map.of("needFullReplan", true))
                .build();
        }

        List<String> critiqueNotes = new ArrayList<>();
        List<String> anomalies = new ArrayList<>();
        Map<String, Object> directives = new HashMap<>();

        // 1. 维度一：先修拓扑合规度 (Prerequisite Score, 满分 100)
        int prereqScore = evaluatePrerequisites(plan, critiqueNotes, anomalies, directives);

        // 2. 维度二：认知阶梯平滑度 (Smoothness Score, 满分 100)
        int smoothnessScore = evaluateSmoothness(plan, profile, critiqueNotes, anomalies, directives);

        // 3. 维度三：阶段容量均衡度 (Balance Score, 满分 100)
        int balanceScore = evaluateBalance(plan, critiqueNotes, anomalies, directives);

        // 综合打分：40% 拓扑合规 + 35% 认知平滑 + 25% 阶段均衡
        int overallScore = (int) Math.round(0.40 * prereqScore + 0.35 * smoothnessScore + 0.25 * balanceScore);
        overallScore = Math.max(0, Math.min(100, overallScore));

        boolean passed = overallScore >= PASS_THRESHOLD;

        String verdictLevel;
        if (overallScore >= 95) {
            verdictLevel = "卓越 (A+)";
        } else if (overallScore >= 88) {
            verdictLevel = "优良 (A)";
        } else if (overallScore >= 80) {
            verdictLevel = "合格 (B)";
        } else {
            verdictLevel = "待优化 (C)";
        }

        if (passed) {
            critiqueNotes.add("综合质检通过：路径拓扑无明显倒置，认知阶梯平缓，符合工程化进阶标准。");
        } else {
            critiqueNotes.add(String.format("综合质检未通过（得分 %d < %d）：检测到先修或难度断层，触发编排器反思自省闭环。", overallScore, PASS_THRESHOLD));
        }

        log.info("[PathCriticAgent] 路径审判完成: 综合分={}, 拓扑分={}, 平滑分={}, 均衡分={}, 判定={}",
            overallScore, prereqScore, smoothnessScore, balanceScore, verdictLevel);

        return CriticReport.builder()
            .passed(passed)
            .overallScore(overallScore)
            .prerequisiteScore(prereqScore)
            .smoothnessScore(smoothnessScore)
            .balanceScore(balanceScore)
            .verdictLevel(verdictLevel)
            .critiqueNotes(critiqueNotes)
            .detectedAnomalies(anomalies)
            .refinementDirectives(directives)
            .build();
    }

    /**
     * 核查先修拓扑合规性 (杜绝后置课排在前面、前置课排在后面的倒置现象)
     */
    private int evaluatePrerequisites(LearningPathPlan plan,
                                      List<String> critiqueNotes,
                                      List<String> anomalies,
                                      Map<String, Object> directives) {
        int score = 100;
        List<PathStageVO> stages = plan.getStages();

        // 建立课程 ID 到其所处阶段索引 (0-based) 的映射
        Map<String, Integer> courseNameToStage = new HashMap<>();
        for (int i = 0; i < stages.size(); i++) {
            PathStageVO stage = stages.get(i);
            if (stage.getCourses() == null) continue;
            for (AnalyzedCourseVO c : stage.getCourses()) {
                if (c.getCourseName() != null) {
                    courseNameToStage.put(c.getCourseName().toLowerCase(), i);
                }
            }
        }

        int inversions = 0;
        for (int i = 0; i < stages.size(); i++) {
            PathStageVO stage = stages.get(i);
            if (stage.getCourses() == null) continue;

            for (AnalyzedCourseVO c : stage.getCourses()) {
                List<String> prereqs = c.getPrerequisiteSkills();
                if (prereqs == null) continue;

                for (String req : prereqs) {
                    if (!StringUtils.hasText(req)) continue;
                    String reqKey = req.toLowerCase();

                    // 检查先修课程是否被错误放置在后续阶段
                    for (Map.Entry<String, Integer> entry : courseNameToStage.entrySet()) {
                        if (entry.getKey().contains(reqKey) || reqKey.contains(entry.getKey())) {
                            int prereqStageIdx = entry.getValue();
                            if (prereqStageIdx > i) {
                                // 发现先修倒置！
                                inversions++;
                                String anomalyMsg = String.format("先修倒置：课程《%s》(阶段%d) 的前置知识《%s》被错误放置在后续阶段(阶段%d)",
                                    c.getCourseName(), i + 1, entry.getKey(), prereqStageIdx + 1);
                                anomalies.add(anomalyMsg);
                            }
                        }
                    }
                }
            }
        }

        if (inversions > 0) {
            score = Math.max(40, 100 - inversions * 18);
            directives.put("fixPrerequisiteInversion", true);
            critiqueNotes.add(String.format("拓扑核验：探测到 %d 处先修依赖时序异常，已提出拓扑重排修正指令。", inversions));
        } else {
            critiqueNotes.add("拓扑核验：全路径课程先修依赖严格满足 DAG 时序约束，无倒置回环。");
        }

        return score;
    }

    /**
     * 核查认知难度阶梯平滑度
     */
    private int evaluateSmoothness(LearningPathPlan plan,
                                   UserProfileContext profile,
                                   List<String> critiqueNotes,
                                   List<String> anomalies,
                                   Map<String, Object> directives) {
        int score = 100;
        List<PathStageVO> stages = plan.getStages();
        if (stages.isEmpty()) return 50;

        // 检查阶段 1 是否具备筑基能力 (难度 <= 2)
        PathStageVO stage1 = stages.get(0);
        boolean hasBeginnerCourse = false;
        if (stage1.getCourses() != null) {
            for (AnalyzedCourseVO c : stage1.getCourses()) {
                int diff = c.getDifficultyLevel() != null ? c.getDifficultyLevel() : 2;
                if (diff <= 2) {
                    hasBeginnerCourse = true;
                    break;
                }
            }
        }

        if (!hasBeginnerCourse) {
            score -= 25;
            anomalies.add("认知悬崖：阶段一缺乏筑基承接课程，整体难度过高可能导致弃学");
            directives.put("needMoreBeginnerCourses", true);
        }

        // 检查相邻阶段的平均难度是否存在断崖式跳跃 (> 1.5 级跨度)
        double prevDiff = 1.5;
        for (int i = 0; i < stages.size(); i++) {
            PathStageVO s = stages.get(i);
            if (s.getCourses() == null || s.getCourses().isEmpty()) continue;

            double avgDiff = s.getCourses().stream()
                .mapToInt(c -> c.getDifficultyLevel() != null ? c.getDifficultyLevel() : 2)
                .average()
                .orElse(2.0);

            if (i > 0 && (avgDiff - prevDiff) > 1.4) {
                score -= 15;
                anomalies.add(String.format("阶梯过陡：阶段%d 到 阶段%d 难度跳跃过大 (%.1f -> %.1f)", i, i + 1, prevDiff, avgDiff));
                directives.put("smoothDifficultyTransition", true);
            }
            prevDiff = avgDiff;
        }

        if (score >= 90) {
            critiqueNotes.add("阶梯评估：难度梯级平滑递进（筑基 ➔ 进阶 ➔ 架构 ➔ 突破），符合布鲁姆认知跃迁模型。");
        }

        return Math.max(45, score);
    }

    /**
     * 核查阶段容量与课时分布均衡度
     */
    private int evaluateBalance(LearningPathPlan plan,
                                List<String> critiqueNotes,
                                List<String> anomalies,
                                Map<String, Object> directives) {
        int score = 100;
        List<PathStageVO> stages = plan.getStages();

        int emptyStageCount = 0;
        int maxCourseInSingleStage = 0;

        for (PathStageVO s : stages) {
            int courseCount = s.getCourses() != null ? s.getCourses().size() : 0;
            if (courseCount == 0) {
                emptyStageCount++;
            }
            if (courseCount > maxCourseInSingleStage) {
                maxCourseInSingleStage = courseCount;
            }
        }

        if (emptyStageCount > 0) {
            score -= (emptyStageCount * 20);
            anomalies.add(String.format("结构畸变：存在 %d 个完全空白的进阶里程碑阶段", emptyStageCount));
            directives.put("rebalanceStageDistribution", true);
        }

        if (maxCourseInSingleStage > 6) {
            score -= 15;
            anomalies.add(String.format("局部过载：单个阶段聚合课程过多 (%d门)，易造成单阶段学习疲倦", maxCourseInSingleStage));
            directives.put("splitHeavyStage", true);
        }

        if (score >= 90) {
            critiqueNotes.add("容量评估：各进阶阶段课时体量与课程数量分布均衡，学时密度合理。");
        }

        return Math.max(50, score);
    }
}
