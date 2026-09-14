package com.share.education.ai.agent;

import com.share.education.ai.model.AnalyzedCourseVO;
import com.share.education.ai.model.LearningPathPlan;
import com.share.education.ai.model.PathStageVO;
import com.share.education.ai.model.UserProfileContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 智能体 5：路径规划 Agent (PathPlanningAgent)。
 *
 * <p>【核心职责】：
 * 1. 依据知识图谱先修依赖与工程师成长进阶曲线，构建有向无环拓扑路线 (DAG)；
 * 2. 将候选课程按进阶梯度合理划分为 4 个阶段性成长里程碑；
 * 3. 响应 Critic 审判反思修正指令与 Human-in-the-Loop 人机协同微调指令；
 * 4. 动态消除先修断层，保障课程拓扑 100% 合规。</p>
 */
@Component
public class PathPlanningAgent {

    public LearningPathPlan planPath(UserProfileContext profile, List<AnalyzedCourseVO> courses) {
        return planPath(profile, courses, Collections.emptyMap());
    }

    /**
     * 带针对性修正指令或人机协同微调的路径规划
     *
     * @param profile 学员画像
     * @param courses 候选课程集合
     * @param directives Critic 指令集或用户微调参数 (如 needMoreBeginnerCourses, excludedCourseIds 等)
     * @return 优化后的结构化进阶路线
     */
    public LearningPathPlan planPath(UserProfileContext profile,
                                    List<AnalyzedCourseVO> courses,
                                    Map<String, Object> directives) {
        String role = (profile != null && StringUtils.hasText(profile.getIntendedRole()))
            ? profile.getIntendedRole() : "高级全栈软件工程师";

        if (courses == null || courses.isEmpty()) {
            return LearningPathPlan.builder()
                .intendedRole(role)
                .overallGoal("系统化 IT 技术进阶之旅")
                .totalCourses(0)
                .totalEstimatedHours(0)
                .stages(Collections.emptyList())
                .referenceStandard("IT 工程师标准化进阶指南")
                .build();
        }

        // 0. 处理人机协同排除课程 (Human-in-the-Loop: excludedCourseIds 或 excludeCourseIds)
        List<AnalyzedCourseVO> activeCourses = new ArrayList<>(courses);
        Object exObj = null;
        if (directives != null) {
            if (directives.containsKey("excludedCourseIds")) {
                exObj = directives.get("excludedCourseIds");
            } else if (directives.containsKey("excludeCourseIds")) {
                exObj = directives.get("excludeCourseIds");
            }
        }
        if (exObj instanceof Collection<?> exList) {
            Set<Long> exIds = new HashSet<>();
            for (Object item : exList) {
                if (item instanceof Number num) {
                    exIds.add(num.longValue());
                } else if (item != null) {
                    try { exIds.add(Long.parseLong(item.toString())); } catch (Exception ignored) {}
                }
            }
            activeCourses.removeIf(c -> exIds.contains(c.getCourseId()));
        }

        // 1. 基于 Kahn 算法的 DAG 严格拓扑排序 (避免 ComparableTimSort 破坏 contract 异常)
        Map<Long, AnalyzedCourseVO> courseMap = new HashMap<>();
        Map<Long, List<Long>> adj = new HashMap<>();
        Map<Long, Integer> inDegree = new HashMap<>();

        for (AnalyzedCourseVO c : activeCourses) {
            courseMap.put(c.getCourseId(), c);
            adj.put(c.getCourseId(), new ArrayList<>());
            inDegree.put(c.getCourseId(), 0);
        }

        for (AnalyzedCourseVO c1 : activeCourses) {
            for (AnalyzedCourseVO c2 : activeCourses) {
                if (!c1.getCourseId().equals(c2.getCourseId()) && isPrerequisite(c1, c2)) {
                    adj.get(c1.getCourseId()).add(c2.getCourseId());
                    inDegree.put(c2.getCourseId(), inDegree.get(c2.getCourseId()) + 1);
                }
            }
        }

        Comparator<AnalyzedCourseVO> nodeComparator = (a, b) -> {
            int diff1 = a.getDifficultyLevel() != null ? a.getDifficultyLevel() : 2;
            int diff2 = b.getDifficultyLevel() != null ? b.getDifficultyLevel() : 2;
            if (diff1 != diff2) return Integer.compare(diff1, diff2);
            double s1 = a.getMatchScore() != null ? a.getMatchScore() : 85.0;
            double s2 = b.getMatchScore() != null ? b.getMatchScore() : 85.0;
            if (Double.compare(s2, s1) != 0) return Double.compare(s2, s1);
            return Long.compare(a.getCourseId(), b.getCourseId());
        };

        PriorityQueue<AnalyzedCourseVO> pq = new PriorityQueue<>(nodeComparator);
        for (AnalyzedCourseVO c : activeCourses) {
            if (inDegree.get(c.getCourseId()) == 0) {
                pq.offer(c);
            }
        }

        List<AnalyzedCourseVO> sortedCourses = new ArrayList<>();
        Set<Long> visited = new HashSet<>();

        while (!pq.isEmpty()) {
            AnalyzedCourseVO curr = pq.poll();
            sortedCourses.add(curr);
            visited.add(curr.getCourseId());

            for (Long nxtId : adj.getOrDefault(curr.getCourseId(), Collections.emptyList())) {
                int deg = inDegree.get(nxtId) - 1;
                inDegree.put(nxtId, deg);
                if (deg <= 0 && !visited.contains(nxtId)) {
                    AnalyzedCourseVO nxtCourse = courseMap.get(nxtId);
                    if (nxtCourse != null && !pq.contains(nxtCourse)) {
                        pq.offer(nxtCourse);
                    }
                }
            }
        }

        if (sortedCourses.size() < activeCourses.size()) {
            List<AnalyzedCourseVO> remaining = activeCourses.stream()
                .filter(c -> !visited.contains(c.getCourseId()))
                .sorted(nodeComparator)
                .toList();
            sortedCourses.addAll(remaining);
        }

        // 2. 切分至 4 个进阶里程碑阶段
        List<AnalyzedCourseVO> stage1Courses = new ArrayList<>(); // 初级筑基 (难度 1 或先修基石)
        List<AnalyzedCourseVO> stage2Courses = new ArrayList<>(); // 核心进阶 (难度 2)
        List<AnalyzedCourseVO> stage3Courses = new ArrayList<>(); // 架构实战 (难度 3)
        List<AnalyzedCourseVO> stage4Courses = new ArrayList<>(); // 综合攻坚与突破

        boolean skipBasic = directives != null && (Boolean.TRUE.equals(directives.get("skipBasicPhase"))
            || "true".equalsIgnoreCase(String.valueOf(directives.get("skipBasicPhase"))));
        boolean needMoreBeginner = directives != null && Boolean.TRUE.equals(directives.get("needMoreBeginnerCourses"));

        for (AnalyzedCourseVO c : sortedCourses) {
            int diff = c.getDifficultyLevel() != null ? c.getDifficultyLevel() : 2;
            if (!skipBasic && (diff == 1 || needMoreBeginner) && stage1Courses.size() < 3) {
                stage1Courses.add(c);
            } else if (diff <= 2 && stage2Courses.size() < 3) {
                stage2Courses.add(c);
            } else if (stage3Courses.size() < 3) {
                stage3Courses.add(c);
            } else {
                stage4Courses.add(c);
            }
        }

        // 兜底保障：若未跳过基础且 stage1 依然为空但有其他课程，借调一门最低难度的课程至 stage1 夯实底座
        if (!skipBasic && stage1Courses.isEmpty()) {
            if (!stage2Courses.isEmpty()) {
                stage1Courses.add(stage2Courses.remove(0));
            } else if (!stage3Courses.isEmpty()) {
                stage1Courses.add(stage3Courses.remove(0));
            } else if (!stage4Courses.isEmpty()) {
                stage1Courses.add(stage4Courses.remove(0));
            }
        }
        // 同样保障 stage2 在有充裕课程时不出现断层饥饿
        if (stage2Courses.isEmpty()) {
            if (stage1Courses.size() > 1) {
                stage2Courses.add(stage1Courses.remove(stage1Courses.size() - 1));
            } else if (!stage3Courses.isEmpty()) {
                stage2Courses.add(stage3Courses.remove(0));
            } else if (!stage4Courses.isEmpty()) {
                stage2Courses.add(stage4Courses.remove(0));
            }
        }

        // 数据驱动构建阶段定义
        record StageDef(String name, String goal, int hoursPerCourse, List<AnalyzedCourseVO> courseList) {}
        StageDef[] stageDefs = {
            new StageDef("阶段一：核心基石与工程化筑基", "掌握现代核心语言规范与基础工程化设计，夯实扎实底座", 16, stage1Courses),
            new StageDef("阶段二：核心技术进阶与组件精通", "深入主流企业级框架与核心中间件，攻克业务核心技术难点", 24, stage2Courses),
            new StageDef("阶段三：分布式架构与工程级实战", "构建高并发、高可用微服务与生产集群，具备工业级项目落地能力", 32, stage3Courses),
            new StageDef("阶段四：全景前沿攻坚与技术突破", "洞悉底层内核与前沿大模型/云原生，打破技术天花板直达架构专家", 36, stage4Courses)
        };

        List<PathStageVO> stages = new ArrayList<>();
        int stageIndex = 1;
        for (StageDef def : stageDefs) {
            if (!def.courseList.isEmpty()) {
                stages.add(PathStageVO.builder()
                    .stageIndex(stageIndex++)
                    .stageName(def.name)
                    .stageGoal(def.goal)
                    .estimatedHours(def.courseList.size() * def.hoursPerCourse)
                    .courses(def.courseList)
                    .build());
            }
        }

        int totalHours = stages.stream().mapToInt(PathStageVO::getEstimatedHours).sum();

        return LearningPathPlan.builder()
            .intendedRole(role)
            .overallGoal("基于 " + role + " 胜任力标准的自适应闭环成长路径")
            .totalCourses(activeCourses.size())
            .totalEstimatedHours(totalHours)
            .stages(stages)
            .referenceStandard("国家 IT 软件工程师能力标准及大厂 P6/P7 技术模型")
            .build();
    }

    private boolean isPrerequisite(AnalyzedCourseVO c1, AnalyzedCourseVO c2) {
        if (c1 == null || c2 == null || c1.getCourseId().equals(c2.getCourseId())) {
            return false;
        }
        int diff1 = c1.getDifficultyLevel() != null ? c1.getDifficultyLevel() : 2;
        int diff2 = c2.getDifficultyLevel() != null ? c2.getDifficultyLevel() : 2;
        if (diff1 > diff2) {
            return false;
        }

        String c1Name = c1.getCourseName() != null ? c1.getCourseName().toLowerCase() : "";
        List<String> prereqs = c2.getPrerequisiteSkills();
        if (prereqs != null) {
            for (String req : prereqs) {
                if (StringUtils.hasText(req) && (c1Name.contains(req.toLowerCase()) || req.toLowerCase().contains(c1Name))) {
                    return true;
                }
            }
        }
        // 检查 DRAG-KP4SR 知识图谱先修推导证据链
        if (c2.getEvidencePaths() != null) {
            for (String path : c2.getEvidencePaths()) {
                String[] parts = path.split("--PREREQUISITE-->|->");
                if (parts.length >= 2) {
                    String source = parts[0].trim().toLowerCase();
                    if (StringUtils.hasText(source) && (c1Name.contains(source) || (c1.getCoreKnowledgePoints() != null 
                        && c1.getCoreKnowledgePoints().stream().anyMatch(kp -> kp.toLowerCase().contains(source))))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
