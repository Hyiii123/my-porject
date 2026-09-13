package com.share.education.ai.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.education.ai.model.UserProfileContext;
import com.share.education.domain.EduLearningRecord;
import com.share.education.domain.EduUserPortrait;
import com.share.education.mapper.EduLearningRecordMapper;
import com.share.education.mapper.EduUserPortraitMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能体 1：用户画像 Agent (UserProfileAgent)。
 *
 * <p>聚合学员多维静态画像、动态学情轨迹、技能掌握雷达与目标职业差距，
 * 产出结构化《学员认知与能力画像报告》。</p>
 */
@Component
public class UserProfileAgent {

    private static final Logger log = LoggerFactory.getLogger(UserProfileAgent.class);

    private final EduUserPortraitMapper portraitMapper;
    private final EduLearningRecordMapper learningMapper;
    private final ObjectMapper objectMapper;

    public UserProfileAgent(EduUserPortraitMapper portraitMapper,
                            EduLearningRecordMapper learningMapper,
                            ObjectMapper objectMapper) {
        this.portraitMapper = portraitMapper;
        this.learningMapper = learningMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 构建学员深度画像上下文
     */
    public UserProfileContext buildProfile(Long userId) {
        if (userId == null || userId <= 0) {
            // 未登录访客：默认返回冷启动通用画像
            return UserProfileContext.builder()
                .userId(null)
                .intendedRole("全栈开发工程师")
                .preferredDifficulty(2)
                .skillWeights(Map.of("Java", 50, "Python", 40, "Vue", 40))
                .topSkills(List.of("Java", "Vue"))
                .skillGaps(List.of("微服务架构", "高并发调优", "云原生K8s"))
                .disciplineScore(70)
                .userTags(List.of("探索者", "全能视野"))
                .enrolledCourseIds(Collections.emptySet())
                .completedHours(0.0)
                .cognitiveLevel("初级进阶")
                .build();
        }

        // 1. 查询用户画像数据
        EduUserPortrait portrait = portraitMapper.selectOne(
            new LambdaQueryWrapper<EduUserPortrait>()
                .eq(EduUserPortrait::getUserId, userId)
        );

        // 2. 查询已学习/已报名课程列表与总学时
        List<EduLearningRecord> learningRecords = learningMapper.selectList(
            new LambdaQueryWrapper<EduLearningRecord>()
                .eq(EduLearningRecord::getUserId, userId)
        );

        Set<Long> enrolledCourseIds = new HashSet<>();
        List<Long> chronologicalCourseIds = new ArrayList<>();
        Map<Long, Double> courseProgressMap = new LinkedHashMap<>();
        double totalHours = 0.0;
        if (learningRecords != null) {
            // 按最后学习时间与记录ID升序，构建严格学习历史时序
            List<EduLearningRecord> sortedRecords = new ArrayList<>(learningRecords);
            sortedRecords.sort(Comparator.comparing(EduLearningRecord::getLastLearnTime, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(EduLearningRecord::getId, Comparator.nullsFirst(Comparator.naturalOrder())));

            for (EduLearningRecord r : sortedRecords) {
                if (r.getCourseId() != null) {
                    enrolledCourseIds.add(r.getCourseId());
                    if (!chronologicalCourseIds.contains(r.getCourseId())) {
                        chronologicalCourseIds.add(r.getCourseId());
                    }
                    if (r.getProgressPercent() != null) {
                        courseProgressMap.put(r.getCourseId(), r.getProgressPercent().doubleValue());
                    }
                }
                if (r.getLearnDurationSeconds() != null) {
                    totalHours += r.getLearnDurationSeconds() / 3600.0;
                }
            }
        }

        // 3. 解析画像技能权重
        Map<String, Integer> skillWeights = new LinkedHashMap<>();
        String intendedRole = "Java 全栈开发工程师";
        int preferredDifficulty = 2;
        int disciplineScore = 80;
        List<String> tags = new ArrayList<>();

        if (portrait != null) {
            if (StringUtils.hasText(portrait.getIntendedRole())) {
                intendedRole = portrait.getIntendedRole();
            }
            if (portrait.getPreferredDifficulty() != null) {
                preferredDifficulty = portrait.getPreferredDifficulty();
            }
            if (portrait.getCompletionRate() != null) {
                disciplineScore = portrait.getCompletionRate().intValue();
            }
            if (StringUtils.hasText(portrait.getTags())) {
                try {
                    tags = objectMapper.readValue(portrait.getTags(), new TypeReference<List<String>>() {});
                } catch (Exception ignored) {
                    tags = Arrays.asList(portrait.getTags().split("[,，、 ]+"));
                }
            }
            if (StringUtils.hasText(portrait.getSkillWeights())) {
                try {
                    skillWeights = objectMapper.readValue(portrait.getSkillWeights(), new TypeReference<LinkedHashMap<String, Integer>>() {});
                } catch (Exception ex) {
                    log.warn("解析学员画像技能权重失败: {}", ex.getMessage());
                }
            }
        }

        // 4. 识别学员优势技能 (Top 3~5)
        List<String> topSkills = skillWeights.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // 5. 识别针对目标岗位的技能短板 (Skill Gaps)
        List<String> skillGaps = identifySkillGaps(intendedRole, skillWeights);

        // 6. 综合评定学员认知层级
        String cognitiveLevel;
        if (totalHours > 80 || (portrait != null && portrait.getCompletionRate() != null && portrait.getCompletionRate().doubleValue() > 88.0)) {
            cognitiveLevel = "架构突破期";
        } else if (totalHours > 20 || preferredDifficulty >= 2) {
            cognitiveLevel = "技能跃升期";
        } else {
            cognitiveLevel = "基础筑基期";
        }

        return UserProfileContext.builder()
            .userId(userId)
            .intendedRole(intendedRole)
            .preferredDifficulty(preferredDifficulty)
            .skillWeights(skillWeights)
            .topSkills(topSkills)
            .skillGaps(skillGaps)
            .disciplineScore(disciplineScore)
            .userTags(tags)
            .enrolledCourseIds(enrolledCourseIds)
            .completedHours(Math.round(totalHours * 10.0) / 10.0)
            .cognitiveLevel(cognitiveLevel)
            .chronologicalCourseIds(chronologicalCourseIds)
            .courseProgressMap(courseProgressMap)
            .build();
    }

    private List<String> identifySkillGaps(String role, Map<String, Integer> currentSkills) {
        List<String> required = getRequiredSkillsForRole(role);
        List<String> gaps = new ArrayList<>();
        for (String req : required) {
            int current = currentSkills.getOrDefault(req, 0);
            if (current < 60) {
                gaps.add(req);
            }
        }
        return gaps.isEmpty() ? List.of("分布式架构高可用", "性能深度压测与调优") : gaps;
    }

    private List<String> getRequiredSkillsForRole(String role) {
        if (!StringUtils.hasText(role)) return List.of("Java", "微服务", "MySQL", "Redis");
        if (role.contains("Java") || role.contains("后端")) {
            return List.of("SpringBoot", "SpringCloud", "MySQL", "Redis", "微服务", "高并发");
        } else if (role.contains("前端") || role.contains("Web")) {
            return List.of("Vue3", "React", "TypeScript", "Next.js", "Node.js");
        } else if (role.contains("AI") || role.contains("算法") || role.contains("大模型")) {
            return List.of("Python", "PyTorch", "大语言模型", "RAG", "LangChain");
        } else if (role.contains("云原生") || role.contains("DevOps")) {
            return List.of("Docker", "Kubernetes", "Linux", "CI/CD", "Prometheus");
        } else if (role.contains("移动") || role.contains("鸿蒙")) {
            return List.of("Flutter", "HarmonyOS", "TypeScript");
        }
        return List.of("数据结构与算法", "系统架构", "核心技术实战");
    }
}
