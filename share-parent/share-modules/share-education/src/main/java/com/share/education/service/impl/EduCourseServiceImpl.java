package com.share.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.common.core.exception.ServiceException;
import com.share.common.redis.service.RedisService;
import com.share.common.security.utils.SecurityUtils;
import com.share.education.domain.*;
import com.share.education.mapper.*;
import com.share.education.service.IEduCourseService;
import com.share.education.service.support.EduUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static com.share.education.service.support.EduUtils.*;

/**
 * 课程主体、章节目录与讲师领域服务实现
 */
@Slf4j
@Service
@Primary
public class EduCourseServiceImpl implements IEduCourseService {

    private static final int ENABLED = 1;

    private final EduCourseMapper courseMapper;
    private final EduCourseCatalogMapper catalogMapper;
    private final EduCatalogQuestionMapper catalogQuestionMapper;
    private final EduCourseTeacherMapper courseTeacherMapper;
    private final EduTeacherMapper teacherMapper;
    private final EduCategoryMapper categoryMapper;
    private final EduExamQuestionBankMapper questionBankMapper;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public EduCourseServiceImpl(EduCourseMapper courseMapper,
                                EduCourseCatalogMapper catalogMapper,
                                EduCatalogQuestionMapper catalogQuestionMapper,
                                EduCourseTeacherMapper courseTeacherMapper,
                                EduTeacherMapper teacherMapper,
                                EduCategoryMapper categoryMapper,
                                EduExamQuestionBankMapper questionBankMapper,
                                RedisService redisService,
                                ObjectMapper objectMapper) {
        this.courseMapper = courseMapper;
        this.catalogMapper = catalogMapper;
        this.catalogQuestionMapper = catalogQuestionMapper;
        this.courseTeacherMapper = courseTeacherMapper;
        this.teacherMapper = teacherMapper;
        this.categoryMapper = categoryMapper;
        this.questionBankMapper = questionBankMapper;
        this.redisService = redisService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> legacyCourse(Long id) {
        EduCourse course = requireCourse(id);
        Map<String, Object> result = courseView(course);
        String categoryName = String.valueOf(result.getOrDefault("categoryName", ""));
        result.put("name", course.getCourseName());
        result.put("detail", cleanHtmlTags(course.getDescription()));
        result.put("introduce", cleanHtmlTags(course.getShortDescription()));
        result.put("usePeople", StringUtils.hasText(course.getPrerequisites()) ? course.getPrerequisites() : "具备基础IT知识与编程兴趣的学员");
        result.put("free", Integer.valueOf(1).equals(course.getIsFree()) ? Boolean.TRUE : Boolean.FALSE);
        result.put("thirdCateId", course.getCategoryId() == null ? List.of() : List.of(course.getCategoryId()));
        result.put("cateNames", categoryName);
        result.put("purchaseEndTime", null);
        result.put("validDuration", 9999);
        result.put("step", 1);
        result.put("canUpdate", Boolean.TRUE);
        result.put("chapters", legacyCatalogs(id));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> saveLegacyCourse(Map<String, ?> body) {
        Long id = longValue(body == null ? null : body.get("id"));
        EduCourse value = id == null ? new EduCourse() : courseMapper.selectById(id);
        if (value == null) throw new ServiceException("课程不存在");
        String name = defaultText(body == null ? null : body.get("name"),
                defaultText(body == null ? null : body.get("title"),
                        defaultText(body == null ? null : body.get("courseName"), null)));
        require(StringUtils.hasText(name), "课程名称不能为空");
        Long categoryId = firstLong(body == null ? null : body.get("thirdCateId"));
        if (categoryId == null) categoryId = longValue(body == null ? null : body.get("categoryId"));
        require(categoryId != null, "课程分类不能为空");
        value.setCourseName(name.trim());
        value.setCategoryId(categoryId);
        value.setCoverUrl(defaultText(body == null ? null : body.get("coverUrl"),
                defaultText(body == null ? null : body.get("cover"), value.getCoverUrl())));
        value.setShortDescription(cleanHtmlTags(defaultText(body == null ? null : body.get("introduce"), value.getShortDescription())));
        value.setDescription(cleanHtmlTags(defaultText(body == null ? null : body.get("detail"),
                defaultText(body == null ? null : body.get("description"), value.getDescription()))));
        boolean free = bool(body == null ? null : body.get("free")) || bool(body == null ? null : body.get("isFree"));
        BigDecimal price = moneyYuan(body == null ? null : body.get("price"), value.getPrice());
        value.setIsFree(free ? 1 : 0);
        value.setPrice(free ? BigDecimal.ZERO : defaultValue(price, BigDecimal.ZERO));
        value.setOriginalPrice(defaultValue(value.getOriginalPrice(), value.getPrice()));
        value.setLessonCount(intValue(body == null ? null : body.get("lessons"),
                intValue(body == null ? null : body.get("lessonCount"), defaultValue(value.getLessonCount(), 0))));
        value.setStatus(defaultValue(value.getStatus(), 0));
        LocalDateTime now = LocalDateTime.now();
        if (value.getId() == null) {
            value.setId(newId());
            value.setLearnerCount(defaultValue(value.getLearnerCount(), 0));
            value.setDurationMinutes(defaultValue(value.getDurationMinutes(), 0));
            value.setRating(defaultValue(value.getRating(), BigDecimal.ZERO));
            value.setIsRecommended(defaultValue(value.getIsRecommended(), 0));
            value.setIsHot(defaultValue(value.getIsHot(), 0));
            value.setIsNew(defaultValue(value.getIsNew(), 1));
            value.setSortNum(defaultValue(value.getSortNum(), 0));
            value.setCreateTime(now);
            value.setDelFlag(0);
            value.setVersion(0);
            value.setUpdateTime(now);
            courseMapper.insert(value);
        } else {
            value.setUpdateTime(now);
            courseMapper.updateById(value);
        }
        return legacyCourse(value.getId());
    }

    @Override
    public Map<String, Object> checkCourseName(Map<String, ?> params) {
        String name = defaultText(params == null ? null : params.get("name"),
                defaultText(params == null ? null : params.get("courseName"), null));
        Long id = longValue(params == null ? null : params.get("id"));
        boolean existed = StringUtils.hasText(name) && courseMapper.selectCount(new LambdaQueryWrapper<EduCourse>()
                .eq(EduCourse::getCourseName, name.trim())
                .ne(id != null, EduCourse::getId, id)) > 0;
        return new LinkedHashMap<>(Map.of("existed", existed, "name", defaultText(name, "")));
    }

    @Override
    public Map<String, Object> checkBeforeUpShelf(Long id) {
        EduCourse course = requireCourse(id);
        List<String> errors = new ArrayList<>();
        if (!StringUtils.hasText(course.getCourseName())) errors.add("课程名称不能为空");
        if (course.getCategoryId() == null) errors.add("课程分类不能为空");
        if (!StringUtils.hasText(course.getCoverUrl())) errors.add("课程封面不能为空");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pass", errors.isEmpty());
        result.put("canUpShelf", errors.isEmpty());
        result.put("errors", errors);
        result.put("course", legacyCourse(id));
        return result;
    }

    @Override
    public List<Map<String, Object>> simpleCourses() {
        return courseMapper.selectList(new LambdaQueryWrapper<EduCourse>()
                .orderByAsc(EduCourse::getSortNum).orderByDesc(EduCourse::getCreateTime))
                .stream().map(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", item.getId());
                    row.put("name", item.getCourseName());
                    row.put("title", item.getCourseName());
                    row.put("courseName", item.getCourseName());
                    row.put("status", item.getStatus());
                    return row;
                }).toList();
    }

    @Override
    @Transactional
    public List<Map<String, Object>> saveLegacyCatalog(Long courseId, int step, Object payload) {
        requireCourse(courseId);
        Object source = unwrapData(payload);
        List<Map<String, ?>> chapters = mapList(source);
        if (chapters.isEmpty()) return legacyCatalogs(courseId);
        List<EduCourseCatalog> existing = catalogMapper.selectList(new LambdaQueryWrapper<EduCourseCatalog>()
                .eq(EduCourseCatalog::getCourseId, courseId));
        Set<Long> retained = new HashSet<>();
        Map<String, Long> references = new LinkedHashMap<>();
        int lessonCount = 0;
        for (int chapterIndex = 0; chapterIndex < chapters.size(); chapterIndex++) {
            Map<String, ?> chapter = chapters.get(chapterIndex);
            EduCourseCatalog chapterEntity = upsertLegacyCatalog(courseId, chapter, 0L, 1, chapterIndex, references);
            retained.add(chapterEntity.getId());
            Object sectionPayload = chapter.get("sections");
            List<Map<String, ?>> sections = mapList(sectionPayload);
            for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
                EduCourseCatalog sectionEntity = upsertLegacyCatalog(courseId, sections.get(sectionIndex),
                        chapterEntity.getId(), catalogType(sections.get(sectionIndex), 2), sectionIndex, references);
                retained.add(sectionEntity.getId());
                if (!Integer.valueOf(3).equals(sectionEntity.getCatalogType())) lessonCount++;
            }
        }
        for (EduCourseCatalog item : existing) {
            if (!retained.contains(item.getId())) catalogMapper.deleteById(item.getId());
        }
        EduCourse course = requireCourse(courseId);
        course.setLessonCount(lessonCount);
        course.setUpdateTime(LocalDateTime.now());
        courseMapper.updateById(course);
        return legacyCatalogs(courseId);
    }

    @Override
    @Transactional
    public List<Map<String, Object>> saveLegacyMedia(Long courseId, Object payload) {
        requireCourse(courseId);
        for (Map<String, ?> row : mapList(unwrapData(payload))) {
            Long catalogId = firstLong(row.get("cataId"));
            if (catalogId == null) catalogId = firstLong(row.get("catalogId"));
            EduCourseCatalog catalog = catalogId == null ? null : catalogMapper.selectById(catalogId);
            if (catalog == null || !Objects.equals(catalog.getCourseId(), courseId)) continue;
            catalog.setMediaId(firstLong(row.get("mediaId")));
            catalog.setMediaName(defaultText(row.get("videoName"), defaultText(row.get("mediaName"), catalog.getMediaName())));
            catalog.setDurationSeconds(durationSeconds(row.get("mediaDuration"), catalog.getDurationSeconds()));
            if (row.containsKey("trailer")) catalog.setTrailer(bool(row.get("trailer")) ? 1 : 0);
            catalog.setUpdateTime(LocalDateTime.now());
            catalogMapper.updateById(catalog);
        }
        return legacyCatalogs(courseId);
    }

    @Override
    @Transactional
    public Map<String, Object> bindCatalogMedia(Long courseId, Long sectionId, Long mediaId,
                                                String mediaName, Integer durationSeconds) {
        requireCourse(courseId);
        if (sectionId == null) {
            throw new ServiceException("小节编号不能为空");
        }
        EduCourseCatalog catalog = catalogMapper.selectById(sectionId);
        if (catalog == null || !Objects.equals(catalog.getCourseId(), courseId)) {
            throw new ServiceException("未找到指定课程的小节目录");
        }
        catalog.setMediaId(mediaId);
        if (StringUtils.hasText(mediaName)) {
            catalog.setMediaName(mediaName.trim());
        }
        if (durationSeconds != null && durationSeconds >= 0) {
            catalog.setDurationSeconds(durationSeconds);
        }
        catalog.setUpdateTime(LocalDateTime.now());
        catalogMapper.updateById(catalog);
        return legacyCatalogView(catalog);
    }

    @Override
    @Transactional
    public void unbindCatalogMedia(Long courseId, Long sectionId, Long mediaId) {
        if (sectionId != null) {
            EduCourseCatalog catalog = catalogMapper.selectById(sectionId);
            if (catalog != null && (courseId == null || Objects.equals(catalog.getCourseId(), courseId))) {
                catalog.setMediaId(null);
                catalog.setMediaName(null);
                catalog.setUpdateTime(LocalDateTime.now());
                catalogMapper.updateById(catalog);
            }
        } else if (mediaId != null) {
            List<EduCourseCatalog> rows = catalogMapper.selectList(new LambdaQueryWrapper<EduCourseCatalog>()
                    .eq(EduCourseCatalog::getMediaId, mediaId));
            for (EduCourseCatalog row : rows) {
                row.setMediaId(null);
                row.setMediaName(null);
                row.setUpdateTime(LocalDateTime.now());
                catalogMapper.updateById(row);
            }
        }
    }

    @Override
    @Transactional
    public List<Map<String, Object>> saveLegacyTeachers(Long courseId, Map<String, ?> payload) {
        requireCourse(courseId);
        courseTeacherMapper.delete(new LambdaQueryWrapper<EduCourseTeacher>()
                .eq(EduCourseTeacher::getCourseId, courseId));
        Object source = payload == null ? null : payload.get("teachers");
        if (source == null && payload != null) source = payload.get("datas");
        int sort = 0;
        for (Map<String, ?> row : mapList(source)) {
            Long teacherId = firstLong(row.get("id"));
            if (teacherId == null) teacherId = firstLong(row.get("teacherId"));
            if (teacherId == null || teacherMapper.selectById(teacherId) == null) continue;
            EduCourseTeacher relation = new EduCourseTeacher();
            relation.setId(newId());
            relation.setCourseId(courseId);
            relation.setTeacherId(teacherId);
            relation.setTeacherRole(defaultText(row.get("teacherRole"), "讲师"));
            relation.setSortNum(sort++);
            relation.setCreateTime(LocalDateTime.now());
            courseTeacherMapper.insert(relation);
        }
        return teachers(courseId);
    }

    @Override
    public List<Map<String, Object>> legacySubjectGroups(Long courseId) {
        requireCourse(courseId);
        List<EduCourseCatalog> catalogs = catalogMapper.selectList(new LambdaQueryWrapper<EduCourseCatalog>()
                .eq(EduCourseCatalog::getCourseId, courseId).orderByAsc(EduCourseCatalog::getParentId)
                .orderByAsc(EduCourseCatalog::getSortNum));
        List<Map<String, Object>> result = new ArrayList<>();
        for (EduCourseCatalog catalog : catalogs) {
            if (Integer.valueOf(1).equals(catalog.getCatalogType())) continue;
            List<EduCatalogQuestion> relations = catalogQuestionMapper.selectList(new LambdaQueryWrapper<EduCatalogQuestion>()
                    .eq(EduCatalogQuestion::getCatalogId, catalog.getId()).orderByAsc(EduCatalogQuestion::getSortNum));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("cataId", catalog.getId());
            row.put("catalogId", catalog.getId());
            row.put("subjectIds", relations.stream().map(EduCatalogQuestion::getQuestionId).toList());
            row.put("subjects", relations.stream().map(item -> questionBankMapper.selectById(item.getQuestionId()))
                    .filter(Objects::nonNull).map(this::legacyQuestionView).toList());
            result.add(row);
        }
        return result;
    }

    @Override
    @Transactional
    public List<Map<String, Object>> saveLegacySubjects(Long courseId, Object payload) {
        requireCourse(courseId);
        catalogQuestionMapper.delete(new LambdaQueryWrapper<EduCatalogQuestion>()
                .eq(EduCatalogQuestion::getCourseId, courseId));
        Object source = unwrapData(payload);
        for (Map<String, ?> row : mapList(source)) {
            Long catalogId = firstLong(row.get("cataId"));
            if (catalogId == null) catalogId = firstLong(row.get("catalogId"));
            if (catalogId == null) continue;
            EduCourseCatalog catalog = catalogMapper.selectById(catalogId);
            if (catalog == null || !Objects.equals(catalog.getCourseId(), courseId)) continue;
            Object ids = row.get("subjectIds");
            if (ids == null) ids = row.get("questionIds");
            int sort = 0;
            for (Object idValue : listValues(ids)) {
                Long questionId = firstLong(idValue);
                if (questionId == null || questionBankMapper.selectById(questionId) == null) continue;
                EduCatalogQuestion relation = new EduCatalogQuestion();
                relation.setId(newId());
                relation.setCourseId(courseId);
                relation.setCatalogId(catalogId);
                relation.setQuestionId(questionId);
                relation.setSortNum(sort++);
                relation.setCreateTime(LocalDateTime.now());
                catalogQuestionMapper.insert(relation);
            }
        }
        return legacySubjectGroups(courseId);
    }

    @Override
    public Map<String, Object> portalCourses(Map<String, ?> params) {
        long pageNo = number(params, "pageNo", number(params, "pageNum", 1));
        long pageSize = number(params, "pageSize", 10);
        Long categoryId = longValue(params.get("categoryId"));
        String keyword = text(params.get("keyword"));
        String sortBy = text(params.get("sortBy"));
        String priceType = text(params.get("priceType"));

        Integer status = null;
        Object statusObj = params.get("status");
        if (statusObj != null && StringUtils.hasText(String.valueOf(statusObj))) {
            try {
                status = Integer.valueOf(String.valueOf(statusObj).trim());
            } catch (Exception ignored) {}
        }
        boolean requestedAdmin = "true".equalsIgnoreCase(String.valueOf(params.get("admin")))
                || "admin".equalsIgnoreCase(String.valueOf(params.get("role")))
                || Boolean.TRUE.equals(params.get("admin"));
        boolean isAdmin = false;
        if (requestedAdmin || status != null) {
            try {
                Long uid = SecurityUtils.getUserId();
                isAdmin = SecurityUtils.isAdmin(uid) || com.share.common.security.auth.AuthUtil.hasRole("admin")
                        || com.share.common.security.auth.AuthUtil.hasRole("teacher");
            } catch (Exception ignored) {
                isAdmin = false;
            }
        }
        if (!isAdmin) {
            status = ENABLED;
        }

        Page<EduCourse> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        LambdaQueryWrapper<EduCourse> wrapper = new LambdaQueryWrapper<EduCourse>()
                .eq(status != null, EduCourse::getStatus, status)
                .eq(status == null && !isAdmin, EduCourse::getStatus, ENABLED)
                .eq(categoryId != null && categoryId > 0, EduCourse::getCategoryId, categoryId)
                .eq("free".equalsIgnoreCase(priceType), EduCourse::getIsFree, 1)
                .gt("paid".equalsIgnoreCase(priceType), EduCourse::getPrice, BigDecimal.ZERO)
                .and(StringUtils.hasText(keyword), item -> item.like(EduCourse::getCourseName, keyword)
                        .or().like(EduCourse::getShortDescription, keyword)
                        .or().like(EduCourse::getDescription, keyword));
        if ("price".equalsIgnoreCase(sortBy) || "priceAsc".equalsIgnoreCase(sortBy)) {
            wrapper.orderByAsc(EduCourse::getPrice);
        } else if ("priceDesc".equalsIgnoreCase(sortBy)) {
            wrapper.orderByDesc(EduCourse::getPrice);
        } else if ("learners".equalsIgnoreCase(sortBy) || "hot".equalsIgnoreCase(sortBy)) {
            wrapper.orderByDesc(EduCourse::getLearnerCount);
        } else if ("newest".equalsIgnoreCase(sortBy) || "new".equalsIgnoreCase(sortBy)) {
            wrapper.orderByDesc(EduCourse::getPublishTime).orderByDesc(EduCourse::getCreateTime);
        } else {
            wrapper.orderByAsc(EduCourse::getSortNum).orderByDesc(EduCourse::getCreateTime);
        }
        courseMapper.selectPage(page, wrapper);
        return pageView(page.getTotal(), page.getRecords().stream().map(this::courseView).toList());
    }

    @Override
    public Map<String, Object> courseStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long total = defaultValue(courseMapper.selectCount(new LambdaQueryWrapper<>()), 0L);
        long published = defaultValue(courseMapper.selectCount(new LambdaQueryWrapper<EduCourse>().eq(EduCourse::getStatus, 1)), 0L);
        long pending = defaultValue(courseMapper.selectCount(new LambdaQueryWrapper<EduCourse>().eq(EduCourse::getStatus, 0)), 0L);
        long offline = defaultValue(courseMapper.selectCount(new LambdaQueryWrapper<EduCourse>().eq(EduCourse::getStatus, 2)), 0L);
        long finished = defaultValue(courseMapper.selectCount(new LambdaQueryWrapper<EduCourse>().eq(EduCourse::getStatus, 3)), 0L);
        stats.put("total", total);
        stats.put("published", published);
        stats.put("pending", pending);
        stats.put("offline", offline);
        stats.put("finished", finished);
        return stats;
    }

    @Override
    public Map<String, Object> course(Long id) {
        EduCourse course = courseMapper.selectById(id);
        if (course == null) {
            throw new ServiceException("课程不存在");
        }
        return courseView(course);
    }

    @Override
    public IPage<EduCourse> pageCourses(String keyword, Long categoryId, Integer status,
                                        long pageNo, long pageSize) {
        Page<EduCourse> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        return courseMapper.selectPage(page, new LambdaQueryWrapper<EduCourse>()
                .like(StringUtils.hasText(keyword), EduCourse::getCourseName, keyword)
                .eq(categoryId != null, EduCourse::getCategoryId, categoryId)
                .eq(status != null, EduCourse::getStatus, status)
                .orderByAsc(EduCourse::getSortNum).orderByDesc(EduCourse::getCreateTime));
    }

    @Override
    @Transactional
    public EduCourse saveCourse(EduCourse value) {
        require(value != null && StringUtils.hasText(value.getCourseName()), "课程名称不能为空");
        LocalDateTime now = LocalDateTime.now();
        if (value.getId() == null) {
            value.setId(newId());
            value.setPrice(defaultValue(value.getPrice(), BigDecimal.ZERO));
            value.setOriginalPrice(defaultValue(value.getOriginalPrice(), value.getPrice()));
            value.setLessonCount(defaultValue(value.getLessonCount(), 0));
            value.setLearnerCount(defaultValue(value.getLearnerCount(), 0));
            value.setDurationMinutes(defaultValue(value.getDurationMinutes(), 0));
            value.setRating(defaultValue(value.getRating(), BigDecimal.ZERO));
            value.setIsFree(defaultValue(value.getIsFree(), value.getPrice().signum() == 0 ? 1 : 0));
            value.setIsRecommended(defaultValue(value.getIsRecommended(), 0));
            value.setIsHot(defaultValue(value.getIsHot(), 0));
            value.setIsNew(defaultValue(value.getIsNew(), 1));
            value.setSortNum(defaultValue(value.getSortNum(), 0));
            value.setStatus(defaultValue(value.getStatus(), 0));
            value.setCreateTime(now);
            value.setUpdateTime(now);
            value.setDelFlag(0);
            value.setVersion(0);
            courseMapper.insert(value);
        } else {
            value.setUpdateTime(now);
            courseMapper.updateById(value);
        }
        return value;
    }

    @Override
    @Transactional
    public void updateCourseStatus(Long id, int status) {
        EduCourse course = requireCourse(id);
        course.setStatus(status);
        course.setPublishTime(status == ENABLED ? LocalDateTime.now() : course.getPublishTime());
        course.setUpdateTime(LocalDateTime.now());
        courseMapper.updateById(course);
    }

    @Override
    public void removeCourses(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            courseMapper.deleteBatchIds(ids);
        }
    }

    @Override
    public List<Map<String, Object>> teachers(Long courseId) {
        List<EduTeacher> teachers;
        if (courseId == null) {
            teachers = teacherMapper.selectList(new LambdaQueryWrapper<EduTeacher>()
                    .eq(EduTeacher::getStatus, ENABLED).orderByDesc(EduTeacher::getRating));
        } else {
            List<Long> ids = courseTeacherMapper.selectList(new LambdaQueryWrapper<EduCourseTeacher>()
                    .eq(EduCourseTeacher::getCourseId, courseId).orderByAsc(EduCourseTeacher::getSortNum))
                    .stream().map(EduCourseTeacher::getTeacherId).toList();
            teachers = ids.isEmpty() ? teacherMapper.selectList(new LambdaQueryWrapper<EduTeacher>()
                    .eq(EduTeacher::getStatus, ENABLED).orderByDesc(EduTeacher::getRating))
                    : teacherMapper.selectBatchIds(ids);
        }
        return teachers.stream().map(this::teacherView).toList();
    }

    @Override
    public IPage<EduTeacher> pageTeachers(String keyword, Integer status, long pageNo, long pageSize) {
        Page<EduTeacher> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        return teacherMapper.selectPage(page, new LambdaQueryWrapper<EduTeacher>()
                .like(StringUtils.hasText(keyword), EduTeacher::getTeacherName, keyword)
                .eq(status != null, EduTeacher::getStatus, status)
                .orderByDesc(EduTeacher::getRating));
    }

    @Override
    public Map<String, Object> teacherProfile(Long userId) {
        EduTeacher teacher = findTeacherByUserId(userId);
        return teacher == null ? null : teacherView(teacher);
    }

    @Override
    @Transactional
    public Map<String, Object> saveTeacherProfile(Map<String, ?> payload) {
        Map<String, ?> source = payload == null ? Map.of() : payload;
        Long userId = firstLong(source.get("userId"));
        require(userId != null, "教师系统用户编号不能为空");

        Long profileId = firstLong(source.get("teacherId"));
        if (profileId == null) profileId = firstLong(source.get("profileId"));
        EduTeacher teacher = profileId == null ? null : teacherMapper.selectById(profileId);
        if (teacher == null) teacher = findTeacherByUserId(userId);
        boolean created = teacher == null;
        if (created) {
            teacher = new EduTeacher();
            teacher.setId(newId());
            teacher.setUserId(userId);
            teacher.setTeacherName("教师");
            teacher.setCourseCount(0);
            teacher.setStudentCount(0);
            teacher.setRating(BigDecimal.ZERO);
            teacher.setStatus(ENABLED);
            teacher.setVersion(0);
            teacher.setCreateTime(LocalDateTime.now());
        } else {
            teacher.setUserId(userId);
        }

        String teacherName = firstText(source, "teacherName", "name", "nickname", "nickName");
        if (teacherName != null) teacher.setTeacherName(teacherName);
        String avatar = firstText(source, "avatarUrl", "avatar", "icon", "photo");
        if (avatar != null) teacher.setAvatarUrl(avatar);
        String title = firstText(source, "title", "job");
        if (title != null) teacher.setTitle(title);
        String introduction = firstText(source, "introduction", "intro", "description");
        if (introduction != null) teacher.setIntroduction(introduction);
        String specialty = firstText(source, "specialty", "expertise");
        if (specialty != null) teacher.setSpecialty(specialty);
        if (hasAny(source, "status", "teacherStatus", "enabled")) {
            Integer status = firstInt(source, "teacherStatus", "status", "enabled");
            if (status != null) teacher.setStatus(status == 0 ? 0 : 1);
        }
        String legacyId = firstText(source, "legacyId");
        if (legacyId != null) teacher.setLegacyId(legacyId);
        Long operatorId = firstLong(source.get("updateBy"));
        if (operatorId == null) operatorId = firstLong(source.get("operatorId"));
        if (operatorId != null) teacher.setUpdateBy(operatorId);
        teacher.setUpdateTime(LocalDateTime.now());

        if (created) teacherMapper.insert(teacher);
        else teacherMapper.updateById(teacher);
        return teacherView(teacher);
    }

    @Override
    @Transactional
    public void deleteTeacherProfile(Long userId) {
        EduTeacher teacher = findTeacherByUserId(userId);
        if (teacher != null) teacherMapper.deleteById(teacher.getId());
    }

    @Override
    public List<Map<String, Object>> catalogs(Long courseId, boolean onlyLessons) {
        LambdaQueryWrapper<EduCourseCatalog> wrapper = new LambdaQueryWrapper<EduCourseCatalog>()
                .eq(EduCourseCatalog::getCourseId, courseId)
                .eq(EduCourseCatalog::getStatus, ENABLED)
                .orderByAsc(EduCourseCatalog::getParentId).orderByAsc(EduCourseCatalog::getSortNum);
        return catalogMapper.selectList(wrapper).stream()
                .filter(item -> !onlyLessons || item.getCatalogType() == null || item.getCatalogType() == 2)
                .map(this::catalogView).toList();
    }

    @Override
    public IPage<EduCourseCatalog> pageCatalogs(Long courseId, long pageNo, long pageSize) {
        Page<EduCourseCatalog> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        return catalogMapper.selectPage(page, new LambdaQueryWrapper<EduCourseCatalog>()
                .eq(courseId != null, EduCourseCatalog::getCourseId, courseId)
                .orderByAsc(EduCourseCatalog::getCourseId).orderByAsc(EduCourseCatalog::getParentId)
                .orderByAsc(EduCourseCatalog::getSortNum));
    }

    @Override
    public List<Map<String, Object>> legacyCatalogs(Long courseId) {
        List<EduCourseCatalog> rows = catalogMapper.selectList(new LambdaQueryWrapper<EduCourseCatalog>()
                .eq(EduCourseCatalog::getCourseId, courseId).eq(EduCourseCatalog::getStatus, ENABLED)
                .orderByAsc(EduCourseCatalog::getParentId).orderByAsc(EduCourseCatalog::getSortNum)
                .orderByAsc(EduCourseCatalog::getId));
        Map<Long, Map<String, Object>> views = new LinkedHashMap<>();
        for (EduCourseCatalog row : rows) {
            Map<String, Object> view = legacyCatalogView(row);
            view.put("sections", new ArrayList<Map<String, Object>>());
            views.put(row.getId(), view);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (EduCourseCatalog row : rows) {
            Map<String, Object> view = views.get(row.getId());
            Map<String, Object> parent = views.get(row.getParentId());
            if (parent == null || row.getParentId() == null || row.getParentId() == 0L) {
                roots.add(view);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> sections = (List<Map<String, Object>>) parent.get("sections");
                sections.add(view);
            }
        }
        return roots;
    }

    @Override
    public List<Map<String, Object>> courseLikeRanking(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        String rankingKey = "edu:course:likes:zset";
        Long size = redisService.zCard(rankingKey);
        if (size == null || size == 0) {
            List<EduCourse> courses = courseMapper.selectList(new LambdaQueryWrapper<EduCourse>()
                    .eq(EduCourse::getStatus, ENABLED)
                    .orderByDesc(EduCourse::getLearnerCount)
                    .last("limit 20"));
            for (EduCourse c : courses) {
                double initialScore = c.getLearnerCount() != null ? c.getLearnerCount().doubleValue() : 10.0;
                redisService.zAdd(rankingKey, String.valueOf(c.getId()), initialScore);
            }
        }

        Set<TypedTuple<Object>> rankingTuples = redisService.zReverseRangeWithScores(rankingKey, 0, safeLimit - 1);
        List<Map<String, Object>> result = new ArrayList<>();
        if (rankingTuples == null || rankingTuples.isEmpty()) {
            return result;
        }

        int rank = 1;
        for (TypedTuple<Object> tuple : rankingTuples) {
            if (tuple.getValue() == null) continue;
            Long courseId = longValue(tuple.getValue());
            if (courseId == null) continue;
            EduCourse course = courseMapper.selectById(courseId);
            if (course == null || !Integer.valueOf(ENABLED).equals(course.getStatus())) continue;

            Map<String, Object> view = courseView(course);
            view.put("rank", rank++);
            long likes = tuple.getScore() != null ? Math.max(0, tuple.getScore().longValue()) : 0L;
            view.put("likes", likes);
            view.put("likeCount", likes);
            result.add(view);
        }
        return result;
    }

    @Override
    public Map<String, Object> courseView(EduCourse item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("title", item.getCourseName());
        result.put("courseName", item.getCourseName());
        result.put("cover", item.getCoverUrl());
        result.put("coverUrl", item.getCoverUrl());
        result.put("categoryId", item.getCategoryId());
        EduCategory category = categoryMapper.selectById(item.getCategoryId());
        result.put("categoryName", category == null ? "未分类" : category.getCategoryName());
        result.put("price", moneyCents(item.getPrice()));
        result.put("originalPrice", moneyCents(item.getOriginalPrice()));
        result.put("lessons", item.getLessonCount());
        result.put("lessonCount", item.getLessonCount());
        result.put("learners", item.getLearnerCount());
        result.put("learnerCount", item.getLearnerCount());
        result.put("durationMinutes", item.getDurationMinutes());
        result.put("rating", item.getRating());
        result.put("difficultyLevel", item.getDifficultyLevel() == null ? 2 : item.getDifficultyLevel());
        result.put("difficulty", item.getDifficultyLevel() == null ? 2 : item.getDifficultyLevel());
        result.put("skills", item.getSkills());
        List<String> skillList = item.getSkills() != null
                ? Arrays.stream(item.getSkills().split("[,，、]+")).map(String::trim).filter(StringUtils::hasText).toList()
                : List.of();
        result.put("skillsList", skillList);
        result.put("targetRole", item.getTargetRole());
        result.put("prerequisites", item.getPrerequisites());
        result.put("isFree", item.getIsFree());
        result.put("status", item.getStatus());
        String rawDesc = item.getDescription() == null ? item.getShortDescription() : item.getDescription();
        result.put("description", cleanHtmlTags(rawDesc));
        result.put("shortDescription", cleanHtmlTags(item.getShortDescription()));
        result.put("createTime", item.getCreateTime());
        Double zScore = redisService.zScore("edu:course:likes:zset", String.valueOf(item.getId()));
        long likes = zScore != null ? Math.max(0, zScore.longValue()) : 0L;
        result.put("likeCount", likes);
        result.put("likes", likes);
        boolean isLiked = false;
        try {
            Long currentUid = SecurityUtils.getUserId();
            if (currentUid != null && currentUid > 0) {
                isLiked = Boolean.TRUE.equals(redisService.sIsMember("edu:course:likes:users:" + item.getId(), String.valueOf(currentUid)));
            }
        } catch (Exception ignored) {}
        result.put("isLiked", isLiked);
        List<Map<String, Object>> teacherList = teachers(item.getId());
        if (!teacherList.isEmpty()) {
            result.put("teacherId", teacherList.get(0).get("id"));
            result.put("teacherName", teacherList.get(0).get("name"));
        }
        return result;
    }

    @Override
    public Map<String, Object> catalogView(EduCourseCatalog item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("courseId", item.getCourseId());
        result.put("parentId", item.getParentId());
        result.put("catalogTitle", item.getCatalogTitle());
        result.put("title", item.getCatalogTitle());
        result.put("index", item.getCatalogTitle());
        result.put("catalogType", item.getCatalogType());
        result.put("mediaId", item.getMediaId());
        result.put("mediaName", item.getMediaName());
        result.put("durationSeconds", item.getDurationSeconds());
        result.put("isFree", item.getIsFree());
        result.put("trailer", item.getTrailer());
        result.put("sortNum", item.getSortNum());
        return result;
    }

    @Override
    public Map<String, Object> legacyCatalogView(EduCourseCatalog item) {
        Map<String, Object> result = catalogView(item);
        result.put("name", item.getCatalogTitle());
        result.put("title", item.getCatalogTitle());
        result.put("index", item.getSortNum());
        result.put("type", item.getCatalogType());
        result.put("mediaDuration", item.getDurationSeconds());
        result.put("videoName", item.getMediaName());
        result.put("trailer", Integer.valueOf(1).equals(item.getTrailer()));
        result.put("canUpdate", Boolean.TRUE);
        result.put("maxIndexOnShelf", item.getSortNum() == null ? 0 : item.getSortNum());
        result.put("isShow", Boolean.TRUE);
        return result;
    }

    @Override
    public Map<String, Object> teacherView(EduTeacher item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("userId", item.getUserId());
        result.put("name", item.getTeacherName());
        result.put("teacherName", item.getTeacherName());
        result.put("avatar", item.getAvatarUrl());
        result.put("avatarUrl", item.getAvatarUrl());
        result.put("title", item.getTitle());
        result.put("introduction", item.getIntroduction());
        result.put("job", item.getTitle());
        result.put("intro", item.getIntroduction());
        result.put("specialty", item.getSpecialty());
        result.put("courses", item.getCourseCount());
        result.put("courseCount", item.getCourseCount());
        result.put("students", item.getStudentCount());
        result.put("studentCount", item.getStudentCount());
        result.put("rating", item.getRating());
        return result;
    }

    @Override
    public int courseLessonCount(Long courseId) {
        if (courseId == null) return 0;
        int count = Optional.ofNullable(courseMapper.selectById(courseId)).map(EduCourse::getLessonCount).orElse(0);
        if (count <= 0) {
            Long catalogCount = catalogMapper.selectCount(new LambdaQueryWrapper<EduCourseCatalog>()
                    .eq(EduCourseCatalog::getCourseId, courseId)
                    .eq(EduCourseCatalog::getStatus, ENABLED)
                    .ne(EduCourseCatalog::getParentId, 0L));
            if (catalogCount != null && catalogCount > 0) {
                count = catalogCount.intValue();
            }
        }
        return count;
    }

    @Override
    public String courseName(Long courseId) {
        return Optional.ofNullable(courseId).map(courseMapper::selectById).map(EduCourse::getCourseName).orElse("");
    }

    @Override
    public EduCourse requireCourse(Long id) {
        EduCourse value = courseMapper.selectById(id);
        if (value == null) throw new ServiceException("课程不存在");
        return value;
    }

    private EduCourseCatalog upsertLegacyCatalog(Long courseId, Map<String, ?> source, Long parentId,
                                                int defaultType, int sortNum, Map<String, Long> references) {
        String sourceKey = defaultText(source.get("id"), defaultText(source.get("subId"), null));
        Long sourceId = firstLong(source.get("id"));
        EduCourseCatalog value = sourceId == null ? null : catalogMapper.selectById(sourceId);
        if (value != null && !Objects.equals(value.getCourseId(), courseId)) value = null;
        if (value == null && StringUtils.hasText(sourceKey)) {
            String legacyKey = courseId + ":" + sourceKey;
            value = catalogMapper.selectOne(new LambdaQueryWrapper<EduCourseCatalog>()
                    .eq(EduCourseCatalog::getLegacyId, legacyKey));
        }
        if (value == null) {
            value = new EduCourseCatalog();
            value.setId(newId());
            value.setCourseId(courseId);
            value.setCreateTime(LocalDateTime.now());
            value.setDelFlag(0);
            value.setVersion(0);
        }
        value.setParentId(parentId == null ? 0L : parentId);
        value.setCatalogTitle(defaultText(source.get("name"),
                defaultText(source.get("title"), defaultText(source.get("catalogTitle"), "未命名目录"))));
        value.setCatalogType(catalogType(source, defaultType));
        value.setMediaId(firstLong(source.get("mediaId")));
        value.setMediaName(defaultText(source.get("videoName"),
                defaultText(source.get("mediaName"), value.getMediaName())));
        value.setDurationSeconds(durationSeconds(source.get("mediaDuration"),
                durationSeconds(source.get("durationSeconds"), durationSeconds(source.get("duration"), 0))));
        value.setIsFree(bool(source.get("isFree")) || bool(source.get("free")) ? 1 : 0);
        value.setTrailer(bool(source.get("trailer")) ? 1 : 0);
        value.setSortNum(intValue(source.get("index"), intValue(source.get("sortNum"), sortNum + 1)));
        value.setStatus(defaultValue(intValue(source.get("status"), 1), ENABLED));
        value.setLegacyId(StringUtils.hasText(sourceKey) ? courseId + ":" + sourceKey : null);
        value.setUpdateTime(LocalDateTime.now());
        if (value.getCreateTime() == null) value.setCreateTime(value.getUpdateTime());
        if (value.getId() == null) { value.setId(newId()); catalogMapper.insert(value); }
        else if (catalogMapper.selectById(value.getId()) == null) catalogMapper.insert(value);
        else catalogMapper.updateById(value);
        if (StringUtils.hasText(sourceKey)) {
            references.put(sourceKey, value.getId());
            references.put(String.valueOf(value.getId()), value.getId());
        }
        return value;
    }

    private EduTeacher findTeacherByUserId(Long userId) {
        if (userId == null) return null;
        return teacherMapper.selectOne(new LambdaQueryWrapper<EduTeacher>()
                .eq(EduTeacher::getUserId, userId).last("limit 1"));
    }

    private boolean hasAny(Map<String, ?> source, String... keys) {
        for (String key : keys) if (source.containsKey(key)) return true;
        return false;
    }

    private String firstText(Map<String, ?> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) return String.valueOf(value).trim();
        }
        return null;
    }

    private Integer firstInt(Map<String, ?> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value == null || !StringUtils.hasText(String.valueOf(value))) continue;
            try { return Integer.valueOf(String.valueOf(value)); }
            catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private Map<String, Object> questionBankView(EduExamQuestionBank item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("questionType", item.getQuestionType());
        result.put("type", item.getQuestionType());
        result.put("stem", item.getStem());
        result.put("title", item.getStem());
        result.put("options", item.getOptionsJson());
        result.put("optionsJson", item.getOptionsJson());
        result.put("correctAnswer", item.getCorrectAnswer());
        result.put("analysis", item.getAnalysis());
        result.put("score", item.getScore());
        result.put("difficulty", item.getDifficulty());
        result.put("categoryId", item.getCategoryId());
        return result;
    }

    private Map<String, Object> legacyQuestionView(EduExamQuestionBank item) {
        Map<String, Object> result = questionBankView(item);
        result.put("questionType", item.getQuestionType());
        result.put("type", legacyQuestionType(item.getQuestionType()));
        result.put("title", item.getStem());
        result.put("name", item.getStem());
        result.put("answer", item.getCorrectAnswer());
        result.put("categoryName", "题库题目");
        result.put("createTime", item.getCreateTime());
        List<?> options = List.of();
        if (StringUtils.hasText(item.getOptionsJson())) {
            try {
                options = objectMapper.readValue(item.getOptionsJson(), List.class);
            } catch (Exception ignored) {
                options = List.of(item.getOptionsJson());
            }
        }
        result.put("options", options);
        return result;
    }
}
