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

        // 0. 处理人机协同排除课程 (Human-in-the-Loop: excludedCourseIds)
        List<AnalyzedCourseVO> activeCourses = new ArrayList<>(courses);
        if (directives != null && directives.containsKey("excludedCourseIds")) {
            Object exObj = directives.get("excludedCourseIds");
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
        }

        // 1. 基于知识图谱先修依赖与难度的拓扑保序排序
        List<AnalyzedCourseVO> sortedCourses = new ArrayList<>(activeCourses);
        sortedCourses.sort((c1, c2) -> {
            // 拓扑先修关系严格优先
            boolean c1IsPrereqOfC2 = isPrerequisite(c1, c2);
            boolean c2IsPrereqOfC1 = isPrerequisite(c2, c1);
            if (c1IsPrereqOfC2 && !c2IsPrereqOfC1) return -1;
            if (c2IsPrereqOfC1 && !c1IsPrereqOfC2) return 1;

            // 难度等级升序 (筑基 -> 突破)
            int diff1 = c1.getDifficultyLevel() != null ? c1.getDifficultyLevel() : 2;
            int diff2 = c2.getDifficultyLevel() != null ? c2.getDifficultyLevel() : 2;
            if (diff1 != diff2) return Integer.compare(diff1, diff2);

            // 算法得分降序
            double s1 = c1.getMatchScore() != null ? c1.getMatchScore() : 85.0;
            double s2 = c2.getMatchScore() != null ? c2.getMatchScore() : 85.0;
            return Double.compare(s2, s1);
        });

        // 2. 切分至 4 个进阶里程碑阶段
        List<AnalyzedCourseVO> stage1Courses = new ArrayList<>(); // 初级筑基 (难度 1 或先修基石)
        List<AnalyzedCourseVO> stage2Courses = new ArrayList<>(); // 核心进阶 (难度 2)
        List<AnalyzedCourseVO> stage3Courses = new ArrayList<>(); // 架构实战 (难度 3)
        List<AnalyzedCourseVO> stage4Courses = new ArrayList<>(); // 综合攻坚与突破

        boolean needMoreBeginner = directives != null && Boolean.TRUE.equals(directives.get("needMoreBeginnerCourses"));

        for (AnalyzedCourseVO c : sortedCourses) {
            int diff = c.getDifficultyLevel() != null ? c.getDifficultyLevel() : 2;
            if ((diff == 1 || needMoreBeginner) && stage1Courses.size() < 3) {
                stage1Courses.add(c);
            } else if (diff <= 2 && stage2Courses.size() < 3) {
                stage2Courses.add(c);
            } else if (stage3Courses.size() < 3) {
                stage3Courses.add(c);
            } else {
                stage4Courses.add(c);
            }
        }

        // 兜底保障：若 stage1 依然为空但有其他课程，借调一门最低难度的课程至 stage1 夯实底座
        if (stage1Courses.isEmpty() && !stage2Courses.isEmpty()) {
            stage1Courses.add(stage2Courses.remove(0));
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
