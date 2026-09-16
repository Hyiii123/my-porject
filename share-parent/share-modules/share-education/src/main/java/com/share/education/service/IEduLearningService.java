package com.share.education.service;

import com.share.education.domain.EduLearningPlan;
import com.share.education.domain.EduLearningRecord;

import java.util.List;
import java.util.Map;

/**
 * 学习进度、选课报名与学习计划领域接口
 */
public interface IEduLearningService {

    Map<String, Object> learningCourse(Long courseId);

    Map<String, Object> learningRecord(Long lessonId);

    Map<String, Object> enrollCourse(Long courseId);

    Map<String, Object> enrollCourseForUser(Long userId, Long courseId);

    Map<String, Object> revokeCourse(Long courseId);

    Map<String, Object> restartLearning(Long courseId);

    Map<String, Object> learningPage(long pageNo, long pageSize, boolean current);

    List<Map<String, Object>> plans();

    EduLearningPlan savePlan(EduLearningPlan value);

    void removeLearning(Long courseId);

    EduLearningRecord saveLearning(EduLearningRecord value);

    Map<String, Object> learningView(EduLearningRecord item);

    Map<String, Object> planView(EduLearningPlan item);
}
