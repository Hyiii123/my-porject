package com.share.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.education.domain.EduCourse;
import com.share.education.domain.EduCourseCatalog;
import com.share.education.domain.EduLearningPlan;
import com.share.education.domain.EduLearningRecord;
import com.share.education.mapper.EduCourseCatalogMapper;
import com.share.education.mapper.EduCourseMapper;
import com.share.education.mapper.EduLearningPlanMapper;
import com.share.education.mapper.EduLearningRecordMapper;
import com.share.education.service.IEduCourseService;
import com.share.education.service.IEduLearningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static com.share.education.service.support.EduUtils.*;

/**
 * 学习进度、选课报名与学习计划领域服务实现
 */
@Slf4j
@Service
@Primary
public class EduLearningServiceImpl implements IEduLearningService {

    private static final int ENABLED = 1;

    private final EduLearningRecordMapper learningMapper;
    private final EduLearningPlanMapper planMapper;
    private final EduCourseMapper courseMapper;
    private final EduCourseCatalogMapper catalogMapper;
    private final IEduCourseService courseService;

    public EduLearningServiceImpl(EduLearningRecordMapper learningMapper,
                                  EduLearningPlanMapper planMapper,
                                  EduCourseMapper courseMapper,
                                  EduCourseCatalogMapper catalogMapper,
                                  IEduCourseService courseService) {
        this.learningMapper = learningMapper;
        this.planMapper = planMapper;
        this.courseMapper = courseMapper;
        this.catalogMapper = catalogMapper;
        this.courseService = courseService;
    }

    @Override
    public Map<String, Object> learningCourse(Long courseId) {
        EduLearningRecord record = learningMapper.selectOne(new LambdaQueryWrapper<EduLearningRecord>()
                .eq(EduLearningRecord::getUserId, currentUserId()).eq(EduLearningRecord::getCourseId, courseId)
                .orderByDesc(EduLearningRecord::getUpdateTime).last("limit 1"));
        if (record == null) {
            return null;
        }
        return learningView(record);
    }

    @Override
    public Map<String, Object> learningRecord(Long lessonId) {
        require(lessonId != null, "小节编号不能为空");
        EduLearningRecord record = learningMapper.selectOne(new LambdaQueryWrapper<EduLearningRecord>()
                .eq(EduLearningRecord::getUserId, currentUserId())
                .eq(EduLearningRecord::getCatalogId, lessonId)
                .orderByDesc(EduLearningRecord::getUpdateTime)
                .last("limit 1"));
        return record == null ? null : learningView(record);
    }

    @Override
    @Transactional
    public Map<String, Object> enrollCourse(Long courseId) {
        EduCourse course = courseService.requireCourse(courseId);
        Long userId = currentUserId();
        EduLearningRecord record = learningMapper.selectOne(new LambdaQueryWrapper<EduLearningRecord>()
                .eq(EduLearningRecord::getUserId, userId)
                .eq(EduLearningRecord::getCourseId, courseId)
                .orderByDesc(EduLearningRecord::getUpdateTime)
                .last("limit 1"));
        if (record != null) {
            return learningView(record);
        }
        LocalDateTime now = LocalDateTime.now();
        record = new EduLearningRecord();
        record.setId(newId());
        record.setUserId(userId);
        record.setCourseId(courseId);
        record.setCatalogId(null);
        record.setProgressPercent(BigDecimal.ZERO);
        record.setProgressSeconds(0);
        record.setLearnDurationSeconds(0);
        record.setCompletedLessons(0);
        record.setTotalLessons(defaultValue(course.getLessonCount(), 0));
        record.setStatus(ENABLED);
        record.setLastLearnTime(now);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setDelFlag(0);
        record.setVersion(0);
        learningMapper.insert(record);
        return learningView(record);
    }

    @Override
    @Transactional
    public Map<String, Object> revokeCourse(Long courseId) {
        Long userId = currentUserId();
        if (userId == null || courseId == null) {
            return Map.of();
        }
        learningMapper.delete(new LambdaQueryWrapper<EduLearningRecord>()
                .eq(EduLearningRecord::getUserId, userId)
                .eq(EduLearningRecord::getCourseId, courseId));
        return Map.of("revoked", true, "courseId", courseId, "userId", userId);
    }

    @Override
    @Transactional
    public Map<String, Object> restartLearning(Long courseId) {
        EduCourse course = courseService.requireCourse(courseId);
        Long userId = currentUserId();
        List<EduLearningRecord> records = learningMapper.selectList(new LambdaQueryWrapper<EduLearningRecord>()
                .eq(EduLearningRecord::getUserId, userId)
                .eq(EduLearningRecord::getCourseId, courseId)
                .orderByAsc(EduLearningRecord::getCatalogId)
                .orderByDesc(EduLearningRecord::getUpdateTime));
        require(!records.isEmpty(), "未找到该课程的学习记录");

        LocalDateTime now = LocalDateTime.now();
        int totalLessons = defaultValue(course.getLessonCount(), 0);
        for (EduLearningRecord record : records) {
            record.setProgressPercent(BigDecimal.ZERO);
            record.setProgressSeconds(0);
            record.setLearnDurationSeconds(0);
            record.setCompletedLessons(0);
            record.setTotalLessons(totalLessons);
            record.setStatus(ENABLED);
            record.setLastLearnTime(now);
            record.setUpdateTime(now);
            learningMapper.updateById(record);
        }

        List<EduLearningPlan> plans = planMapper.selectList(new LambdaQueryWrapper<EduLearningPlan>()
                .eq(EduLearningPlan::getUserId, userId)
                .eq(EduLearningPlan::getCourseId, courseId));
        for (EduLearningPlan plan : plans) {
            plan.setProgressPercent(BigDecimal.ZERO);
            plan.setStatus(ENABLED);
            plan.setUpdateTime(now);
            planMapper.updateById(plan);
        }

        EduLearningRecord summary = records.stream()
                .filter(item -> item.getCatalogId() == null)
                .findFirst()
                .orElse(records.get(0));
        return learningView(summary);
    }

    @Override
    public Map<String, Object> learningPage(long pageNo, long pageSize, boolean current) {
        Long userId = currentUserId();
        List<EduLearningRecord> allRecords = learningMapper.selectList(new LambdaQueryWrapper<EduLearningRecord>()
                .eq(EduLearningRecord::getUserId, userId)
                .orderByDesc(EduLearningRecord::getLastLearnTime)
                .orderByDesc(EduLearningRecord::getUpdateTime));

        Map<Long, EduLearningRecord> courseMap = new LinkedHashMap<>();
        for (EduLearningRecord record : allRecords) {
            if (record.getCourseId() == null) continue;
            EduLearningRecord existing = courseMap.get(record.getCourseId());
            if (existing == null) {
                EduLearningRecord copy = new EduLearningRecord();
                copy.setId(record.getId());
                copy.setUserId(record.getUserId());
                copy.setCourseId(record.getCourseId());
                copy.setCatalogId(record.getCatalogId());
                copy.setProgressPercent(record.getProgressPercent());
                copy.setProgressSeconds(record.getProgressSeconds());
                copy.setLearnDurationSeconds(record.getLearnDurationSeconds());
                copy.setCompletedLessons(record.getCompletedLessons());
                copy.setTotalLessons(record.getTotalLessons());
                copy.setStatus(record.getStatus());
                copy.setLastLearnTime(record.getLastLearnTime());
                copy.setUpdateTime(record.getUpdateTime());
                courseMap.put(record.getCourseId(), copy);
            } else {
                if (record.getProgressPercent() != null && (existing.getProgressPercent() == null || record.getProgressPercent().compareTo(existing.getProgressPercent()) > 0)) {
                    existing.setProgressPercent(record.getProgressPercent());
                }
                if (record.getCompletedLessons() != null && (existing.getCompletedLessons() == null || record.getCompletedLessons() > existing.getCompletedLessons())) {
                    existing.setCompletedLessons(record.getCompletedLessons());
                }
                if (record.getLastLearnTime() != null && (existing.getLastLearnTime() == null || record.getLastLearnTime().isAfter(existing.getLastLearnTime()))) {
                    existing.setLastLearnTime(record.getLastLearnTime());
                    if (record.getCatalogId() != null) {
                        existing.setCatalogId(record.getCatalogId());
                    }
                }
            }
        }

        List<EduLearningRecord> distinctList = new ArrayList<>(courseMap.values());
        if (current) {
            distinctList = distinctList.stream()
                    .filter(r -> r.getProgressPercent() == null || r.getProgressPercent().compareTo(BigDecimal.valueOf(100)) < 0)
                    .toList();
        }

        long total = distinctList.size();
        int safePageNo = (int) safePage(pageNo);
        int safePageSize = (int) safeSize(pageSize);
        int fromIndex = Math.min((safePageNo - 1) * safePageSize, (int) total);
        int toIndex = Math.min(fromIndex + safePageSize, (int) total);
        List<EduLearningRecord> pagedList = distinctList.subList(fromIndex, toIndex);

        return pageView(total, pagedList.stream().map(this::learningView).toList());
    }

    @Override
    public List<Map<String, Object>> plans() {
        return planMapper.selectList(new LambdaQueryWrapper<EduLearningPlan>()
                .eq(EduLearningPlan::getUserId, currentUserId()).orderByAsc(EduLearningPlan::getTargetDate))
                .stream().map(this::planView).toList();
    }

    @Override
    @Transactional
    public EduLearningPlan savePlan(EduLearningPlan value) {
        require(value != null && value.getCourseId() != null, "课程不能为空");
        value.setUserId(currentUserId());
        if (value.getId() == null) {
            value.setId(newId());
            value.setDailyMinutes(defaultValue(value.getDailyMinutes(), 30));
            value.setProgressPercent(defaultValue(value.getProgressPercent(), BigDecimal.ZERO));
            value.setStatus(defaultValue(value.getStatus(), ENABLED));
            value.setCreateTime(LocalDateTime.now());
            value.setUpdateTime(LocalDateTime.now());
            value.setDelFlag(0);
            value.setVersion(0);
            planMapper.insert(value);
        } else {
            value.setUpdateTime(LocalDateTime.now());
            planMapper.updateById(value);
        }
        return value;
    }

    @Override
    @Transactional
    public void removeLearning(Long courseId) {
        learningMapper.delete(new LambdaQueryWrapper<EduLearningRecord>()
                .eq(EduLearningRecord::getUserId, currentUserId()).eq(EduLearningRecord::getCourseId, courseId));
        planMapper.delete(new LambdaQueryWrapper<EduLearningPlan>()
                .eq(EduLearningPlan::getUserId, currentUserId()).eq(EduLearningPlan::getCourseId, courseId));
    }

    @Override
    @Transactional
    public EduLearningRecord saveLearning(EduLearningRecord value) {
        require(value != null && value.getCourseId() != null, "课程不能为空");
        Long userId = currentUserId();
        LocalDateTime now = LocalDateTime.now();

        EduLearningRecord old = null;
        if (value.getCatalogId() != null) {
            old = learningMapper.selectOne(new LambdaQueryWrapper<EduLearningRecord>()
                    .eq(EduLearningRecord::getUserId, userId)
                    .eq(EduLearningRecord::getCourseId, value.getCourseId())
                    .eq(EduLearningRecord::getCatalogId, value.getCatalogId())
                    .last("limit 1"));
            if (old == null) {
                value.setId(newId());
                value.setUserId(userId);
                value.setProgressPercent(defaultValue(value.getProgressPercent(), BigDecimal.ZERO));
                value.setProgressSeconds(defaultValue(value.getProgressSeconds(), 0));
                value.setLearnDurationSeconds(defaultValue(value.getLearnDurationSeconds(), 0));
                value.setCompletedLessons(defaultValue(value.getCompletedLessons(), 0));
                value.setTotalLessons(defaultValue(value.getTotalLessons(), courseService.courseLessonCount(value.getCourseId())));
                value.setStatus(defaultValue(value.getStatus(), 1));
                value.setLastLearnTime(now);
                value.setCreateTime(now);
                value.setUpdateTime(now);
                value.setDelFlag(0);
                value.setVersion(0);
                learningMapper.insert(value);
                old = value;
            } else {
                if (value.getProgressPercent() != null) old.setProgressPercent(value.getProgressPercent());
                if (value.getProgressSeconds() != null) old.setProgressSeconds(value.getProgressSeconds());
                if (value.getLearnDurationSeconds() != null) old.setLearnDurationSeconds(value.getLearnDurationSeconds());
                if (value.getCompletedLessons() != null) old.setCompletedLessons(value.getCompletedLessons());
                if (value.getTotalLessons() != null) old.setTotalLessons(value.getTotalLessons());
                if (value.getStatus() != null) old.setStatus(value.getStatus());
                old.setLastLearnTime(now);
                old.setUpdateTime(now);
                learningMapper.updateById(old);
            }
        }

        EduLearningRecord summary = learningMapper.selectOne(new LambdaQueryWrapper<EduLearningRecord>()
                .eq(EduLearningRecord::getUserId, userId)
                .eq(EduLearningRecord::getCourseId, value.getCourseId())
                .isNull(EduLearningRecord::getCatalogId)
                .last("limit 1"));

        int totalLessons = courseService.courseLessonCount(value.getCourseId());
        long completedCount = learningMapper.selectCount(new LambdaQueryWrapper<EduLearningRecord>()
                .eq(EduLearningRecord::getUserId, userId)
                .eq(EduLearningRecord::getCourseId, value.getCourseId())
                .isNotNull(EduLearningRecord::getCatalogId)
                .and(w -> w.ge(EduLearningRecord::getProgressPercent, 90).or().eq(EduLearningRecord::getStatus, 2)));
        BigDecimal overallPercent;
        if (totalLessons <= 0) {
            overallPercent = value.getProgressPercent() != null ? value.getProgressPercent() : BigDecimal.ZERO;
            totalLessons = Math.max(1, (int) completedCount);
        } else {
            overallPercent = BigDecimal.valueOf(completedCount * 100.0 / totalLessons)
                    .setScale(2, RoundingMode.HALF_UP);
            if (overallPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
                overallPercent = BigDecimal.valueOf(100);
            }
            if (completedCount == 0 && value.getProgressPercent() != null) {
                overallPercent = value.getProgressPercent().divide(BigDecimal.valueOf(totalLessons), 2, RoundingMode.HALF_UP);
            }
        }

        if (summary == null) {
            summary = new EduLearningRecord();
            summary.setId(newId());
            summary.setUserId(userId);
            summary.setCourseId(value.getCourseId());
            summary.setCatalogId(null);
            summary.setProgressPercent(overallPercent);
            summary.setProgressSeconds(defaultValue(value.getProgressSeconds(), 0));
            summary.setLearnDurationSeconds(defaultValue(value.getLearnDurationSeconds(), 0));
            summary.setCompletedLessons((int) completedCount);
            summary.setTotalLessons(totalLessons);
            summary.setStatus(overallPercent.compareTo(BigDecimal.valueOf(100)) >= 0 ? 2 : 1);
            summary.setLastLearnTime(now);
            summary.setCreateTime(now);
            summary.setUpdateTime(now);
            summary.setDelFlag(0);
            summary.setVersion(0);
            learningMapper.insert(summary);
        } else {
            summary.setProgressPercent(overallPercent);
            summary.setCompletedLessons((int) completedCount);
            summary.setTotalLessons(totalLessons);
            summary.setLastLearnTime(now);
            summary.setUpdateTime(now);
            if (overallPercent.compareTo(BigDecimal.valueOf(100)) >= 0) {
                summary.setStatus(2);
            }
            learningMapper.updateById(summary);
        }

        try {
            List<EduLearningPlan> plans = planMapper.selectList(new LambdaQueryWrapper<EduLearningPlan>()
                    .eq(EduLearningPlan::getUserId, userId)
                    .eq(EduLearningPlan::getCourseId, value.getCourseId()));
            for (EduLearningPlan plan : plans) {
                plan.setProgressPercent(overallPercent);
                if (overallPercent.compareTo(BigDecimal.valueOf(100)) >= 0) {
                    plan.setStatus(2);
                } else if (overallPercent.compareTo(BigDecimal.ZERO) > 0) {
                    plan.setStatus(1);
                }
                plan.setUpdateTime(now);
                planMapper.updateById(plan);
            }
        } catch (Exception ex) {
            log.warn("联动更新学习计划异常: {}", ex.getMessage());
        }

        return summary;
    }

    @Override
    public Map<String, Object> learningView(EduLearningRecord item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("userId", item.getUserId());
        result.put("courseId", item.getCourseId());
        result.put("catalogId", item.getCatalogId());
        result.put("progress", item.getProgressPercent());
        result.put("progressPercent", item.getProgressPercent());
        result.put("progressSeconds", item.getProgressSeconds());
        result.put("learnDurationSeconds", item.getLearnDurationSeconds());
        result.put("completedLessons", item.getCompletedLessons());
        result.put("totalLessons", item.getTotalLessons());
        result.put("learnedSections", item.getCompletedLessons() != null ? item.getCompletedLessons() : 0);
        result.put("status", item.getStatus());
        result.put("lastLearnTime", item.getLastLearnTime());
        result.put("createTime", item.getCreateTime() != null ? item.getCreateTime() : item.getLastLearnTime());
        result.put("expireTime", null);
        EduCourse course = courseMapper.selectById(item.getCourseId());
        if (course != null) {
            result.put("courseName", course.getCourseName());
            result.put("title", course.getCourseName());
            result.put("cover", course.getCoverUrl());
            result.put("coverUrl", course.getCoverUrl());
            result.put("courseCoverUrl", course.getCoverUrl());
            result.put("sections", item.getTotalLessons() != null ? item.getTotalLessons() : course.getLessonCount());
        } else {
            String fallbackName = "课程 " + (item.getCourseId() != null ? item.getCourseId() : "");
            result.put("courseName", fallbackName);
            result.put("title", fallbackName);
            result.put("cover", "");
            result.put("coverUrl", "");
            result.put("courseCoverUrl", "");
            result.put("sections", item.getTotalLessons() != null ? item.getTotalLessons() : 0);
        }
        EduCourseCatalog section = null;
        if (item.getCatalogId() != null) {
            try {
                section = catalogMapper.selectById(item.getCatalogId());
            } catch (Exception ignored) {}
        }
        if (section == null && item.getCourseId() != null) {
            try {
                List<EduCourseCatalog> firstSections = catalogMapper.selectList(new LambdaQueryWrapper<EduCourseCatalog>()
                        .eq(EduCourseCatalog::getCourseId, item.getCourseId())
                        .orderByAsc(EduCourseCatalog::getSortNum)
                        .last("limit 1"));
                if (firstSections != null && !firstSections.isEmpty()) {
                    section = firstSections.get(0);
                }
            } catch (Exception ignored) {}
        }
        if (section != null) {
            result.put("latestSectionName", section.getCatalogTitle());
            result.put("latestSectionIndex", section.getSortNum() != null ? section.getSortNum() : 1);
        } else {
            result.put("latestSectionName", "课程导学");
            result.put("latestSectionIndex", 1);
        }
        return result;
    }

    @Override
    public Map<String, Object> planView(EduLearningPlan item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("courseId", item.getCourseId());
        result.put("planName", item.getPlanName());
        result.put("courseName", Optional.ofNullable(courseMapper.selectById(item.getCourseId())).map(EduCourse::getCourseName).orElse("课程"));
        result.put("targetDate", item.getTargetDate());
        result.put("planDate", item.getTargetDate());
        result.put("dailyMinutes", item.getDailyMinutes());
        result.put("progressPercent", item.getProgressPercent());
        result.put("status", item.getStatus());
        return result;
    }
}
