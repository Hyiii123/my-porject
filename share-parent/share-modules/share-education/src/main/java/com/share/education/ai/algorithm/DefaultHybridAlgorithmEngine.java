package com.share.education.ai.algorithm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.education.ai.model.AlgorithmCandidateDTO;
import com.share.education.ai.model.UserProfileContext;
import com.share.education.domain.EduCourse;
import com.share.education.mapper.EduCourseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 默认混合基线推荐算法引擎 (用于在用户实验中的自研算法嵌入前提供高可用、高精度的多维特征召回)。
 *
 * <p>核心特征计算：
 * 1. 50 维技术图谱向量空间余弦相似度 (Cosine Vector Similarity)
 * 2. 离散技术栈图谱重叠度与关键词覆盖率
 * 3. 目标岗位契合加权与先修难度亲和度
 * 4. 学习热度对数平滑惩罚 (平衡冷门好课与全站爆款)
 * </p>
 */
@Component
public class DefaultHybridAlgorithmEngine implements IRecommendAlgorithmEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultHybridAlgorithmEngine.class);

    private final EduCourseMapper courseMapper;

    // 预置主流技术图谱向量维度 (与教育知识库对齐的 50 维技术特征空间)
    private static final String[] TECH_FEATURES = {
        "Java", "Spring", "SpringBoot", "SpringCloud", "MyBatis", "MySQL", "Redis", "Kafka",
        "RabbitMQ", "JVM", "Linux", "Docker", "Kubernetes", "Vue", "Vue3", "React", "TypeScript",
        "JavaScript", "Node.js", "Python", "Go", "Golang", "Rust", "C++", "PyTorch", "TensorFlow",
        "大语言模型", "LLM", "RAG", "LangChain", "Flink", "Spark", "Hadoop", "ClickHouse", "Elasticsearch",
        "微服务", "高并发", "分布式", "中间件", "性能优化", "设计模式", "网络编程", "数据结构与算法",
        "系统架构", "安全渗透", "Web安全", "Flutter", "鸿蒙", "HarmonyOS", "区块链"
    };

    public DefaultHybridAlgorithmEngine(EduCourseMapper courseMapper) {
        this.courseMapper = courseMapper;
    }

    @Override
    public List<AlgorithmCandidateDTO> recallCandidates(Long userId, UserProfileContext profile, int topK) {
        int safeLimit = Math.max(1, Math.min(topK, 50));

        // 1. 查询所有有效上架课程
        List<EduCourse> activeCourses = courseMapper.selectList(
            new LambdaQueryWrapper<EduCourse>()
                .eq(EduCourse::getStatus, 1)
                .orderByDesc(EduCourse::getLearnerCount)
        );

        if (activeCourses == null || activeCourses.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> enrolledSet = (profile != null && profile.getEnrolledCourseIds() != null)
                ? profile.getEnrolledCourseIds() : Collections.emptySet();

        Map<String, Integer> userSkills = (profile != null && profile.getSkillWeights() != null)
                ? profile.getSkillWeights() : Collections.emptyMap();
        String intendedRole = (profile != null && profile.getIntendedRole() != null)
                ? profile.getIntendedRole() : "";
        int preferredDifficulty = (profile != null && profile.getPreferredDifficulty() != null)
                ? profile.getPreferredDifficulty() : 2;

        // 2. 构建学员技能特征向量
        double[] userVector = buildFeatureVector(userSkills);

        List<AlgorithmCandidateDTO> candidates = new ArrayList<>();

        for (EduCourse c : activeCourses) {
            // 过滤已购买或已加入学习计划的课程
            if (enrolledSet.contains(c.getId())) {
                continue;
            }

            double baseScore = 40.0;
            Map<String, Object> featureMap = new LinkedHashMap<>();

            // 2.1 密集技术向量余弦相似度 (0 ~ 35 分)
            double[] courseVector = buildCourseVector(c);
            double cosSim = computeCosineSimilarity(userVector, courseVector);
            double vectorScore = cosSim * 35.0;
            featureMap.put("vectorSimilarity", String.format("%.4f", cosSim));
            featureMap.put("vectorScore", String.format("%.2f", vectorScore));

            // 2.2 离散技术标签重叠得分 (0 ~ 35 分)
            double discreteScore = 0.0;
            List<String> matchedSkills = new ArrayList<>();
            if (StringUtils.hasText(c.getSkills()) && !userSkills.isEmpty()) {
                String[] skills = c.getSkills().split("[,，、 ]+");
                for (String s : skills) {
                    String trimmed = s.trim();
                    if (userSkills.containsKey(trimmed)) {
                        int w = userSkills.get(trimmed);
                        discreteScore += w * 0.35;
                        matchedSkills.add(trimmed);
                    }
                }
            }
            double skillScore = Math.min(35.0, discreteScore);
            featureMap.put("matchedSkills", matchedSkills);

            // 融合向量与离散得分 (取其长者并提供适度协同提升)
            double techMatchScore = Math.max(vectorScore, skillScore);
            baseScore += techMatchScore;

            // 2.3 目标职业岗位对齐度 (0 ~ 15 分)
            boolean roleMatched = false;
            if (StringUtils.hasText(intendedRole) && StringUtils.hasText(c.getTargetRole())) {
                if (c.getTargetRole().contains(intendedRole) || intendedRole.contains(c.getTargetRole())) {
                    baseScore += 15.0;
                    roleMatched = true;
                }
            }
            featureMap.put("roleMatched", roleMatched);

            // 2.4 难度适配平滑度 (0 ~ 10 分)
            int courseDiff = c.getDifficultyLevel() != null ? c.getDifficultyLevel() : 2;
            if (courseDiff == preferredDifficulty) {
                baseScore += 10.0;
            } else if (courseDiff == preferredDifficulty + 1) {
                // 适度挑战进阶，给予 7 分
                baseScore += 7.0;
            } else {
                baseScore += 3.0;
            }
            featureMap.put("difficultyAlignment", courseDiff);

            // 2.5 学习者热度与评分对数平滑奖励 (0 ~ 5 分)
            int learners = c.getLearnerCount() != null ? c.getLearnerCount() : 0;
            double popularityBonus = Math.min(5.0, Math.log10(Math.max(1, learners)) * 1.2);
            baseScore += popularityBonus;
            featureMap.put("popularityBonus", String.format("%.2f", popularityBonus));

            // 归一化总得分到 0 ~ 100
            double finalScore = Math.min(99.5, Math.max(30.0, baseScore));

            // 标签标注
            String matchTag = (roleMatched && techMatchScore > 20) ? "岗位强契合"
                : (techMatchScore > 25 ? "核心技术对齐"
                : (courseDiff > preferredDifficulty ? "架构跃升突破"
                : (learners > 15000 ? "全站爆款好课" : "精选进阶")));

            candidates.add(AlgorithmCandidateDTO.builder()
                .courseId(c.getId())
                .score(finalScore)
                .matchTag(matchTag)
                .featureMap(featureMap)
                .build());
        }

        // 按得分降序排序，取 Top K
        candidates.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return candidates.stream().limit(safeLimit).toList();
    }

    @Override
    public String getEngineName() {
        return "DefaultHybridBaseline-50D-Cosine-Graph";
    }

    private double[] buildFeatureVector(Map<String, Integer> skills) {
        double[] vec = new double[TECH_FEATURES.length];
        if (skills == null || skills.isEmpty()) {
            return vec;
        }
        for (int i = 0; i < TECH_FEATURES.length; i++) {
            String f = TECH_FEATURES[i];
            for (Map.Entry<String, Integer> entry : skills.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(f) || entry.getKey().contains(f) || f.contains(entry.getKey())) {
                    vec[i] = Math.max(vec[i], entry.getValue() / 100.0);
                }
            }
        }
        return vec;
    }

    private double[] buildCourseVector(EduCourse c) {
        double[] vec = new double[TECH_FEATURES.length];
        String combined = (c.getCourseName() + " " + c.getSkills() + " " + c.getTargetRole() + " " + c.getDescription()).toLowerCase();
        for (int i = 0; i < TECH_FEATURES.length; i++) {
            String f = TECH_FEATURES[i].toLowerCase();
            if (combined.contains(f)) {
                vec[i] = 1.0;
            }
        }
        return vec;
    }

    private double computeCosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
