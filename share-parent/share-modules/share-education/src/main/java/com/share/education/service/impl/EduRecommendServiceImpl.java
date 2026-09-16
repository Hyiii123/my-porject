package com.share.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.common.security.utils.SecurityUtils;
import com.share.education.ai.evals.AgentEvalMetricsVO;
import com.share.education.ai.model.ActiveProbeQuestion;
import com.share.education.ai.model.LearningPathPlan;
import com.share.education.ai.model.PersonalizedRecommendVO;
import com.share.education.ai.orchestrator.MultiAgentRecommendOrchestrator;
import com.share.education.domain.*;
import com.share.education.mapper.*;
import com.share.education.service.IEduCourseService;
import com.share.education.service.IEduRecommendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.share.education.service.support.EduUtils.*;

/**
 * 智能推荐、多智能体导学与学员画像领域服务实现
 */
@Slf4j
@Service
@Primary
public class EduRecommendServiceImpl implements IEduRecommendService {

    private static final int ENABLED = 1;

    private final EduCourseRecommendMapper recommendMapper;
    private final EduCourseMapper courseMapper;
    private final EduUserPortraitMapper portraitMapper;
    private final EduLearningRecordMapper learningMapper;
    private final EduExamRecordMapper examRecordMapper;
    private final EduExamMapper examMapper;
    private final MultiAgentRecommendOrchestrator multiAgentOrchestrator;
    private final ObjectMapper objectMapper;
    private final IEduCourseService courseService;

    public EduRecommendServiceImpl(EduCourseRecommendMapper recommendMapper,
                                  EduCourseMapper courseMapper,
                                  EduUserPortraitMapper portraitMapper,
                                  EduLearningRecordMapper learningMapper,
                                  EduExamRecordMapper examRecordMapper,
                                  EduExamMapper examMapper,
                                  MultiAgentRecommendOrchestrator multiAgentOrchestrator,
                                  ObjectMapper objectMapper,
                                  IEduCourseService courseService) {
        this.recommendMapper = recommendMapper;
        this.courseMapper = courseMapper;
        this.portraitMapper = portraitMapper;
        this.learningMapper = learningMapper;
        this.examRecordMapper = examRecordMapper;
        this.examMapper = examMapper;
        this.multiAgentOrchestrator = multiAgentOrchestrator;
        this.objectMapper = objectMapper;
        this.courseService = courseService;
    }

    @Override
    public List<Map<String, Object>> recommendations(String type) {
        String recommendType = switch (String.valueOf(type)) {
            case "1", "recommend", "home" -> "home";
            case "2", "hot" -> "hot";
            case "3", "new" -> "new";
            default -> String.valueOf(type);
        };
        List<EduCourse> courses = new ArrayList<>();
        List<EduCourseRecommend> relations = recommendMapper.selectList(new LambdaQueryWrapper<EduCourseRecommend>()
                .eq(EduCourseRecommend::getRecommendType, recommendType)
                .eq(EduCourseRecommend::getStatus, ENABLED)
                .orderByAsc(EduCourseRecommend::getSortNum));
        if (!relations.isEmpty()) {
            Map<Long, EduCourse> byId = courseMapper.selectBatchIds(relations.stream()
                    .map(EduCourseRecommend::getCourseId).toList()).stream()
                    .collect(Collectors.toMap(EduCourse::getId, Function.identity(), (a, b) -> a));
            for (EduCourseRecommend relation : relations) {
                EduCourse course = byId.get(relation.getCourseId());
                if (course != null) {
                    courses.add(course);
                }
            }
        } else {
            LambdaQueryWrapper<EduCourse> wrapper = new LambdaQueryWrapper<EduCourse>().eq(EduCourse::getStatus, ENABLED);
            if ("home".equals(recommendType)) {
                wrapper.eq(EduCourse::getIsRecommended, ENABLED).orderByAsc(EduCourse::getSortNum);
            } else if ("hot".equals(recommendType)) {
                wrapper.eq(EduCourse::getIsHot, ENABLED).orderByDesc(EduCourse::getLearnerCount);
            } else if ("new".equals(recommendType)) {
                wrapper.orderByDesc(EduCourse::getPublishTime).orderByDesc(EduCourse::getCreateTime);
            } else {
                wrapper.orderByAsc(EduCourse::getSortNum);
            }
            courses = courseMapper.selectList(wrapper);
        }
        return courses.stream().limit(20).map(courseService::courseView).toList();
    }

    @Override
    public List<Map<String, Object>> personalizedRecommendations(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        Long currentUid = null;
        try {
            currentUid = SecurityUtils.getUserId();
        } catch (Exception ignored) {}

        List<PersonalizedRecommendVO> recommendList = multiAgentOrchestrator.recommendCourses(currentUid, safeLimit);
        List<Map<String, Object>> result = new ArrayList<>();

        for (PersonalizedRecommendVO vo : recommendList) {
            EduCourse c = courseMapper.selectById(vo.getId());
            Map<String, Object> view = (c != null) ? courseService.courseView(c) : new LinkedHashMap<>();
            view.put("id", vo.getId());
            view.put("title", vo.getTitle());
            view.put("courseName", vo.getTitle());
            view.put("cover", vo.getCover());
            view.put("coverUrl", vo.getCoverUrl());
            if (c != null && c.getPrice() != null) {
                view.put("price", moneyCents(c.getPrice()));
            } else {
                view.put("price", vo.getPrice());
            }
            if (c != null && c.getOriginalPrice() != null) {
                view.put("originalPrice", moneyCents(c.getOriginalPrice()));
            } else {
                view.put("originalPrice", vo.getOriginalPrice());
            }
            view.put("teacherName", vo.getTeacherName());
            view.put("difficulty", vo.getDifficulty());
            view.put("learners", vo.getLearners());
            view.put("matchScore", vo.getMatchScore());
            view.put("matchTag", vo.getMatchTag());
            view.put("recommendReason", vo.getRecommendReason());
            view.put("learningStage", vo.getLearningStage());
            view.put("skillGapFilled", vo.getSkillGapFilled());
            view.put("prerequisiteSkills", vo.getPrerequisiteSkills());
            view.put("evidencePaths", vo.getEvidencePaths());
            view.put("bloomLevel", vo.getBloomLevel());
            view.put("bloomName", vo.getBloomName());
            view.put("bloomLevelName", vo.getBloomLevelName() != null ? vo.getBloomLevelName() : vo.getBloomName());
            view.put("isCapstone", vo.getIsCapstone());
            view.put("capstoneProject", vo.getCapstoneProject() != null ? vo.getCapstoneProject() : vo.getIsCapstone());
            result.add(view);
        }

        return result;
    }

    @Override
    public LearningPathPlan getPersonalizedLearningPath() {
        Long currentUid = null;
        try {
            currentUid = SecurityUtils.getUserId();
        } catch (Exception ignored) {}
        return multiAgentOrchestrator.getLearningPath(currentUid);
    }

    @Override
    public Map<String, Object> refinePersonalizedLearningPath(Map<String, Object> overrides) {
        Long currentUid = null;
        try {
            currentUid = SecurityUtils.getUserId();
        } catch (Exception ignored) {}
        return multiAgentOrchestrator.refineLearningPath(currentUid, overrides);
    }

    @Override
    public List<ActiveProbeQuestion> getActiveProbingQuestions() {
        Long currentUid = null;
        try {
            currentUid = SecurityUtils.getUserId();
        } catch (Exception ignored) {}
        return multiAgentOrchestrator.getProbingQuestions(currentUid);
    }

    @Override
    public Map<String, Object> submitActiveProbingAnswers(Map<String, String> answers) {
        Long currentUid = null;
        try {
            currentUid = SecurityUtils.getUserId();
        } catch (Exception ignored) {}
        return multiAgentOrchestrator.submitProbingAnswers(currentUid, answers);
    }

    @Override
    public AgentEvalMetricsVO getAgentEvaluationMetrics() {
        return multiAgentOrchestrator.getEvaluationMetrics();
    }

    @Override
    public SseEmitter streamPersonalizedReasoning(String targetRole) {
        Long currentUid = null;
        try {
            currentUid = SecurityUtils.getUserId();
        } catch (Exception ignored) {}
        SseEmitter emitter = new SseEmitter(60000L);
        multiAgentOrchestrator.streamReasoning(currentUid, targetRole, emitter);
        return emitter;
    }

    @Override
    public Map<String, Object> orchestrateAgentRecommend(Long userId, String targetRole, Integer limit) {
        int recLimit = limit != null && limit > 0 ? Math.min(limit, 10) : 4;
        Map<String, Object> overrides = new LinkedHashMap<>();
        if (StringUtils.hasText(targetRole)) {
            overrides.put("targetRole", targetRole.trim());
        }
        List<PersonalizedRecommendVO> recs = multiAgentOrchestrator.recommendCourses(userId, recLimit, Collections.emptyMap(), overrides);
        LearningPathPlan plan = multiAgentOrchestrator.getLearningPath(userId, Collections.emptyMap(), overrides);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("targetRole", StringUtils.hasText(targetRole) ? targetRole.trim() : "Java 全栈开发工程师");
        result.put("recommendations", recs);
        result.put("learningPath", plan);
        if (plan != null && plan.getCriticReport() != null) {
            result.put("criticReport", plan.getCriticReport());
        }
        return result;
    }

    @Override
    public Map<String, Object> getUserPortrait(Long userId) {
        Long targetUid = userId != null && userId > 0 ? userId : currentUserId();
        EduUserPortrait portrait = portraitMapper.selectOne(new LambdaQueryWrapper<EduUserPortrait>()
                .eq(EduUserPortrait::getUserId, targetUid)
                .orderByDesc(EduUserPortrait::getUpdateTime)
                .last("limit 1"));
        if (portrait == null || portrait.getLastCalculatedTime() == null
                || portrait.getLastCalculatedTime().isBefore(LocalDateTime.now().minusHours(12))) {
            portrait = calculateAndSaveUserPortrait(targetUid, portrait);
        }
        return portraitView(portrait);
    }

    @Override
    public Map<String, Object> updateUserPortraitPreferences(Long userId, Map<String, Object> body) {
        Long targetUid = userId != null && userId > 0 ? userId : currentUserId();
        EduUserPortrait portrait = portraitMapper.selectOne(new LambdaQueryWrapper<EduUserPortrait>()
                .eq(EduUserPortrait::getUserId, targetUid)
                .orderByDesc(EduUserPortrait::getUpdateTime)
                .last("limit 1"));
        if (portrait == null) {
            portrait = calculateAndSaveUserPortrait(targetUid, null);
        }
        if (body != null) {
            if (body.containsKey("intendedRole") && StringUtils.hasText(String.valueOf(body.get("intendedRole")))) {
                portrait.setIntendedRole(String.valueOf(body.get("intendedRole")).trim());
            }
            if (body.containsKey("preferredDifficulty")) {
                portrait.setPreferredDifficulty(intValue(body.get("preferredDifficulty"), 2));
            }
            if (body.containsKey("learningStyle") && StringUtils.hasText(String.valueOf(body.get("learningStyle")))) {
                portrait.setLearningStyle(String.valueOf(body.get("learningStyle")).trim());
            }
            portrait.setUpdateTime(LocalDateTime.now());
            portraitMapper.updateById(portrait);
            multiAgentOrchestrator.invalidateUserCache(targetUid);
        }
        return portraitView(portrait);
    }

    private EduUserPortrait calculateAndSaveUserPortrait(Long userId, EduUserPortrait existing) {
        if (existing == null) {
            existing = portraitMapper.selectOne(new LambdaQueryWrapper<EduUserPortrait>()
                    .eq(EduUserPortrait::getUserId, userId)
                    .orderByDesc(EduUserPortrait::getUpdateTime)
                    .last("limit 1"));
        }
        List<EduLearningRecord> learningList = learningMapper.selectList(new LambdaQueryWrapper<EduLearningRecord>()
                .eq(EduLearningRecord::getUserId, userId));
        List<EduExamRecord> examList = examRecordMapper.selectList(new LambdaQueryWrapper<EduExamRecord>()
                .eq(EduExamRecord::getUserId, userId));

        Map<String, Double> skillScores = new LinkedHashMap<>();
        BigDecimal totalHours = BigDecimal.ZERO;
        double sumProgress = 0.0;
        int completedCount = 0;

        if (!learningList.isEmpty()) {
            List<Long> courseIds = learningList.stream().map(EduLearningRecord::getCourseId).filter(Objects::nonNull).distinct().toList();
            Map<Long, EduCourse> courseMap = courseIds.isEmpty() ? Collections.emptyMap() : courseMapper.selectBatchIds(courseIds).stream()
                    .collect(Collectors.toMap(EduCourse::getId, Function.identity(), (a, b) -> a));

            for (EduLearningRecord record : learningList) {
                EduCourse c = courseMap.get(record.getCourseId());
                double prog = record.getProgressPercent() != null ? record.getProgressPercent().doubleValue() : 0.0;
                sumProgress += prog;
                if (record.getStatus() != null && record.getStatus() == 2) {
                    completedCount++;
                }
                int durationSec = record.getLearnDurationSeconds() != null ? record.getLearnDurationSeconds() : 0;
                totalHours = totalHours.add(BigDecimal.valueOf(durationSec).divide(BigDecimal.valueOf(3600), 1, RoundingMode.HALF_UP));

                if (c != null && StringUtils.hasText(c.getSkills())) {
                    double skillContribution = 35.0 + (prog * 0.45);
                    if (record.getStatus() != null && record.getStatus() == 2) skillContribution += 15.0;
                    String[] tokens = c.getSkills().split("[,，、]+");
                    for (String token : tokens) {
                        String s = token.trim();
                        if (StringUtils.hasText(s)) {
                            skillScores.put(s, skillScores.getOrDefault(s, 0.0) + skillContribution);
                        }
                    }
                }
            }
        }

        if (!examList.isEmpty()) {
            for (EduExamRecord exam : examList) {
                if (exam.getExamId() != null) {
                    EduExam eduExam = examMapper.selectById(exam.getExamId());
                    boolean passed = eduExam != null && eduExam.getPassScore() != null && exam.getScore() != null
                            && exam.getScore().compareTo(eduExam.getPassScore()) >= 0;
                    if (passed && eduExam.getCourseId() != null) {
                        EduCourse ec = courseMapper.selectById(eduExam.getCourseId());
                        if (ec != null && StringUtils.hasText(ec.getSkills())) {
                            for (String token : ec.getSkills().split("[,，、]+")) {
                                String s = token.trim();
                                if (StringUtils.hasText(s)) {
                                    skillScores.put(s, skillScores.getOrDefault(s, 0.0) + 15.0);
                                }
                            }
                        }
                    }
                }
            }
        }

        Map<String, Integer> normalizedSkills = new LinkedHashMap<>();
        if (!skillScores.isEmpty()) {
            double maxScore = skillScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(100.0);
            skillScores.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .limit(6)
                    .forEach(e -> {
                        int norm = Math.min(95, Math.max(35, (int) Math.round((e.getValue() / Math.max(maxScore, 50.0)) * 90.0)));
                        normalizedSkills.put(e.getKey(), norm);
                    });
        } else {
            normalizedSkills.put("Java", 75);
            normalizedSkills.put("SpringBoot", 70);
            normalizedSkills.put("Redis", 60);
            normalizedSkills.put("MySQL", 68);
            normalizedSkills.put("Vue3", 45);
        }

        double avgCompletionRate = learningList.isEmpty() ? 65.0 : (sumProgress / learningList.size());
        BigDecimal completionRate = BigDecimal.valueOf(avgCompletionRate).setScale(2, RoundingMode.HALF_UP);

        List<String> tagList = new ArrayList<>();
        if (!normalizedSkills.isEmpty()) {
            String topSkill = normalizedSkills.keySet().iterator().next();
            tagList.add(topSkill + "技术栈");
        }
        if (avgCompletionRate >= 70.0) tagList.add("自律学习达人");
        if (normalizedSkills.containsKey("Redis") || normalizedSkills.containsKey("MySQL") || normalizedSkills.containsKey("Go")) {
            tagList.add("高并发探索者");
        }
        if (normalizedSkills.containsKey("Vue3") || normalizedSkills.containsKey("React18") || normalizedSkills.containsKey("TypeScript")) {
            tagList.add("前端全栈开发者");
        }
        if (normalizedSkills.containsKey("机器学习") || normalizedSkills.containsKey("深度学习")) {
            tagList.add("AI大模型先锋");
        }
        if (tagList.size() < 3) tagList.add("系统进阶期");
        if (tagList.size() < 4) tagList.add("夜间专注");

        EduUserPortrait portrait = existing != null ? existing : new EduUserPortrait();
        if (portrait.getId() == null) {
            portrait.setId(newId());
            portrait.setUserId(userId);
            portrait.setCreateTime(LocalDateTime.now());
            portrait.setDelFlag(0);
            portrait.setVersion(0);
        }
        if (!StringUtils.hasText(portrait.getIntendedRole())) {
            portrait.setIntendedRole(normalizedSkills.containsKey("Vue3") ? "前端开发工程师" : "Java后端工程师");
        }
        if (portrait.getPreferredDifficulty() == null) {
            portrait.setPreferredDifficulty(avgCompletionRate > 75.0 ? 2 : 1);
        }
        if (portrait.getLearningStyle() == null) {
            portrait.setLearningStyle("systematic");
        }
        if (portrait.getStudyFrequency() == null) {
            portrait.setStudyFrequency("night");
        }
        if (portrait.getPriceSensitivity() == null) {
            portrait.setPriceSensitivity("medium");
        }
        portrait.setCompletionRate(completionRate);
        portrait.setTotalStudyHours(totalHours.max(BigDecimal.valueOf(12.5)));
        try {
            portrait.setSkillWeights(objectMapper.writeValueAsString(normalizedSkills));
            portrait.setTags(objectMapper.writeValueAsString(tagList));
        } catch (Exception ignored) {}
        portrait.setLastCalculatedTime(LocalDateTime.now());
        portrait.setUpdateTime(LocalDateTime.now());

        if (portraitMapper.selectById(portrait.getId()) == null) {
            portraitMapper.insert(portrait);
        } else {
            portraitMapper.updateById(portrait);
        }
        return portrait;
    }

    @Override
    public Map<String, Object> portraitView(EduUserPortrait item) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (item == null) return result;
        result.put("id", item.getId());
        result.put("userId", item.getUserId());
        result.put("intendedRole", item.getIntendedRole() == null ? "全栈开发工程师" : item.getIntendedRole());
        int prefDiff = item.getPreferredDifficulty() == null ? 2 : item.getPreferredDifficulty();
        result.put("preferredDifficulty", prefDiff);
        String diffName = switch (prefDiff) {
            case 1 -> "初级入门 (Beginner)";
            case 3 -> "高级架构 (Advanced)";
            default -> "中级进阶 (Intermediate)";
        };
        result.put("difficultyName", diffName);
        result.put("learningStyle", item.getLearningStyle() == null ? "systematic" : item.getLearningStyle());
        result.put("completionRate", item.getCompletionRate() == null ? BigDecimal.ZERO : item.getCompletionRate());
        result.put("studyFrequency", item.getStudyFrequency() == null ? "night" : item.getStudyFrequency());
        result.put("totalStudyHours", item.getTotalStudyHours() == null ? BigDecimal.ZERO : item.getTotalStudyHours());
        result.put("priceSensitivity", item.getPriceSensitivity() == null ? "medium" : item.getPriceSensitivity());

        Map<String, Integer> skillsMap = new LinkedHashMap<>();
        List<Map<String, Object>> radarList = new ArrayList<>();
        if (StringUtils.hasText(item.getSkillWeights())) {
            try {
                Map<?, ?> parsed = objectMapper.readValue(item.getSkillWeights(), Map.class);
                parsed.forEach((k, v) -> {
                    int score = Math.min(100, Math.max(10, intValue(v, 50)));
                    skillsMap.put(String.valueOf(k), score);
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("skill", String.valueOf(k));
                    point.put("score", score);
                    radarList.add(point);
                });
            } catch (Exception ignored) {}
        }
        if (radarList.isEmpty()) {
            skillsMap.put("Java", 80);
            skillsMap.put("SpringBoot", 75);
            skillsMap.put("Redis", 65);
            skillsMap.put("MySQL", 70);
            skillsMap.put("Vue3", 50);
            skillsMap.forEach((k, v) -> {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("skill", k);
                point.put("score", v);
                radarList.add(point);
            });
        }
        result.put("skillWeights", skillsMap);
        result.put("skillsRadar", radarList);

        List<String> tagList = new ArrayList<>();
        if (StringUtils.hasText(item.getTags())) {
            try {
                List<?> parsed = objectMapper.readValue(item.getTags(), List.class);
                parsed.forEach(t -> tagList.add(String.valueOf(t)));
            } catch (Exception ignored) {}
        }
        if (tagList.isEmpty()) {
            tagList.addAll(List.of("技术探索者", "系统进阶期", "高自律学习者"));
        }
        result.put("tags", tagList);
        result.put("lastCalculatedTime", item.getLastCalculatedTime());
        return result;
    }
}
