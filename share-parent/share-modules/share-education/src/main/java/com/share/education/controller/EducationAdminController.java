package com.share.education.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.share.common.core.constant.HttpStatus;
import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.log.annotation.Log;
import com.share.common.log.enums.BusinessType;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.education.domain.EduCategory;
import com.share.education.domain.EduCourse;
import com.share.education.domain.EduCourseCatalog;
import com.share.education.domain.EduExam;
import com.share.education.domain.EduExamQuestionBank;
import com.share.education.domain.EduTeacher;
import com.share.education.service.EducationService;
import java.util.Arrays;
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

/** 天机学堂管理端课程、题库和教育统计接口。 */
@RestController
@RequestMapping("/admin")
public class EducationAdminController extends BaseController {
    private final EducationService educationService;

    public EducationAdminController(EducationService educationService) {
        this.educationService = educationService;
    }

    @RequiresPermissions("education:category:list")
    @GetMapping({"/categories/list", "/categorys/list"})
    public TableDataInfo categories(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status, @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize) {
        return page(educationService.pageCategories(keyword, status, pageNo, pageSize));
    }

    @RequiresPermissions("education:category:query")
    @GetMapping({"/categories/{id}", "/categorys/{id}"})
    public AjaxResult category(@PathVariable Long id) {
        return success(educationService.category(id));
    }

    @RequiresPermissions("education:category:add")
    @Log(title = "课程分类", businessType = BusinessType.INSERT)
    @PostMapping({"/categories", "/categorys"})
    public AjaxResult addCategory(@RequestBody EduCategory value) {
        return success(educationService.saveCategory(value));
    }

    @RequiresPermissions("education:category:edit")
    @Log(title = "课程分类", businessType = BusinessType.UPDATE)
    @PutMapping({"/categories", "/categorys"})
    public AjaxResult updateCategory(@RequestBody EduCategory value) {
        return success(educationService.saveCategory(value));
    }

    @RequiresPermissions("education:category:remove")
    @DeleteMapping({"/categories/{ids}", "/categorys/{ids}"})
    public AjaxResult removeCategory(@PathVariable Long[] ids) {
        educationService.removeCategories(Arrays.asList(ids));
        return success();
    }

    @RequiresPermissions("education:course:list")
    @GetMapping("/courses/list")
    public TableDataInfo courses(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId, @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNo, @RequestParam(defaultValue = "10") long pageSize) {
        return page(educationService.pageCourses(keyword, categoryId, status, pageNo, pageSize));
    }

    @RequiresPermissions("education:course:query")
    @GetMapping("/courses/{id}")
    public AjaxResult course(@PathVariable Long id) {
        return success(educationService.course(id));
    }

    @RequiresPermissions("education:course:add")
    @Log(title = "课程", businessType = BusinessType.INSERT)
    @PostMapping("/courses")
    public AjaxResult addCourse(@RequestBody EduCourse value) {
        return success(educationService.saveCourse(value));
    }

    @RequiresPermissions("education:course:edit")
    @Log(title = "课程", businessType = BusinessType.UPDATE)
    @PutMapping("/courses")
    public AjaxResult updateCourse(@RequestBody EduCourse value) {
        return success(educationService.saveCourse(value));
    }

    @RequiresPermissions("education:course:edit")
    @PostMapping("/courses/{id}/shelf")
    public AjaxResult shelf(@PathVariable Long id, @RequestParam(defaultValue = "1") int status) {
        educationService.updateCourseStatus(id, status);
        return success();
    }

    @RequiresPermissions("education:course:remove")
    @DeleteMapping("/courses/{ids}")
    public AjaxResult removeCourse(@PathVariable Long[] ids) {
        educationService.removeCourses(Arrays.asList(ids));
        return success();
    }

    @RequiresPermissions("education:teacher:list")
    @GetMapping("/teachers/list")
    public TableDataInfo teachers(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status, @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize) {
        return page(educationService.pageTeachers(keyword, status, pageNo, pageSize));
    }

    @RequiresPermissions("education:catalog:list")
    @GetMapping("/catalogs/list")
    public TableDataInfo catalogs(@RequestParam(required = false) Long courseId,
            @RequestParam(defaultValue = "1") long pageNo, @RequestParam(defaultValue = "20") long pageSize) {
        return page(educationService.pageCatalogs(courseId, pageNo, pageSize));
    }

    @RequiresPermissions("education:exam:list")
    @GetMapping("/exams/list")
    public TableDataInfo exams(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status, @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize) {
        return page(educationService.pageExams(keyword, status, pageNo, pageSize));
    }

    @RequiresPermissions("education:exam:add")
    @PostMapping("/exams")
    public AjaxResult addExam(@RequestBody EduExam value) {
        return success(educationService.saveExam(value));
    }

    @RequiresPermissions("education:exam:edit")
    @PutMapping("/exams")
    public AjaxResult updateExam(@RequestBody EduExam value) {
        return success(educationService.saveExam(value));
    }

    @RequiresPermissions("education:question:list")
    @GetMapping("/question-bank/list")
    public TableDataInfo questionBank(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String questionType, @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNo, @RequestParam(defaultValue = "10") long pageSize) {
        return page(educationService.pageQuestionBank(keyword, questionType, status, pageNo, pageSize));
    }

    @RequiresPermissions("education:question:add")
    @PostMapping("/question-bank")
    public AjaxResult addQuestion(@RequestBody EduExamQuestionBank value) {
        return success(educationService.saveQuestionBank(value));
    }

    @RequiresPermissions("education:question:edit")
    @PutMapping("/question-bank")
    public AjaxResult updateQuestion(@RequestBody EduExamQuestionBank value) {
        return success(educationService.saveQuestionBank(value));
    }

    /** 旧互动管理页接口（/ls/admin/questions、/replies、/notes）。 */
    @RequiresPermissions("education:interaction:list")
    @GetMapping("/questions/page")
    public TableDataInfo legacyQuestions(@RequestParam Map<String, Object> params) {
        return pageView(educationService.questionPage(params));
    }

    @RequiresPermissions("education:interaction:query")
    @GetMapping("/questions/{id}")
    public AjaxResult legacyQuestion(@PathVariable Long id) {
        return success(educationService.question(id));
    }

    @RequiresPermissions("education:interaction:list")
    @GetMapping("/replies/page")
    public TableDataInfo legacyReplies(@RequestParam Map<String, Object> params) {
        return pageView(educationService.replyPage(params));
    }

    @RequiresPermissions("education:interaction:query")
    @GetMapping("/replies/{id}")
    public AjaxResult legacyReply(@PathVariable Long id) {
        return success(educationService.reply(id));
    }

    @RequiresPermissions("education:interaction:edit")
    @PutMapping("/questions/{id}/hidden/{hidden}")
    public AjaxResult hideQuestion(@PathVariable Long id, @PathVariable boolean hidden) {
        educationService.setQuestionHidden(id, hidden);
        return success();
    }

    @RequiresPermissions("education:interaction:edit")
    @PutMapping("/replies/{id}/hidden/{hidden}")
    public AjaxResult hideReply(@PathVariable Long id, @PathVariable boolean hidden) {
        educationService.setReplyHidden(id, hidden);
        return success();
    }

    @RequiresPermissions("education:note:list")
    @GetMapping("/notes/page")
    public TableDataInfo legacyNotes(@RequestParam Map<String, Object> params) {
        return pageView(educationService.notePage(params));
    }

    @RequiresPermissions("education:note:query")
    @GetMapping("/notes/{id}")
    public AjaxResult legacyNote(@PathVariable Long id) {
        return success(educationService.note(id));
    }

    @RequiresPermissions("education:note:edit")
    @PutMapping("/notes/{id}/hidden/{hidden}")
    public AjaxResult hideNote(@PathVariable Long id, @PathVariable boolean hidden) {
        educationService.setNoteHidden(id, hidden);
        return success();
    }

    @RequiresPermissions("education:note:edit")
    @PutMapping("/notes/{id}/visibility/{visibility}")
    public AjaxResult setNoteVisibility(@PathVariable Long id, @PathVariable boolean visibility) {
        educationService.setNoteVisibility(id, visibility);
        return success();
    }

    @RequiresPermissions("education:note:remove")
    @DeleteMapping("/notes/{id}")
    public AjaxResult removeNote(@PathVariable Long id) {
        educationService.removeNote(id);
        return success();
    }

    @RequiresPermissions("education:statistics:view")
    @GetMapping("/statistics/overview")
    public AjaxResult statistics() {
        return success(educationService.statistics());
    }

    private TableDataInfo page(IPage<?> data) {
        TableDataInfo result = new TableDataInfo();
        result.setCode(HttpStatus.SUCCESS);
        result.setMsg("查询成功");
        result.setRows(data.getRecords());
        result.setTotal(data.getTotal());
        return result;
    }

    @SuppressWarnings("unchecked")
    private TableDataInfo pageView(Map<String, Object> data) {
        TableDataInfo result = new TableDataInfo();
        result.setCode(HttpStatus.SUCCESS);
        result.setMsg("查询成功");
        result.setRows((java.util.List<?>) data.getOrDefault("list", java.util.List.of()));
        Object total = data.get("total");
        result.setTotal(total instanceof Number number ? number.longValue() : 0L);
        return result;
    }
}
