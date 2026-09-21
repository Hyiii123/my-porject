package com.share.education.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.share.education.ai.evals.AgentEvalMetricsVO;
import com.share.education.ai.model.ActiveProbeQuestion;
import com.share.education.ai.model.LearningPathPlan;
import com.share.education.domain.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 教育业务聚合服务主接口
 */
public interface IEducationService {

    // --- Category Domain ---
    List<Map<String, Object>> listCategories(boolean includeDisabled);
    Map<String, Object> category(Long id);
    IPage<EduCategory> pageCategories(String keyword, Integer status, long pageNo, long pageSize);
    EduCategory saveCategory(EduCategory value);
    void removeCategories(List<Long> ids);
    List<Map<String, Object>> legacyCategories(boolean includeDisabled);
    Map<String, Object> saveLegacyCategory(Map<String, ?> body);
    void updateLegacyCategoryStatus(Map<String, ?> body);
    void removeLegacyCategory(Long id);
    List<Map<String, Object>> banners();
    List<Map<String, Object>> interests();
    EduInterest saveInterest(Long categoryId);
    List<Map<String, Object>> interestCourses(Long categoryId);

    // --- Course Domain ---
    Map<String, Object> legacyCourse(Long id);
    Map<String, Object> saveLegacyCourse(Map<String, ?> body);
    Map<String, Object> checkCourseName(Map<String, ?> params);
    Map<String, Object> checkBeforeUpShelf(Long id);
    List<Map<String, Object>> simpleCourses();
    List<Map<String, Object>> saveLegacyCatalog(Long courseId, int step, Object payload);
    List<Map<String, Object>> saveLegacyMedia(Long courseId, Object payload);
    Map<String, Object> bindCatalogMedia(Long courseId, Long sectionId, Long mediaId,
                                         String mediaName, Integer durationSeconds);
    void unbindCatalogMedia(Long courseId, Long sectionId, Long mediaId);
    List<Map<String, Object>> saveLegacyTeachers(Long courseId, Map<String, ?> payload);
    List<Map<String, Object>> legacySubjectGroups(Long courseId);
    List<Map<String, Object>> saveLegacySubjects(Long courseId, Object payload);
    Map<String, Object> portalCourses(Map<String, ?> params);
    Map<String, Object> courseStatistics();
    Map<String, Object> course(Long id);
    IPage<EduCourse> pageCourses(String keyword, Long categoryId, Integer status,
                                long pageNo, long pageSize);
    EduCourse saveCourse(EduCourse value);
    void updateCourseStatus(Long id, int status);
    void removeCourses(List<Long> ids);
    List<Map<String, Object>> teachers(Long courseId);
    IPage<EduTeacher> pageTeachers(String keyword, Integer status, long pageNo, long pageSize);
    Map<String, Object> teacherProfile(Long userId);
    Map<String, Object> saveTeacherProfile(Map<String, ?> payload);
    void deleteTeacherProfile(Long userId);
    List<Map<String, Object>> catalogs(Long courseId, boolean onlyLessons);
    IPage<EduCourseCatalog> pageCatalogs(Long courseId, long pageNo, long pageSize);
    List<Map<String, Object>> legacyCatalogs(Long courseId);
    List<Map<String, Object>> courseLikeRanking(int limit);

    // --- Recommend Domain ---
    List<Map<String, Object>> recommendations(String type);
    List<Map<String, Object>> personalizedRecommendations(int limit);
    LearningPathPlan getPersonalizedLearningPath();
    Map<String, Object> refinePersonalizedLearningPath(Map<String, Object> overrides);
    List<ActiveProbeQuestion> getActiveProbingQuestions();
    Map<String, Object> submitActiveProbingAnswers(Map<String, String> answers);
    AgentEvalMetricsVO getAgentEvaluationMetrics();
    SseEmitter streamPersonalizedReasoning(String targetRole);
    default SseEmitter streamPersonalizedReasoning(String targetRole, String query) {
        return streamPersonalizedReasoning(targetRole);
    }
    Map<String, Object> orchestrateAgentRecommend(Long userId, String targetRole, Integer limit, Integer difficulty);
    default Map<String, Object> orchestrateAgentRecommend(Long userId, String targetRole, Integer limit, Integer difficulty, String query) {
        return orchestrateAgentRecommend(userId, targetRole, limit, difficulty);
    }
    default Map<String, Object> orchestrateAgentRecommend(Long userId, String targetRole, Integer limit) {
        return orchestrateAgentRecommend(userId, targetRole, limit, null);
    }
    Map<String, Object> getUserPortrait(Long userId);
    Map<String, Object> updateUserPortraitPreferences(Long userId, Map<String, Object> body);

    // --- Learning Domain ---
    Map<String, Object> learningCourse(Long courseId);
    Map<String, Object> learningRecord(Long lessonId);
    Map<String, Object> enrollCourse(Long courseId);
    Map<String, Object> enrollCourseForUser(Long userId, Long courseId);
    Map<String, Object> revokeCourse(Long courseId);
    Map<String, Object> revokeCourseForUser(Long userId, Long courseId);
    Map<String, Object> restartLearning(Long courseId);
    Map<String, Object> learningPage(long pageNo, long pageSize, boolean current);
    List<Map<String, Object>> plans();
    EduLearningPlan savePlan(EduLearningPlan value);
    void removeLearning(Long courseId);
    EduLearningRecord saveLearning(EduLearningRecord value);

    // --- Interaction Domain ---
    Map<String, Object> questionPage(Map<String, ?> params);
    Map<String, Object> legacyQuestionPage(Map<String, ?> params);
    Map<String, Object> question(Long id);
    EduQuestion saveQuestion(EduQuestion value);
    void removeQuestion(Long id);
    Map<String, Object> reply(Long id);
    void setQuestionHidden(Long id, boolean hidden);
    void setReplyHidden(Long id, boolean hidden);
    Map<String, Object> replyPage(Map<String, ?> params);
    EduReply saveReply(EduReply value);
    Map<String, Object> notePage(Map<String, ?> params);
    EduNote saveNote(EduNote value);
    void removeNote(Long id);
    Map<String, Object> note(Long id);
    void setNoteHidden(Long id, boolean hidden);
    void setNoteVisibility(Long id, boolean visible);
    boolean collectNote(Long noteId, boolean collect);
    boolean like(String bizType, Long bizId, boolean liked);

    // --- Exam Domain ---
    long generateStageExamId();
    boolean isQuestionBankPayload(Map<String, ?> payload);
    Object saveQuestionPayload(Map<String, ?> payload);
    Map<String, Object> checkQuestionName(Map<String, ?> params);
    Object questionOrQuestionBank(Long id);
    void removeQuestionPayload(Long id);
    List<Map<String, Object>> legacyBizQuestions(Long bizId);
    Map<String, Object> examsPage(Map<String, ?> params);
    Map<String, Object> examRecordDetails(Long recordId);
    Map<String, Object> exam(Long id);
    Map<String, Object> examQuestions(Map<String, ?> params);
    EduExamRecord startExam(EduExamRecord value);
    Map<String, Object> submitExam(Map<String, ?> payload);
    IPage<EduExam> pageExams(String keyword, Integer status, long pageNo, long pageSize);
    IPage<EduExamQuestionBank> pageQuestionBank(String keyword, String questionType, Integer status,
                                               long pageNo, long pageSize);
    IPage<EduExamQuestionBank> pageQuestionBank(String keyword, String questionType, Integer status,
                                               Long categoryId, long pageNo, long pageSize);
    EduExam saveExam(EduExam value);
    EduExamQuestionBank saveQuestionBank(EduExamQuestionBank value);

    // --- Dashboard Domain ---
    Map<String, Object> signInfo();
    Map<String, Object> sign();
    Map<String, Object> pointsToday();
    List<Map<String, Object>> pointsBoard(Map<String, ?> params);
    Map<String, Object> statistics();
    List<EduDashboardDaily> dashboardDaily(int days);
    EduDashboardDaily dashboardToday();
    EduDashboardDaily dashboardPrevious(EduDashboardDaily current);
}

