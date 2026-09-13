package com.share.education.ai.agent;

import com.share.education.ai.model.AnalyzedCourseVO;
import com.share.education.ai.model.LearningPathPlan;
import com.share.education.ai.model.PathStageVO;
import com.share.education.ai.model.UserProfileContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 智能体 4：路径规划 Agent (PathPlanningAgent)。
 *
 * <p>【核心职责】：
 * 1. 依据知识图谱先修依赖与软件工程师成长进阶曲线，构建有向无环拓扑路线 (DAG)；
 * 2. 将候选课程按进阶梯度合理划分为 3~4 个阶段性成长里程碑；
 * 3. 避免先修断层与学习焦虑，确保学员沿着清晰路径稳步提升。</p>
 */
@Component
public class PathPlanningAgent {

    /**
     * 为学员规划个性化阶段成长路线
     */
    public LearningPathPlan planPath(UserProfileContext profile, List<AnalyzedCourseVO> courses) {
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

        // 1. 基于知识图谱先修依赖 (DRAG-KP4SR 证据链) 与难度的拓扑保序排序
        List<AnalyzedCourseVO> sortedCourses = new ArrayList<>(courses);
        sortedCourses.sort((c1, c2) -> {
            // (1) 检查 c1 是否为 c2 的显式先修
            boolean c1IsPrereqOfC2 = isPrerequisite(c1, c2);
            boolean c2IsPrereqOfC1 = isPrerequisite(c2, c1);
            if (c1IsPrereqOfC2 && !c2IsPrereqOfC1) return -1;
            if (c2IsPrereqOfC1 && !c1IsPrereqOfC2) return 1;

            // (2) 难度等级升序 (基础筑基 -> 架构突破)
            int diff1 = c1.getDifficultyLevel() != null ? c1.getDifficultyLevel() : 2;
            int diff2 = c2.getDifficultyLevel() != null ? c2.getDifficultyLevel() : 2;
            if (diff1 != diff2) return Integer.compare(diff1, diff2);

            // (3) 同难度下算法匹配得分降序
            double s1 = c1.getMatchScore() != null ? c1.getMatchScore() : 85.0;
            double s2 = c2.getMatchScore() != null ? c2.getMatchScore() : 85.0;
            return Double.compare(s2, s1);
        });

        // 2. 将拓扑有序的课程平滑切分至 4 个进阶里程碑阶段
        List<AnalyzedCourseVO> stage1Courses = new ArrayList<>(); // 初级筑基 (难度 1 或先修基石)
        List<AnalyzedCourseVO> stage2Courses = new ArrayList<>(); // 核心进阶 (难度 2)
        List<AnalyzedCourseVO> stage3Courses = new ArrayList<>(); // 架构实战 (难度 3)
        List<AnalyzedCourseVO> stage4Courses = new ArrayList<>(); // 综合攻坚与突破

        for (AnalyzedCourseVO c : sortedCourses) {
            int diff = c.getDifficultyLevel() != null ? c.getDifficultyLevel() : 2;
            if (diff == 1 && stage1Courses.size() < 3) {
                stage1Courses.add(c);
            } else if (diff <= 2 && stage2Courses.size() < 3) {
                stage2Courses.add(c);
            } else if (stage3Courses.size() < 3) {
                stage3Courses.add(c);
            } else {
                stage4Courses.add(c);
            }
        }

        // 平衡各阶段课程分布，数据驱动构建进阶里程碑
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
            .totalCourses(courses.size())
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
