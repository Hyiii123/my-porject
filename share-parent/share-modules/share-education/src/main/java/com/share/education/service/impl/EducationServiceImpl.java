package com.share.education.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.share.education.ai.evals.AgentEvalMetricsVO;
import com.share.education.ai.model.ActiveProbeQuestion;
import com.share.education.ai.model.LearningPathPlan;
import com.share.education.domain.*;
import com.share.education.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 教育业务聚合服务主实现类（门面聚合各领域细分子服务）
 */
@Slf4j
@Service
@Primary
public class EducationServiceImpl implements IEducationService {

    private final IEduCategoryService categoryService;
    private final IEduCourseService courseService;
    private final IEduRecommendService recommendService;
    private final IEduLearningService learningService;
    private final IEduInteractionService interactionService;
    private final IEduExamService examService;
    private final IEduDashboardService dashboardService;

    public EducationServiceImpl(IEduCategoryService categoryService,
                                IEduCourseService courseService,
                                IEduRecommendService recommendService,
                                IEduLearningService learningService,
                                IEduInteractionService interactionService,
                                IEduExamService examService,
                                IEduDashboardService dashboardService) {
        this.categoryService = categoryService;
        this.courseService = courseService;
        this.recommendService = recommendService;
        this.learningService = learningService;
        this.interactionService = interactionService;
        this.examService = examService;
        this.dashboardService = dashboardService;
    }

    // --- Category Domain ---
    @Override
    public List<Map<String, Object>> listCategories(boolean includeDisabled) {
        return categoryService.listCategories(includeDisabled);
    }

    @Override
    public Map<String, Object> category(Long id) {
        return categoryService.category(id);
    }

    @Override
    public IPage<EduCategory> pageCategories(String keyword, Integer status, long pageNo, long pageSize) {
        return categoryService.pageCategories(keyword, status, pageNo, pageSize);
    }

    @Override
    public EduCategory saveCategory(EduCategory value) {
        return categoryService.saveCategory(value);
    }

    @Override
    public void removeCategories(List<Long> ids) {
        categoryService.removeCategories(ids);
    }

    @Override
    public List<Map<String, Object>> legacyCategories(boolean includeDisabled) {
        return categoryService.legacyCategories(includeDisabled);
    }

    @Override
    public Map<String, Object> saveLegacyCategory(Map<String, ?> body) {
        return categoryService.saveLegacyCategory(body);
    }

    @Override
    public void updateLegacyCategoryStatus(Map<String, ?> body) {
        categoryService.updateLegacyCategoryStatus(body);
    }

    @Override
    public void removeLegacyCategory(Long id) {
        categoryService.removeLegacyCategory(id);
    }

    @Override
    public List<Map<String, Object>> banners() {
        return categoryService.banners();
    }

    @Override
    public List<Map<String, Object>> interests() {
        return categoryService.interests();
    }

    @Override
    public EduInterest saveInterest(Long categoryId) {
        return categoryService.saveInterest(categoryId);
    }

    @Override
    public List<Map<String, Object>> interestCourses(Long categoryId) {
        return categoryService.interestCourses(categoryId);
    }

    // --- Course Domain ---
    @Override
    public Map<String, Object> legacyCourse(Long id) {
        return courseService.legacyCourse(id);
    }

    @Override
    public Map<String, Object> saveLegacyCourse(Map<String, ?> body) {
        return courseService.saveLegacyCourse(body);
    }

    @Override
    public Map<String, Object> checkCourseName(Map<String, ?> params) {
        return courseService.checkCourseName(params);
    }

    @Override
    public Map<String, Object> checkBeforeUpShelf(Long id) {
        return courseService.checkBeforeUpShelf(id);
    }

    @Override
    public List<Map<String, Object>> simpleCourses() {
        return courseService.simpleCourses();
    }

    @Override
    public List<Map<String, Object>> saveLegacyCatalog(Long courseId, int step, Object payload) {
        return courseService.saveLegacyCatalog(courseId, step, payload);
    }

    @Override
    public List<Map<String, Object>> saveLegacyMedia(Long courseId, Object payload) {
        return courseService.saveLegacyMedia(courseId, payload);
    }

    @Override
    public Map<String, Object> bindCatalogMedia(Long courseId, Long sectionId, Long mediaId,
                                                String mediaName, Integer durationSeconds) {
        return courseService.bindCatalogMedia(courseId, sectionId, mediaId, mediaName, durationSeconds);
    }

    @Override
    public void unbindCatalogMedia(Long courseId, Long sectionId, Long mediaId) {
        courseService.unbindCatalogMedia(courseId, sectionId, mediaId);
    }

    @Override
    public List<Map<String, Object>> saveLegacyTeachers(Long courseId, Map<String, ?> payload) {
        return courseService.saveLegacyTeachers(courseId, payload);
    }

    @Override
    public List<Map<String, Object>> legacySubjectGroups(Long courseId) {
        return courseService.legacySubjectGroups(courseId);
    }

    @Override
    public List<Map<String, Object>> saveLegacySubjects(Long courseId, Object payload) {
        return courseService.saveLegacySubjects(courseId, payload);
    }

    @Override
    public Map<String, Object> portalCourses(Map<String, ?> params) {
        return courseService.portalCourses(params);
    }

    @Override
    public Map<String, Object> courseStatistics() {
        return courseService.courseStatistics();
    }

    @Override
    public Map<String, Object> course(Long id) {
        return courseService.course(id);
    }

    @Override
    public IPage<EduCourse> pageCourses(String keyword, Long categoryId, Integer status,
                                        long pageNo, long pageSize) {
        return courseService.pageCourses(keyword, categoryId, status, pageNo, pageSize);
    }

    @Override
    public EduCourse saveCourse(EduCourse value) {
        return courseService.saveCourse(value);
    }

    @Override
    public void updateCourseStatus(Long id, int status) {
        courseService.updateCourseStatus(id, status);
    }

    @Override
    public void removeCourses(List<Long> ids) {
        courseService.removeCourses(ids);
    }

    @Override
    public List<Map<String, Object>> teachers(Long courseId) {
        return courseService.teachers(courseId);
    }

    @Override
    public IPage<EduTeacher> pageTeachers(String keyword, Integer status, long pageNo, long pageSize) {
        return courseService.pageTeachers(keyword, status, pageNo, pageSize);
    }

    @Override
    public Map<String, Object> teacherProfile(Long userId) {
        return courseService.teacherProfile(userId);
    }

    @Override
    public Map<String, Object> saveTeacherProfile(Map<String, ?> payload) {
        return courseService.saveTeacherProfile(payload);
    }

    @Override
    public void deleteTeacherProfile(Long userId) {
        courseService.deleteTeacherProfile(userId);
    }

    @Override
    public List<Map<String, Object>> catalogs(Long courseId, boolean onlyLessons) {
        return courseService.catalogs(courseId, onlyLessons);
    }

    @Override
    public IPage<EduCourseCatalog> pageCatalogs(Long courseId, long pageNo, long pageSize) {
        return courseService.pageCatalogs(courseId, pageNo, pageSize);
    }

    @Override
    public List<Map<String, Object>> legacyCatalogs(Long courseId) {
        return courseService.legacyCatalogs(courseId);
    }

    @Override
    public List<Map<String, Object>> courseLikeRanking(int limit) {
        return courseService.courseLikeRanking(limit);
    }

    // --- Recommend Domain ---
    @Override
    public List<Map<String, Object>> recommendations(String type) {
        return recommendService.recommendations(type);
    }

    @Override
    public List<Map<String, Object>> personalizedRecommendations(int limit) {
        return recommendService.personalizedRecommendations(limit);
    }

    @Override
    public LearningPathPlan getPersonalizedLearningPath() {
        return recommendService.getPersonalizedLearningPath();
    }

    @Override
    public Map<String, Object> refinePersonalizedLearningPath(Map<String, Object> overrides) {
        return recommendService.refinePersonalizedLearningPath(overrides);
    }

    @Override
    public List<ActiveProbeQuestion> getActiveProbingQuestions() {
        return recommendService.getActiveProbingQuestions();
    }

    @Override
    public Map<String, Object> submitActiveProbingAnswers(Map<String, String> answers) {
        return recommendService.submitActiveProbingAnswers(answers);
    }

    @Override
    public AgentEvalMetricsVO getAgentEvaluationMetrics() {
        return recommendService.getAgentEvaluationMetrics();
    }

    @Override
    public SseEmitter streamPersonalizedReasoning(String targetRole) {
        return recommendService.streamPersonalizedReasoning(targetRole);
    }

    @Override
    public Map<String, Object> orchestrateAgentRecommend(Long userId, String targetRole, Integer limit) {
        return recommendService.orchestrateAgentRecommend(userId, targetRole, limit);
    }

    @Override
    public Map<String, Object> getUserPortrait(Long userId) {
        return recommendService.getUserPortrait(userId);
    }

    @Override
    public Map<String, Object> updateUserPortraitPreferences(Long userId, Map<String, Object> body) {
        return recommendService.updateUserPortraitPreferences(userId, body);
    }

    // --- Learning Domain ---
    @Override
    public Map<String, Object> learningCourse(Long courseId) {
        return learningService.learningCourse(courseId);
    }

    @Override
    public Map<String, Object> learningRecord(Long lessonId) {
        return learningService.learningRecord(lessonId);
    }

    @Override
    public Map<String, Object> enrollCourse(Long courseId) {
        return learningService.enrollCourse(courseId);
    }

    @Override
    public Map<String, Object> revokeCourse(Long courseId) {
        return learningService.revokeCourse(courseId);
    }

    @Override
    public Map<String, Object> restartLearning(Long courseId) {
        return learningService.restartLearning(courseId);
    }

    @Override
    public Map<String, Object> learningPage(long pageNo, long pageSize, boolean current) {
        return learningService.learningPage(pageNo, pageSize, current);
    }

    @Override
    public List<Map<String, Object>> plans() {
        return learningService.plans();
    }

    @Override
    public EduLearningPlan savePlan(EduLearningPlan value) {
        return learningService.savePlan(value);
    }

    @Override
    public void removeLearning(Long courseId) {
        learningService.removeLearning(courseId);
    }

    @Override
    public EduLearningRecord saveLearning(EduLearningRecord value) {
        return learningService.saveLearning(value);
    }

    // --- Interaction Domain ---
    @Override
    public Map<String, Object> questionPage(Map<String, ?> params) {
        return interactionService.questionPage(params);
    }

    @Override
    public Map<String, Object> legacyQuestionPage(Map<String, ?> params) {
        return interactionService.legacyQuestionPage(params);
    }

    @Override
    public Map<String, Object> question(Long id) {
        return interactionService.question(id);
    }

    @Override
    public EduQuestion saveQuestion(EduQuestion value) {
        return interactionService.saveQuestion(value);
    }

    @Override
    public void removeQuestion(Long id) {
        interactionService.removeQuestion(id);
    }

    @Override
    public Map<String, Object> reply(Long id) {
        return interactionService.reply(id);
    }

    @Override
    public void setQuestionHidden(Long id, boolean hidden) {
        interactionService.setQuestionHidden(id, hidden);
    }

    @Override
    public void setReplyHidden(Long id, boolean hidden) {
        interactionService.setReplyHidden(id, hidden);
    }

    @Override
    public Map<String, Object> replyPage(Map<String, ?> params) {
        return interactionService.replyPage(params);
    }

    @Override
    public EduReply saveReply(EduReply value) {
        return interactionService.saveReply(value);
    }

    @Override
    public Map<String, Object> notePage(Map<String, ?> params) {
        return interactionService.notePage(params);
    }

    @Override
    public EduNote saveNote(EduNote value) {
        return interactionService.saveNote(value);
    }

    @Override
    public void removeNote(Long id) {
        interactionService.removeNote(id);
    }

    @Override
    public Map<String, Object> note(Long id) {
        return interactionService.note(id);
    }

    @Override
    public void setNoteHidden(Long id, boolean hidden) {
        interactionService.setNoteHidden(id, hidden);
    }

    @Override
    public void setNoteVisibility(Long id, boolean visible) {
        interactionService.setNoteVisibility(id, visible);
    }

    @Override
    public boolean collectNote(Long noteId, boolean collect) {
        return interactionService.collectNote(noteId, collect);
    }

    @Override
    public boolean like(String bizType, Long bizId, boolean liked) {
        return interactionService.like(bizType, bizId, liked);
    }

    // --- Exam Domain ---
    @Override
    public long generateStageExamId() {
        return examService.generateStageExamId();
    }

    @Override
    public boolean isQuestionBankPayload(Map<String, ?> payload) {
        return examService.isQuestionBankPayload(payload);
    }

    @Override
    public Object saveQuestionPayload(Map<String, ?> payload) {
        return examService.saveQuestionPayload(payload);
    }

    @Override
    public Map<String, Object> checkQuestionName(Map<String, ?> params) {
        return examService.checkQuestionName(params);
    }

    @Override
    public Object questionOrQuestionBank(Long id) {
        return examService.questionOrQuestionBank(id);
    }

    @Override
    public void removeQuestionPayload(Long id) {
        examService.removeQuestionPayload(id);
    }

    @Override
    public List<Map<String, Object>> legacyBizQuestions(Long bizId) {
        return examService.legacyBizQuestions(bizId);
    }

    @Override
    public Map<String, Object> examsPage(Map<String, ?> params) {
        return examService.examsPage(params);
    }

    @Override
    public Map<String, Object> examRecordDetails(Long recordId) {
        return examService.examRecordDetails(recordId);
    }

    @Override
    public Map<String, Object> exam(Long id) {
        return examService.exam(id);
    }

    @Override
    public Map<String, Object> examQuestions(Map<String, ?> params) {
        return examService.examQuestions(params);
    }

    @Override
    public EduExamRecord startExam(EduExamRecord value) {
        return examService.startExam(value);
    }

    @Override
    public Map<String, Object> submitExam(Map<String, ?> payload) {
        return examService.submitExam(payload);
    }

    @Override
    public IPage<EduExam> pageExams(String keyword, Integer status, long pageNo, long pageSize) {
        return examService.pageExams(keyword, status, pageNo, pageSize);
    }

    @Override
    public IPage<EduExamQuestionBank> pageQuestionBank(String keyword, String questionType, Integer status,
                                                      long pageNo, long pageSize) {
        return examService.pageQuestionBank(keyword, questionType, status, pageNo, pageSize);
    }

    @Override
    public IPage<EduExamQuestionBank> pageQuestionBank(String keyword, String questionType, Integer status,
                                                      Long categoryId, long pageNo, long pageSize) {
        return examService.pageQuestionBank(keyword, questionType, status, categoryId, pageNo, pageSize);
    }

    @Override
    public EduExam saveExam(EduExam value) {
        return examService.saveExam(value);
    }

    @Override
    public EduExamQuestionBank saveQuestionBank(EduExamQuestionBank value) {
        return examService.saveQuestionBank(value);
    }

    // --- Dashboard Domain ---
    @Override
    public Map<String, Object> signInfo() {
        return dashboardService.signInfo();
    }

    @Override
    public Map<String, Object> sign() {
        return dashboardService.sign();
    }

    @Override
    public Map<String, Object> pointsToday() {
        return dashboardService.pointsToday();
    }

    @Override
    public List<Map<String, Object>> pointsBoard(Map<String, ?> params) {
        return dashboardService.pointsBoard(params);
    }

    @Override
    public Map<String, Object> statistics() {
        return dashboardService.statistics();
    }

    @Override
    public List<EduDashboardDaily> dashboardDaily(int days) {
        return dashboardService.dashboardDaily(days);
    }

    @Override
    public EduDashboardDaily dashboardToday() {
        return dashboardService.dashboardToday();
    }

    @Override
    public EduDashboardDaily dashboardPrevious(EduDashboardDaily current) {
        return dashboardService.dashboardPrevious(current);
    }
}
