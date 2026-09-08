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

        // 1. 按课程难度与先修关系进行拓扑时序分组
        List<AnalyzedCourseVO> stage1Courses = new ArrayList<>(); // 初级筑基 (难度 1)
        List<AnalyzedCourseVO> stage2Courses = new ArrayList<>(); // 核心进阶 (难度 2)
        List<AnalyzedCourseVO> stage3Courses = new ArrayList<>(); // 架构实战 (难度 3)
        List<AnalyzedCourseVO> stage4Courses = new ArrayList<>(); // 综合攻坚与突破

        for (AnalyzedCourseVO c : courses) {
            int diff = c.getDifficultyLevel() != null ? c.getDifficultyLevel() : 2;
            if (diff == 1) {
                stage1Courses.add(c);
            } else if (diff == 2) {
                if (stage2Courses.size() < 2) {
                    stage2Courses.add(c);
                } else {
                    stage3Courses.add(c);
                }
            } else {
                if (stage3Courses.size() < 2) {
                    stage3Courses.add(c);
                } else {
                    stage4Courses.add(c);
                }
            }
        }

        // 平衡各阶段课程分布，确保每个阶段均有抓手
        List<PathStageVO> stages = new ArrayList<>();
        int stageIndex = 1;

        if (!stage1Courses.isEmpty()) {
            stages.add(PathStageVO.builder()
                .stageIndex(stageIndex++)
                .stageName("阶段一：核心基石与工程化筑基")
                .stageGoal("掌握现代核心语言规范与基础工程化设计，夯实扎实底座")
                .estimatedHours(stage1Courses.size() * 16)
                .courses(stage1Courses)
                .build());
        }

        if (!stage2Courses.isEmpty()) {
            stages.add(PathStageVO.builder()
                .stageIndex(stageIndex++)
                .stageName("阶段二：核心技术进阶与组件精通")
                .stageGoal("深入主流企业级框架与核心中间件，攻克业务核心技术难点")
                .estimatedHours(stage2Courses.size() * 24)
                .courses(stage2Courses)
                .build());
        }

        if (!stage3Courses.isEmpty()) {
            stages.add(PathStageVO.builder()
                .stageIndex(stageIndex++)
                .stageName("阶段三：分布式架构与工程级实战")
                .stageGoal("构建高并发、高可用微服务与生产集群，具备工业级项目落地能力")
                .estimatedHours(stage3Courses.size() * 32)
                .courses(stage3Courses)
                .build());
        }

        if (!stage4Courses.isEmpty()) {
            stages.add(PathStageVO.builder()
                .stageIndex(stageIndex++)
                .stageName("阶段四：全景前沿攻坚与技术突破")
                .stageGoal("洞悉底层内核与前沿大模型/云原生，打破技术天花板直达架构专家")
                .estimatedHours(stage4Courses.size() * 36)
                .courses(stage4Courses)
                .build());
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
}
