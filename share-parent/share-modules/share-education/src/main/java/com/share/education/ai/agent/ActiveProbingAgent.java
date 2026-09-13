package com.share.education.ai.agent;

import com.share.education.ai.model.ActiveProbeQuestion;
import com.share.education.ai.model.ActiveProbeQuestion.ProbeOption;
import com.share.education.ai.model.UserProfileContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 智能体 1-P：冷启动主动探针 Agent (ActiveProbingAgent)。
 *
 * <p>【核心职责】：
 * 1. 评估学员学情画像完备度 ($P_c \in [0.0, 1.0]$)；
 * 2. 对冷启动访客或零记录新学员动态生成 3 项定向诊断探针问卷；
 * 3. 接收学员探针反馈并瞬间自适应校准学情基线，彻底根除“死板硬编码默认画像”带来的推荐失真。</p>
 */
@Component
public class ActiveProbingAgent {

    private static final Logger log = LoggerFactory.getLogger(ActiveProbingAgent.class);

    private static final double COMPLETENESS_THRESHOLD = 0.40;

    /**
     * 计算画像完备度得分 (0.0 ~ 1.0)
     */
    public double computeCompleteness(UserProfileContext profile) {
        if (profile == null || profile.getUserId() == null) {
            return 0.15; // 访客默认偏低
        }

        double score = 0.0;

        // 1. 是否具备明确的目标求职方向 (0.25)
        if (StringUtils.hasText(profile.getIntendedRole()) && !"全栈开发工程师".equals(profile.getIntendedRole())) {
            score += 0.25;
        }

        // 2. 是否具备细分掌握度技能字典 (0.25)
        if (profile.getSkillWeights() != null && profile.getSkillWeights().size() >= 3) {
            score += 0.25;
        }

        // 3. 是否有真实选课与学习时长 (0.30)
        if (profile.getEnrolledCourseIds() != null && !profile.getEnrolledCourseIds().isEmpty()) {
            score += 0.20;
        }
        if (profile.getCompletedHours() != null && profile.getCompletedHours() > 2.0) {
            score += 0.10;
        }

        // 4. 是否具备画像标签或偏好难度 (0.20)
        if (profile.getUserTags() != null && !profile.getUserTags().isEmpty()) {
            score += 0.10;
        }
        if (profile.getPreferredDifficulty() != null && profile.getPreferredDifficulty() > 0) {
            score += 0.10;
        }

        return Math.round(score * 100.0) / 100.0;
    }

    /**
     * 判断是否需要触发主动探针交互
     */
    public boolean needsProbing(UserProfileContext profile) {
        return computeCompleteness(profile) < COMPLETENESS_THRESHOLD;
    }

    /**
     * 动态生成针对冷启动学员的 3 项诊断探针问题
     */
    public List<ActiveProbeQuestion> generateDiagnosticProbes(UserProfileContext profile) {
        List<ActiveProbeQuestion> probes = new ArrayList<>();

        // 探针 1：目标突破技术赛道
        probes.add(ActiveProbeQuestion.builder()
            .questionId("probe_target_role")
            .title("你当前最渴望冲刺的目标技术岗位是？")
            .description("精准锚定目标岗位胜任力模型，智能体将依据大厂职级标准定制成长图谱。")
            .category("CAREER_TARGET")
            .options(List.of(
                ProbeOption.builder()
                    .key("java_architect")
                    .label("Java 全栈架构师")
                    .desc("精通 SpringCloud 微服务治理、高并发性能调优与分布式中间件实战")
                    .inferredSkillWeights(Map.of("Java", 80, "SpringBoot", 75, "MySQL", 70, "Redis", 70, "微服务", 65))
                    .build(),
                ProbeOption.builder()
                    .key("llm_agent_engineer")
                    .label("大语言模型应用工程师")
                    .desc("掌握 LangChain/Spring AI 智能体编排、RAG 向量检索与本地知识库构建")
                    .inferredSkillWeights(Map.of("Python", 80, "大模型", 85, "RAG", 75, "LangChain", 70))
                    .build(),
                ProbeOption.builder()
                    .key("cloud_native_go")
                    .label("Go 云原生架构师")
                    .desc("专注于 K8s 容器云平台、Docker 编排与高性能网络微服务")
                    .inferredSkillWeights(Map.of("Go", 80, "Docker", 80, "K8s", 75, "Linux", 75))
                    .build(),
                ProbeOption.builder()
                    .key("frontend_expert")
                    .label("Web 前端架构专家")
                    .desc("深耕 Vue3/React 源码内核、TypeScript 与现代前端工程化")
                    .inferredSkillWeights(Map.of("Vue3", 85, "TypeScript", 80, "JavaScript", 80, "前端工程化", 75))
                    .build(),
                ProbeOption.builder()
                    .key("big_data_engineer")
                    .label("大数据流批一体工程师")
                    .desc("攻坚 Spark/Flink 实时计算流与高吞吐海量数据湖仓")
                    .inferredSkillWeights(Map.of("Java", 70, "Python", 70, "SQL", 80, "Flink", 75))
                    .build()
            ))
            .build());

        // 探针 2：当前技术段位基准
        probes.add(ActiveProbeQuestion.builder()
            .questionId("probe_current_level")
            .title("你目前的技术积累处于哪一阶段？")
            .description("避免从零说教或突兀拔高，算法将为你匹配认知负荷最舒适的切入点。")
            .category("CURRENT_LEVEL")
            .options(List.of(
                ProbeOption.builder()
                    .key("level_beginner")
                    .label("零基础起步 · 跨考入门")
                    .desc("刚接触编程，希望先打牢语言核心语法与计算机底层常识")
                    .build(),
                ProbeOption.builder()
                    .key("level_junior")
                    .label("初入职场 · 1~2年开发")
                    .desc("掌握日常 CRUD 业务开发，希望突破单体架构迈向工程化规范")
                    .build(),
                ProbeOption.builder()
                    .key("level_intermediate")
                    .label("中坚主力 · 3~5年实战")
                    .desc("熟悉微服务与主流框架，亟需攻坚分布式高并发、性能调优与源码内核")
                    .build(),
                ProbeOption.builder()
                    .key("level_senior")
                    .label("资深工程师 · 冲刺架构专家")
                    .desc("具备系统级技术视野，聚焦前沿 AI 赋能、领域驱动设计与系统稳定性保障")
                    .build()
            ))
            .build());

        // 探针 3：每周学习精力投入
        probes.add(ActiveProbeQuestion.builder()
            .questionId("probe_time_commitment")
            .title("你每周预计可投入的学习时长是？")
            .description("科学排期进阶里程碑，防止因时间透支引发学习焦虑。")
            .category("TIME_COMMITMENT")
            .options(List.of(
                ProbeOption.builder()
                    .key("time_light")
                    .label("碎片精进 (每周 < 6 小时)")
                    .desc("以微课程、核心考点突破与短实战为主，平滑前行")
                    .build(),
                ProbeOption.builder()
                    .key("time_steady")
                    .label("稳步蜕变 (每周 6 ~ 15 小时)")
                    .desc("系统的理论吸收结合工程项目打磨，推荐的黄金进阶节奏")
                    .build(),
                ProbeOption.builder()
                    .key("time_sprint")
                    .label("全速突击 (每周 > 15 小时)")
                    .desc("备战春招/秋招/跳槽跳级，紧密推进各阶段综合攻坚")
                    .build()
            ))
            .build());

        return probes;
    }

    /**
     * 根据学员提交的探针选项，动态校准并注入用户画像上下文
     */
    public UserProfileContext calibrateProfile(UserProfileContext rawProfile, Map<String, String> probeAnswers) {
        if (rawProfile == null) {
            rawProfile = UserProfileContext.builder().build();
        }
        if (probeAnswers == null || probeAnswers.isEmpty()) {
            return rawProfile;
        }

        String targetRole = rawProfile.getIntendedRole();
        int preferredDiff = rawProfile.getPreferredDifficulty() != null ? rawProfile.getPreferredDifficulty() : 2;
        Map<String, Integer> skills = new LinkedHashMap<>(rawProfile.getSkillWeights() != null ? rawProfile.getSkillWeights() : Collections.emptyMap());
        List<String> tags = new ArrayList<>(rawProfile.getUserTags() != null ? rawProfile.getUserTags() : Collections.emptyList());

        // 1. 处理角色选择
        String roleChoice = probeAnswers.get("probe_target_role");
        if (StringUtils.hasText(roleChoice)) {
            switch (roleChoice) {
                case "java_architect" -> {
                    targetRole = "Java全栈架构师";
                    skills.putAll(Map.of("Java", 80, "SpringBoot", 75, "MySQL", 70, "Redis", 70, "微服务", 65));
                }
                case "llm_agent_engineer" -> {
                    targetRole = "大语言模型应用工程师";
                    skills.putAll(Map.of("Python", 80, "大模型", 85, "RAG", 75, "LangChain", 70));
                }
                case "cloud_native_go" -> {
                    targetRole = "Go云原生架构师";
                    skills.putAll(Map.of("Go", 80, "Docker", 80, "K8s", 75, "Linux", 75));
                }
                case "frontend_expert" -> {
                    targetRole = "前端技术专家";
                    skills.putAll(Map.of("Vue3", 85, "TypeScript", 80, "JavaScript", 80, "前端工程化", 75));
                }
                case "big_data_engineer" -> {
                    targetRole = "大数据开发工程师";
                    skills.putAll(Map.of("Java", 70, "Python", 70, "SQL", 80, "Flink", 75));
                }
            }
        }

        // 2. 处理段位选择
        String levelChoice = probeAnswers.get("probe_current_level");
        if (StringUtils.hasText(levelChoice)) {
            switch (levelChoice) {
                case "level_beginner" -> {
                    preferredDiff = 1;
                    tags.add("基础筑基先锋");
                }
                case "level_junior" -> {
                    preferredDiff = 2;
                    tags.add("工程实战进阶");
                }
                case "level_intermediate" -> {
                    preferredDiff = 2;
                    tags.add("系统架构攻坚");
                }
                case "level_senior" -> {
                    preferredDiff = 3;
                    tags.add("技术天花板破局");
                }
            }
        }

        // 3. 提取最高频技能
        List<String> topSkills = skills.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .toList();

        return UserProfileContext.builder()
            .userId(rawProfile.getUserId())
            .intendedRole(targetRole)
            .preferredDifficulty(preferredDiff)
            .skillWeights(skills)
            .topSkills(topSkills)
            .skillGaps(rawProfile.getSkillGaps())
            .disciplineScore(rawProfile.getDisciplineScore() != null ? rawProfile.getDisciplineScore() : 85)
            .userTags(tags.stream().distinct().toList())
            .enrolledCourseIds(rawProfile.getEnrolledCourseIds())
            .completedHours(rawProfile.getCompletedHours())
            .cognitiveLevel(preferredDiff == 1 ? "核心筑基期" : (preferredDiff == 2 ? "技能跃升期" : "架构突破期"))
            .chronologicalCourseIds(rawProfile.getChronologicalCourseIds())
            .courseProgressMap(rawProfile.getCourseProgressMap())
            .build();
    }
}
