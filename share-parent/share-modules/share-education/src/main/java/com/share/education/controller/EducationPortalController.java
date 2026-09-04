package com.share.education.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.education.domain.EduExamRecord;
import com.share.education.domain.EduInterest;
import com.share.education.domain.EduLearningPlan;
import com.share.education.domain.EduLearningRecord;
import com.share.education.domain.EduNote;
import com.share.education.domain.EduQuestion;
import com.share.education.domain.EduReply;
import com.share.education.service.EducationService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面向用户端的教育领域兼容接口。
 *
 * <p>Gateway 将原有 /cs、/ss、/ls、/es、/rs 前缀去掉后转发到这里，
 * 所以接口路径仍与原 Vue 项目的调用约定一致。</p>
 */
@RestController
public class EducationPortalController extends BaseController {
    private final EducationService educationService;

    public EducationPortalController(EducationService educationService) {
        this.educationService = educationService;
    }

    @GetMapping({"/categorys/all", "/categorys/list"})
    public AjaxResult categories(@RequestParam(required = false, defaultValue = "false") boolean includeDisabled) {
        return success(educationService.listCategories(includeDisabled));
    }

    @GetMapping("/categorys/{id}")
    public AjaxResult category(@PathVariable Long id) {
        return success(educationService.category(id));
    }

    @PostMapping("/categorys/add")
    @RequiresPermissions("education:category:add")
    public AjaxResult addLegacyCategory(@RequestBody Map<String, Object> body) {
        return success(educationService.saveLegacyCategory(body));
    }

    @PutMapping("/categorys/update")
    @RequiresPermissions("education:category:edit")
    public AjaxResult updateLegacyCategory(@RequestBody Map<String, Object> body) {
        return success(educationService.saveLegacyCategory(body));
    }

    @PutMapping("/categorys/disableOrEnable")
    @RequiresPermissions("education:category:edit")
    public AjaxResult updateLegacyCategoryStatus(@RequestBody(required = false) Map<String, Object> body) {
        educationService.updateLegacyCategoryStatus(body == null ? Map.of() : body);
        return success();
    }

    @DeleteMapping("/categorys/{id}")
    @RequiresPermissions("education:category:remove")
    public AjaxResult deleteLegacyCategory(@PathVariable Long id) {
        educationService.removeLegacyCategory(id);
        return success();
    }

    @GetMapping("/courses/page")
    public AjaxResult coursePage(@RequestParam Map<String, Object> params) {
        return success(educationService.portalCourses(params));
    }

    @GetMapping("/courses/simpleInfo/list")
    public AjaxResult simpleCourses() {
        return success(educationService.simpleCourses());
    }

    @GetMapping("/courses/portal")
    public AjaxResult coursePortal(@RequestParam Map<String, Object> params) {
        return success(educationService.portalCourses(params));
    }

    @GetMapping("/courses/baseInfo/{id}")
    public AjaxResult course(@PathVariable Long id) {
        return success(educationService.legacyCourse(id));
    }

    @PostMapping("/courses/baseInfo/save")
    @RequiresPermissions("education:course:edit")
    public AjaxResult saveCourse(@RequestBody Map<String, Object> body) {
        return success(educationService.saveLegacyCourse(body));
    }

    /** 旧用户端课程学习页使用的目录入口。 */
    @GetMapping("/courses/{id}/catalogs")
    public AjaxResult courseCatalogs(@PathVariable Long id) {
        return success(educationService.legacyCatalogs(id));
    }

    /** 旧管理端课程删除入口，保留单个课程编号的调用契约。 */
    @DeleteMapping("/courses/delete/{id}")
    @RequiresPermissions("education:course:remove")
    public AjaxResult deleteCourse(@PathVariable Long id) {
        educationService.removeCourses(List.of(id));
        return success();
    }

    /** 旧管理端添加阶段测试前先申请一个目录编号。 */
    @GetMapping("/courses/generator")
    @RequiresPermissions("education:catalog:add")
    public AjaxResult generateStageExam() {
        return success(Map.of("id", educationService.generateStageExamId()));
    }

    @PostMapping("/courses/catas/save/{id}/{step}")
    @RequiresPermissions("education:catalog:edit")
    public AjaxResult saveCatalog(@PathVariable Long id, @PathVariable int step,
            @RequestBody(required = false) Object body) {
        return success(educationService.saveLegacyCatalog(id, step, body));
    }

    @PostMapping("/courses/media/save/{id}")
    @RequiresPermissions("education:catalog:edit")
    public AjaxResult saveMedia(@PathVariable Long id, @RequestBody(required = false) Object body) {
        return success(educationService.saveLegacyMedia(id, body));
    }

    @GetMapping("/courses/subjects/get/{id}")
    public AjaxResult subjects(@PathVariable Long id) {
        return success(educationService.legacySubjectGroups(id));
    }

    @PostMapping("/courses/subjects/save/{id}")
    @RequiresPermissions("education:catalog:edit")
    public AjaxResult saveSubjects(@PathVariable Long id, @RequestBody(required = false) Object body) {
        // 旧管理端实际发送的是题目关系数组；同时兼容 {datas: [...]} 这类包装请求。
        return success(educationService.saveLegacySubjects(id, body));
    }

    @PostMapping("/courses/teachers/save")
    @RequiresPermissions("education:teacher:edit")
    public AjaxResult saveTeachers(@RequestBody Map<String, Object> body) {
        Long courseId = educationServiceLong(body, "id");
        return success(educationService.saveLegacyTeachers(courseId, body));
    }

    @PostMapping("/courses/upShelf")
    @RequiresPermissions("education:course:edit")
    public AjaxResult upShelf(@RequestBody Map<String, Object> body) {
        Long id = educationServiceLong(body, "id");
        educationService.updateCourseStatus(id, 1);
        return success();
    }

    @PostMapping("/courses/downShelf")
    @RequiresPermissions("education:course:edit")
    public AjaxResult downShelf(@RequestBody Map<String, Object> body) {
        Long id = educationServiceLong(body, "id");
        educationService.updateCourseStatus(id, 2);
        return success();
    }

    @GetMapping("/courses/checkBeforeUpShelf/{id}")
    @RequiresPermissions("education:course:edit")
    public AjaxResult checkBeforeUpShelf(@PathVariable Long id) {
        return success(educationService.checkBeforeUpShelf(id));
    }

    @GetMapping("/courses/checkName")
    @RequiresPermissions("education:course:add")
    public AjaxResult checkCourseName(@RequestParam Map<String, Object> params) {
        return success(educationService.checkCourseName(params));
    }

    @GetMapping("/courses/teachers")
    public AjaxResult allTeachers() {
        return success(educationService.teachers(null));
    }

    @GetMapping("/courses/teachers/{id}")
    public AjaxResult courseTeachers(@PathVariable Long id) {
        return success(educationService.teachers(id));
    }

    @GetMapping({"/courses/catas/{id}", "/courses/catalogs/{id}"})
    public AjaxResult catalogs(@PathVariable Long id) {
        return success(educationService.legacyCatalogs(id));
    }

    @GetMapping("/courses/catas/index/list/{id}")
    public AjaxResult lessons(@PathVariable Long id) {
        return success(educationService.catalogs(id, true));
    }

    @GetMapping("/recommend/{type}")
    public AjaxResult recommendations(@PathVariable String type) {
        return success(educationService.recommendations(type));
    }

    @GetMapping("/banners")
    public AjaxResult banners() {
        return success(educationService.banners());
    }

    @RequiresLogin
    @GetMapping("/interests")
    public AjaxResult interests() {
        return success(educationService.interests());
    }

    @RequiresLogin
    @PostMapping("/interests")
    public AjaxResult saveInterest(@RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) Long categoryId) {
        Long id = categoryId == null ? educationServiceLong(body, "categoryId") : categoryId;
        return success(educationService.saveInterest(id));
    }

    @GetMapping("/interests/{id}/courses")
    public AjaxResult interestCourses(@PathVariable Long id) {
        return success(educationService.interestCourses(id));
    }

    @RequiresLogin
    @GetMapping("/lessons/{courseId}")
    public AjaxResult learningCourse(@PathVariable Long courseId) {
        return success(educationService.learningCourse(courseId));
    }

    /** 交易服务完成支付后调用，创建当前用户的学习记录。 */
    @RequiresLogin
    @PostMapping("/internal/enrollments/{courseId}")
    public AjaxResult enrollCourse(@PathVariable Long courseId) {
        return success(educationService.enrollCourse(courseId));
    }

    /**
     * 重置当前用户指定课程的学习进度，保留报名关系和课程本身。
     */
    @RequiresLogin
    @PutMapping("/lessons/{courseId}/restart")
    public AjaxResult restartLearning(@PathVariable Long courseId) {
        return success(educationService.restartLearning(courseId));
    }

    @RequiresLogin
    @GetMapping("/lessons/page")
    public AjaxResult learningPage(@RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize) {
        return success(educationService.learningPage(pageNo, pageSize, false));
    }

    @RequiresLogin
    @GetMapping("/lessons/now")
    public AjaxResult learningNow(@RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize) {
        return success(educationService.learningPage(pageNo, pageSize, true));
    }

    @RequiresLogin
    @GetMapping({"/lessons/plans", "/plans"})
    public AjaxResult plans() {
        return success(educationService.plans());
    }

    @RequiresLogin
    @PostMapping({"/lessons/plans", "/plans"})
    public AjaxResult savePlan(@RequestBody EduLearningPlan plan) {
        return success(educationService.savePlan(plan));
    }

    @RequiresLogin
    @DeleteMapping("/lessons/{courseId}")
    public AjaxResult removeLearning(@PathVariable Long courseId) {
        educationService.removeLearning(courseId);
        return success();
    }

    @RequiresLogin
    @GetMapping("/learning-records/lessons/{lessonId}")
    public AjaxResult learningLog(@PathVariable Long lessonId) {
        return success(educationService.learningRecord(lessonId));
    }

    @RequiresLogin
    @PostMapping("/learning-records")
    public AjaxResult saveLearning(@RequestBody EduLearningRecord record) {
        return success(educationService.saveLearning(record));
    }

    @GetMapping("/questions/page")
    public AjaxResult questionPage(@RequestParam Map<String, Object> params) {
        return success(educationService.questionPage(params));
    }

    @GetMapping("/questions/checkName")
    public AjaxResult checkQuestionName(@RequestParam Map<String, Object> params) {
        return success(educationService.checkQuestionName(params));
    }

    @GetMapping("/questions/listOfBiz")
    public AjaxResult questionsOfBiz(@RequestParam(required = false) Long bizId) {
        return success(educationService.legacyBizQuestions(bizId));
    }

    @PostMapping({"/questions", "/questions/add"})
    @RequiresLogin
    public AjaxResult saveQuestion(@RequestBody Map<String, Object> body) {
        return success(educationService.saveQuestionPayload(body));
    }

    @GetMapping("/questions/{id}")
    public AjaxResult question(@PathVariable Long id) {
        return success(educationService.questionOrQuestionBank(id));
    }

    @RequiresLogin
    @PutMapping("/questions/{id}")
    public AjaxResult updateQuestion(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> value = new LinkedHashMap<>(body == null ? Map.of() : body);
        value.put("id", id);
        return success(educationService.saveQuestionPayload(value));
    }

    @RequiresLogin
    @DeleteMapping("/questions/{id}")
    public AjaxResult removeQuestion(@PathVariable Long id) {
        educationService.removeQuestionPayload(id);
        return success();
    }

    @GetMapping("/replies/page")
    public AjaxResult replyPage(@RequestParam Map<String, Object> params) {
        return success(educationService.replyPage(params));
    }

    @RequiresLogin
    @PostMapping("/replies")
    public AjaxResult saveReply(@RequestBody EduReply reply) {
        return success(educationService.saveReply(reply));
    }

    @GetMapping("/notes/page")
    public AjaxResult notePage(@RequestParam Map<String, Object> params) {
        return success(educationService.notePage(params));
    }

    @RequiresLogin
    @PostMapping({"/notes", "/notes/add"})
    public AjaxResult saveNote(@RequestBody EduNote note) {
        return success(educationService.saveNote(note));
    }

    /** 用户端笔记详情，兼容原天机前端的 GET /ls/notes/{id} 调用。 */
    @RequiresLogin
    @GetMapping("/notes/{id}")
    public AjaxResult note(@PathVariable Long id) {
        return success(educationService.note(id));
    }

    @RequiresLogin
    @PutMapping("/notes/{id}")
    public AjaxResult updateNote(@PathVariable Long id, @RequestBody EduNote note) {
        note.setId(id);
        return success(educationService.saveNote(note));
    }

    @RequiresLogin
    @DeleteMapping("/notes/{id}")
    public AjaxResult removeNote(@PathVariable Long id) {
        educationService.removeNote(id);
        return success();
    }

    @RequiresLogin
    @PostMapping({"/notes/{id}", "/notes/gathers/{id}"})
    public AjaxResult collectNote(@PathVariable Long id) {
        return success(educationService.collectNote(id, true));
    }

    @RequiresLogin
    @DeleteMapping("/notes/gathers/{id}")
    public AjaxResult uncollectNote(@PathVariable Long id) {
        return success(educationService.collectNote(id, false));
    }

    @RequiresLogin
    @PostMapping("/note/{id}")
    public AjaxResult likeNote(@PathVariable Long id) {
        return success(educationService.like("NOTE", id, true));
    }

    @RequiresLogin
    @DeleteMapping("/note/{id}")
    public AjaxResult unlikeNote(@PathVariable Long id) {
        return success(educationService.like("NOTE", id, false));
    }

    @PostMapping("/likes")
    @RequiresLogin
    public AjaxResult like(@RequestBody Map<String, Object> body) {
        Long id = educationServiceLong(body, "bizId");
        boolean liked = educationServiceBoolean(body, "liked");
        return success(educationService.like(String.valueOf(body.getOrDefault("bizType", "QA")), id, liked));
    }

    @RequiresLogin
    @GetMapping("/exams/page")
    public AjaxResult examPage(@RequestParam Map<String, Object> params) {
        return success(educationService.examsPage(params));
    }

    @GetMapping("/exams/{id}")
    public AjaxResult exam(@PathVariable Long id) {
        return success(educationService.exam(id));
    }

    @RequiresLogin
    @GetMapping("/exam-records/{id}")
    public AjaxResult examRecordDetails(@PathVariable Long id) {
        return success(educationService.examRecordDetails(id));
    }

    @PostMapping("/exams")
    public AjaxResult examQuestions(@RequestBody(required = false) Map<String, Object> body) {
        return success(educationService.examQuestions(body == null ? Map.of() : body));
    }

    @PostMapping("/exams/details")
    @RequiresLogin
    public AjaxResult submitExam(@RequestBody Map<String, Object> body) {
        return success(educationService.submitExam(body));
    }

    @PostMapping("/exam-records")
    @RequiresLogin
    public AjaxResult startExam(@RequestBody EduExamRecord record) {
        return success(educationService.startExam(record));
    }

    @PostMapping("/exam-records/details")
    @RequiresLogin
    public AjaxResult submitExamRecord(@RequestBody Map<String, Object> body) {
        return success(educationService.submitExam(body));
    }

    @GetMapping("/sign-records")
    @RequiresLogin
    public AjaxResult signInfo() {
        return success(educationService.signInfo());
    }

    @PostMapping("/sign-records")
    @RequiresLogin
    public AjaxResult sign() {
        return success(educationService.sign());
    }

    @GetMapping("/points/today")
    @RequiresLogin
    public AjaxResult pointsToday() {
        return success(educationService.pointsToday());
    }

    @GetMapping("/boards")
    public AjaxResult pointsBoard(@RequestParam Map<String, Object> params) {
        return success(educationService.pointsBoard(params));
    }

    @GetMapping("/boards/seasons/list")
    public AjaxResult seasons() {
        return success(List.of(Map.of("id", 0, "name", "2026 学习赛季", "value", 0)));
    }

    private Long educationServiceLong(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) return null;
        try { return Long.valueOf(String.valueOf(body.get(key))); } catch (NumberFormatException ignored) { return null; }
    }

    private boolean educationServiceBoolean(Map<String, Object> body, String key) {
        Object value = body == null ? null : body.get(key);
        return value instanceof Boolean b ? b : "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value));
    }
}
