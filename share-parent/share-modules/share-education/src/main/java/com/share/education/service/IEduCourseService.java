package com.share.education.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.share.education.domain.EduCourse;
import com.share.education.domain.EduCourseCatalog;
import com.share.education.domain.EduTeacher;

import java.util.List;
import java.util.Map;

/**
 * 课程主体、章节目录与讲师管理领域接口
 */
public interface IEduCourseService {

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

    Map<String, Object> courseView(EduCourse item);

    Map<String, Object> catalogView(EduCourseCatalog item);

    Map<String, Object> legacyCatalogView(EduCourseCatalog item);

    Map<String, Object> teacherView(EduTeacher item);

    int courseLessonCount(Long courseId);

    String courseName(Long courseId);

    EduCourse requireCourse(Long id);
}
