package com.share.education.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.common.core.exception.ServiceException;
import com.share.common.security.utils.SecurityUtils;
import com.share.education.domain.EduBanner;
import com.share.education.domain.EduCategory;
import com.share.education.domain.EduCatalogQuestion;
import com.share.education.domain.EduCourse;
import com.share.education.domain.EduCourseCatalog;
import com.share.education.domain.EduCourseRecommend;
import com.share.education.domain.EduCourseTeacher;
import com.share.education.domain.EduDashboardDaily;
import com.share.education.domain.EduExam;
import com.share.education.domain.EduExamAnswer;
import com.share.education.domain.EduExamQuestion;
import com.share.education.domain.EduExamQuestionBank;
import com.share.education.domain.EduExamRecord;
import com.share.education.domain.EduInterest;
import com.share.education.domain.EduLearningPlan;
import com.share.education.domain.EduLearningRecord;
import com.share.education.domain.EduNote;
import com.share.education.domain.EduNoteCollect;
import com.share.education.domain.EduNoteLike;
import com.share.education.domain.EduPointsLedger;
import com.share.education.domain.EduQuestion;
import com.share.education.domain.EduQuestionLike;
import com.share.education.domain.EduReply;
import com.share.education.domain.EduSignRecord;
import com.share.education.domain.EduTeacher;
import com.share.education.mapper.EduBannerMapper;
import com.share.education.mapper.EduCategoryMapper;
import com.share.education.mapper.EduCatalogQuestionMapper;
import com.share.education.mapper.EduCourseCatalogMapper;
import com.share.education.mapper.EduCourseMapper;
import com.share.education.mapper.EduCourseRecommendMapper;
import com.share.education.mapper.EduCourseTeacherMapper;
import com.share.education.mapper.EduDashboardDailyMapper;
import com.share.education.mapper.EduExamAnswerMapper;
import com.share.education.mapper.EduExamMapper;
import com.share.education.mapper.EduExamQuestionBankMapper;
import com.share.education.mapper.EduExamQuestionMapper;
import com.share.education.mapper.EduExamRecordMapper;
import com.share.education.mapper.EduInterestMapper;
import com.share.education.mapper.EduLearningPlanMapper;
import com.share.education.mapper.EduLearningRecordMapper;
import com.share.education.mapper.EduNoteCollectMapper;
import com.share.education.mapper.EduNoteLikeMapper;
import com.share.education.mapper.EduNoteMapper;
import com.share.education.mapper.EduPointsLedgerMapper;
import com.share.education.mapper.EduQuestionLikeMapper;
import com.share.education.mapper.EduQuestionMapper;
import com.share.education.mapper.EduReplyMapper;
import com.share.education.mapper.EduSignRecordMapper;
import com.share.education.mapper.EduTeacherMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 教育领域的业务服务。
 *
 * <p>该服务是迁移层的适配中心：数据库实体使用新的规范字段，返回给原用户端的
 * portal DTO 保留 title、cover、lessons、learners 等旧字段，因此迁移过程中页面
 * 不需要重写业务逻辑。</p>
 */
@Service
public class EducationService {
    private static final int ENABLED = 1;
    private static final int DELETED = 1;
    private static final int SIGN_POINTS = 10;

    private final EduBannerMapper bannerMapper;
    private final EduCategoryMapper categoryMapper;
    private final EduDashboardDailyMapper dashboardDailyMapper;
    private final EduCatalogQuestionMapper catalogQuestionMapper;
    private final EduCourseMapper courseMapper;
    private final EduCourseCatalogMapper catalogMapper;
    private final EduCourseRecommendMapper recommendMapper;
    private final EduCourseTeacherMapper courseTeacherMapper;
    private final EduTeacherMapper teacherMapper;
    private final EduInterestMapper interestMapper;
    private final EduLearningPlanMapper planMapper;
    private final EduLearningRecordMapper learningMapper;
    private final EduQuestionMapper questionMapper;
    private final EduReplyMapper replyMapper;
    private final EduQuestionLikeMapper questionLikeMapper;
    private final EduNoteMapper noteMapper;
    private final EduNoteCollectMapper noteCollectMapper;
    private final EduNoteLikeMapper noteLikeMapper;
    private final EduExamMapper examMapper;
    private final EduExamQuestionBankMapper questionBankMapper;
    private final EduExamQuestionMapper examQuestionMapper;
    private final EduExamRecordMapper examRecordMapper;
    private final EduExamAnswerMapper examAnswerMapper;
    private final EduSignRecordMapper signMapper;
    private final EduPointsLedgerMapper pointsMapper;
    private final ObjectMapper objectMapper;

    public EducationService(EduBannerMapper bannerMapper, EduCategoryMapper categoryMapper,
            EduDashboardDailyMapper dashboardDailyMapper,
            EduCatalogQuestionMapper catalogQuestionMapper,
            EduCourseMapper courseMapper, EduCourseCatalogMapper catalogMapper,
            EduCourseRecommendMapper recommendMapper, EduCourseTeacherMapper courseTeacherMapper,
            EduTeacherMapper teacherMapper, EduInterestMapper interestMapper,
            EduLearningPlanMapper planMapper, EduLearningRecordMapper learningMapper,
            EduQuestionMapper questionMapper, EduReplyMapper replyMapper,
            EduQuestionLikeMapper questionLikeMapper, EduNoteMapper noteMapper,
            EduNoteCollectMapper noteCollectMapper, EduNoteLikeMapper noteLikeMapper,
            EduExamMapper examMapper, EduExamQuestionBankMapper questionBankMapper,
            EduExamQuestionMapper examQuestionMapper, EduExamRecordMapper examRecordMapper,
            EduExamAnswerMapper examAnswerMapper, EduSignRecordMapper signMapper,
            EduPointsLedgerMapper pointsMapper, ObjectMapper objectMapper) {
        this.bannerMapper = bannerMapper;
        this.categoryMapper = categoryMapper;
        this.dashboardDailyMapper = dashboardDailyMapper;
        this.catalogQuestionMapper = catalogQuestionMapper;
        this.courseMapper = courseMapper;
        this.catalogMapper = catalogMapper;
        this.recommendMapper = recommendMapper;
        this.courseTeacherMapper = courseTeacherMapper;
        this.teacherMapper = teacherMapper;
        this.interestMapper = interestMapper;
        this.planMapper = planMapper;
        this.learningMapper = learningMapper;
        this.questionMapper = questionMapper;
        this.replyMapper = replyMapper;
        this.questionLikeMapper = questionLikeMapper;
        this.noteMapper = noteMapper;
        this.noteCollectMapper = noteCollectMapper;
        this.noteLikeMapper = noteLikeMapper;
        this.examMapper = examMapper;
        this.questionBankMapper = questionBankMapper;
        this.examQuestionMapper = examQuestionMapper;
        this.examRecordMapper = examRecordMapper;
        this.examAnswerMapper = examAnswerMapper;
        this.signMapper = signMapper;
        this.pointsMapper = pointsMapper;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> listCategories(boolean includeDisabled) {
        LambdaQueryWrapper<EduCategory> wrapper = new LambdaQueryWrapper<EduCategory>()
                .eq(!includeDisabled, EduCategory::getStatus, ENABLED)
                .orderByAsc(EduCategory::getSortNum).orderByAsc(EduCategory::getId);
        return categoryMapper.selectList(wrapper).stream().map(this::categoryView).toList();
    }

    public Map<String, Object> category(Long id) {
        EduCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new ServiceException("课程分类不存在");
        }
        return categoryView(category);
    }

    public IPage<EduCategory> pageCategories(String keyword, Integer status, long pageNo, long pageSize) {
        Page<EduCategory> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        return categoryMapper.selectPage(page, new LambdaQueryWrapper<EduCategory>()
                .like(StringUtils.hasText(keyword), EduCategory::getCategoryName, keyword)
                .eq(status != null, EduCategory::getStatus, status)
                .orderByAsc(EduCategory::getSortNum).orderByAsc(EduCategory::getId));
    }

    @Transactional
    public EduCategory saveCategory(EduCategory value) {
        require(value != null && StringUtils.hasText(value.getCategoryName()), "分类名称不能为空");
        LocalDateTime now = LocalDateTime.now();
        if (value.getId() == null) {
            value.setId(newId());
            value.setParentId(defaultValue(value.getParentId(), 0L));
            value.setSortNum(defaultValue(value.getSortNum(), 0));
            value.setStatus(defaultValue(value.getStatus(), ENABLED));
            value.setCreateTime(now);
            value.setUpdateTime(now);
            value.setDelFlag(0);
            value.setVersion(0);
            categoryMapper.insert(value);
        } else {
            value.setUpdateTime(now);
            categoryMapper.updateById(value);
        }
        return value;
    }

    public void removeCategories(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            categoryMapper.deleteBatchIds(ids);
        }
    }

    /**
     * 旧天机管理端分类接口使用 categorys 拼写，并且返回树形数组而不是若依分页对象。
     * 这里集中完成字段和层级适配，避免把兼容逻辑散落到 Controller 中。
     */
    public List<Map<String, Object>> legacyCategories(boolean includeDisabled) {
        LambdaQueryWrapper<EduCategory> wrapper = new LambdaQueryWrapper<EduCategory>()
                .eq(!includeDisabled, EduCategory::getStatus, ENABLED)
                .orderByAsc(EduCategory::getSortNum).orderByAsc(EduCategory::getId);
        List<EduCategory> categories = categoryMapper.selectList(wrapper);
        Map<Long, Map<String, Object>> views = new LinkedHashMap<>();
        for (EduCategory category : categories) {
            Map<String, Object> view = categoryView(category);
            int courseNum = courseMapper.selectCount(new LambdaQueryWrapper<EduCourse>()
                    .eq(EduCourse::getCategoryId, category.getId())).intValue();
            view.put("courseNum", courseNum);
            view.put("courses", courseNum);
            view.put("children", new ArrayList<Map<String, Object>>());
            view.put("level", Integer.valueOf(1));
            views.put(category.getId(), view);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (EduCategory category : categories) {
            Map<String, Object> view = views.get(category.getId());
            Long parentId = category.getParentId();
            Map<String, Object> parent = parentId == null ? null : views.get(parentId);
            if (parent == null || parentId == 0L) {
                roots.add(view);
            } else {
                view.put("level", Integer.valueOf(2));
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                children.add(view);
            }
        }
        return roots;
    }

    /** 保存旧 categorys/add、categorys/update 请求中的 name/index 字段。 */
    @Transactional
    public Map<String, Object> saveLegacyCategory(Map<String, ?> body) {
        Long id = longValue(body == null ? null : body.get("id"));
        EduCategory value = id == null ? new EduCategory() : categoryMapper.selectById(id);
        if (value == null) {
            throw new ServiceException("课程分类不存在");
        }
        String name = defaultText(body == null ? null : body.get("name"),
                defaultText(body == null ? null : body.get("categoryName"), null));
        require(StringUtils.hasText(name), "分类名称不能为空");
        value.setCategoryName(name.trim());
        value.setParentId(defaultValue(longValue(body == null ? null : body.get("parentId")), 0L));
        value.setSortNum(intValue(body == null ? null : body.get("index"),
                intValue(body == null ? null : body.get("sort"), 0)));
        value.setDescription(defaultText(body == null ? null : body.get("description"), value.getDescription()));
        value.setIcon(defaultText(body == null ? null : body.get("icon"), value.getIcon()));
        value.setStatus(defaultValue(intValue(body == null ? null : body.get("status"), value.getStatus()), ENABLED));
        LocalDateTime now = LocalDateTime.now();
        if (value.getId() == null) {
            value.setId(newId());
            value.setParentId(defaultValue(value.getParentId(), 0L));
            value.setSortNum(defaultValue(value.getSortNum(), 0));
            value.setStatus(defaultValue(value.getStatus(), ENABLED));
            value.setCreateTime(now);
            value.setDelFlag(0);
            value.setVersion(0);
            categoryMapper.insert(value);
            value.setUpdateTime(now);
            return categoryView(value);
        }
        value.setUpdateTime(now);
        categoryMapper.updateById(value);
        return categoryView(value);
    }

    @Transactional
    public void updateLegacyCategoryStatus(Map<String, ?> body) {
        Long id = longValue(body == null ? null : body.get("id"));
        EduCategory value = id == null ? null : categoryMapper.selectById(id);
        require(value != null, "课程分类不存在");
        value.setStatus(defaultValue(intValue(body == null ? null : body.get("status"), value.getStatus()), value.getStatus()));
        value.setUpdateTime(LocalDateTime.now());
        categoryMapper.updateById(value);
    }

    @Transactional
    public void removeLegacyCategory(Long id) {
        require(id != null, "分类编号不能为空");
        long children = categoryMapper.selectCount(new LambdaQueryWrapper<EduCategory>()
                .eq(EduCategory::getParentId, id));
        long courses = courseMapper.selectCount(new LambdaQueryWrapper<EduCourse>()
                .eq(EduCourse::getCategoryId, id));
        require(children == 0 && courses == 0, "该分类下仍有子分类或课程，无法删除");
        categoryMapper.deleteById(id);
    }

    /**
     * 旧课程编辑页使用 name、detail、thirdCateId、free 等字段，数据库实体使用规范化字段。
     * 兼容转换放在服务层，保证 Controller 只负责接收请求。
     */
    public Map<String, Object> legacyCourse(Long id) {
        EduCourse course = requireCourse(id);
        Map<String, Object> result = courseView(course);
        String categoryName = String.valueOf(result.getOrDefault("categoryName", ""));
        result.put("name", course.getCourseName());
        result.put("detail", course.getDescription());
        result.put("introduce", course.getShortDescription());
        result.put("usePeople", null);
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

    /** 保存旧课程基本信息接口 /courses/baseInfo/save。金额按旧接口约定接收分。 */
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
        value.setShortDescription(defaultText(body == null ? null : body.get("introduce"), value.getShortDescription()));
        value.setDescription(defaultText(body == null ? null : body.get("detail"),
                defaultText(body == null ? null : body.get("description"), value.getDescription())));
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

    public Map<String, Object> checkCourseName(Map<String, ?> params) {
        String name = defaultText(params == null ? null : params.get("name"),
                defaultText(params == null ? null : params.get("courseName"), null));
        Long id = longValue(params == null ? null : params.get("id"));
        boolean existed = StringUtils.hasText(name) && courseMapper.selectCount(new LambdaQueryWrapper<EduCourse>()
                .eq(EduCourse::getCourseName, name.trim())
                .ne(id != null, EduCourse::getId, id)) > 0;
        return new LinkedHashMap<>(Map.of("existed", existed, "name", defaultText(name, "")));
    }

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

    /** 将旧课程目录树转换并持久化，支持章节、课时和阶段练习。 */
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
            EduCourseCatalog chapterEntity = upsertLegacyCatalog(courseId, chapter, 0L, 1, chapterIndex,
                    references);
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
            relation.setId(newId()); relation.setCourseId(courseId); relation.setTeacherId(teacherId);
            relation.setTeacherRole(defaultText(row.get("teacherRole"), "讲师"));
            relation.setSortNum(sort++); relation.setCreateTime(LocalDateTime.now());
            courseTeacherMapper.insert(relation);
        }
        return teachers(courseId);
    }

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
                relation.setId(newId()); relation.setCourseId(courseId); relation.setCatalogId(catalogId);
                relation.setQuestionId(questionId); relation.setSortNum(sort++); relation.setCreateTime(LocalDateTime.now());
                catalogQuestionMapper.insert(relation);
            }
        }
        return legacySubjectGroups(courseId);
    }

    /** 首页课程分页，data 结构兼容旧用户端：{total,list}。 */
    public Map<String, Object> portalCourses(Map<String, ?> params) {
        long pageNo = number(params, "pageNo", number(params, "pageNum", 1));
        long pageSize = number(params, "pageSize", 10);
        Long categoryId = longValue(params.get("categoryId"));
        String keyword = text(params.get("keyword"));
        String sortBy = text(params.get("sortBy"));
        String priceType = text(params.get("priceType"));
        Page<EduCourse> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        LambdaQueryWrapper<EduCourse> wrapper = new LambdaQueryWrapper<EduCourse>()
                .eq(EduCourse::getStatus, ENABLED)
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

    public Map<String, Object> course(Long id) {
        EduCourse course = courseMapper.selectById(id);
        if (course == null) {
            throw new ServiceException("课程不存在");
        }
        return courseView(course);
    }

    public IPage<EduCourse> pageCourses(String keyword, Long categoryId, Integer status,
            long pageNo, long pageSize) {
        Page<EduCourse> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        return courseMapper.selectPage(page, new LambdaQueryWrapper<EduCourse>()
                .like(StringUtils.hasText(keyword), EduCourse::getCourseName, keyword)
                .eq(categoryId != null, EduCourse::getCategoryId, categoryId)
                .eq(status != null, EduCourse::getStatus, status)
                .orderByAsc(EduCourse::getSortNum).orderByDesc(EduCourse::getCreateTime));
    }

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

    @Transactional
    public void updateCourseStatus(Long id, int status) {
        EduCourse course = requireCourse(id);
        course.setStatus(status);
        course.setPublishTime(status == ENABLED ? LocalDateTime.now() : course.getPublishTime());
        course.setUpdateTime(LocalDateTime.now());
        courseMapper.updateById(course);
    }

    public void removeCourses(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            courseMapper.deleteBatchIds(ids);
        }
    }

    /**
     * 旧课程编辑器会先调用 generator 取得阶段测试的临时编号，随后在保存目录时
     * 通过该编号建立真正的课程目录记录。此处只生成分布式唯一编号，不提前写入孤儿数据。
     */
    public long generateStageExamId() {
        return newId();
    }

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
        return courses.stream().limit(20).map(this::courseView).toList();
    }

    public List<Map<String, Object>> banners() {
        return bannerMapper.selectList(new LambdaQueryWrapper<EduBanner>()
                .eq(EduBanner::getStatus, ENABLED).orderByAsc(EduBanner::getSortNum))
                .stream().map(item -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("id", item.getId());
                    result.put("title", item.getTitle());
                    result.put("image", item.getImageUrl());
                    result.put("link", "course".equalsIgnoreCase(item.getTargetType())
                            ? "/details/index?id=" + item.getTargetValue() : item.getTargetValue());
                    result.put("imageUrl", item.getImageUrl());
                    result.put("targetType", item.getTargetType());
                    result.put("targetValue", item.getTargetValue());
                    return result;
                }).toList();
    }

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

    public IPage<EduTeacher> pageTeachers(String keyword, Integer status, long pageNo, long pageSize) {
        Page<EduTeacher> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        return teacherMapper.selectPage(page, new LambdaQueryWrapper<EduTeacher>()
                .like(StringUtils.hasText(keyword), EduTeacher::getTeacherName, keyword)
                .eq(status != null, EduTeacher::getStatus, status)
                .orderByDesc(EduTeacher::getRating));
    }

    /**
     * 按系统用户编号读取教师扩展资料，供系统服务内部聚合用户视图使用。
     * 教师账号和教师资料分别属于 system、education 两个服务，不能让系统服务
     * 直接跨库查询 edu_teacher。
     */
    public Map<String, Object> teacherProfile(Long userId) {
        EduTeacher teacher = findTeacherByUserId(userId);
        return teacher == null ? null : teacherView(teacher);
    }

    /**
     * 新增或更新教师扩展资料。该方法只接受内部调用，字段名同时兼容新 DTO
     * 和原天机前端的 job/intro/photo 命名。
     */
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

    /** 删除与系统用户绑定的教师扩展资料。 */
    @Transactional
    public void deleteTeacherProfile(Long userId) {
        EduTeacher teacher = findTeacherByUserId(userId);
        if (teacher != null) teacherMapper.deleteById(teacher.getId());
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
            catch (NumberFormatException ignored) { }
        }
        return null;
    }

    public List<Map<String, Object>> catalogs(Long courseId, boolean onlyLessons) {
        LambdaQueryWrapper<EduCourseCatalog> wrapper = new LambdaQueryWrapper<EduCourseCatalog>()
                .eq(EduCourseCatalog::getCourseId, courseId)
                .eq(EduCourseCatalog::getStatus, ENABLED)
                .orderByAsc(EduCourseCatalog::getParentId).orderByAsc(EduCourseCatalog::getSortNum);
        return catalogMapper.selectList(wrapper).stream()
                .filter(item -> !onlyLessons || item.getCatalogType() == null || item.getCatalogType() == 2)
                .map(this::catalogView).toList();
    }

    public IPage<EduCourseCatalog> pageCatalogs(Long courseId, long pageNo, long pageSize) {
        Page<EduCourseCatalog> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        return catalogMapper.selectPage(page, new LambdaQueryWrapper<EduCourseCatalog>()
                .eq(courseId != null, EduCourseCatalog::getCourseId, courseId)
                .orderByAsc(EduCourseCatalog::getCourseId).orderByAsc(EduCourseCatalog::getParentId)
                .orderByAsc(EduCourseCatalog::getSortNum));
    }

    /** 旧课程编辑页需要章节-小节树，数据库中仍按单表父子关系保存。 */
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

    public List<Map<String, Object>> interests() {
        Long userId = currentUserId();
        return interestMapper.selectList(new LambdaQueryWrapper<EduInterest>().eq(EduInterest::getUserId, userId))
                .stream().map(item -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("id", item.getId());
                    result.put("userId", item.getUserId());
                    result.put("categoryId", item.getCategoryId());
                    result.put("category", categoryView(categoryMapper.selectById(item.getCategoryId())));
                    return result;
                }).toList();
    }

    @Transactional
    public EduInterest saveInterest(Long categoryId) {
        require(categoryId != null, "兴趣分类不能为空");
        Long userId = currentUserId();
        EduInterest value = interestMapper.selectOne(new LambdaQueryWrapper<EduInterest>()
                .eq(EduInterest::getUserId, userId).eq(EduInterest::getCategoryId, categoryId));
        if (value == null) {
            value = new EduInterest();
            value.setId(newId());
            value.setUserId(userId);
            value.setCategoryId(categoryId);
            value.setCreateTime(LocalDateTime.now());
            interestMapper.insert(value);
        }
        return value;
    }

    public List<Map<String, Object>> interestCourses(Long categoryId) {
        return courseMapper.selectList(new LambdaQueryWrapper<EduCourse>()
                .eq(EduCourse::getCategoryId, categoryId).eq(EduCourse::getStatus, ENABLED)
                .orderByDesc(EduCourse::getPublishTime).last("limit 20"))
                .stream().map(this::courseView).toList();
    }

    public Map<String, Object> learningCourse(Long courseId) {
        EduLearningRecord record = learningMapper.selectOne(new LambdaQueryWrapper<EduLearningRecord>()
                .eq(EduLearningRecord::getUserId, currentUserId()).eq(EduLearningRecord::getCourseId, courseId)
                .orderByDesc(EduLearningRecord::getUpdateTime).last("limit 1"));
        if (record == null) {
            return null;
        }
        return learningView(record);
    }

    /**
     * 查询当前用户某个小节（目录）的学习记录。
     *
     * <p>旧前端把目录编号命名为 lessonId。课程报名记录的 catalog_id 为
     * {@code null}，不能把它误当成小节记录返回，否则播放页会显示错误进度。</p>
     */
    public Map<String, Object> learningRecord(Long lessonId) {
        require(lessonId != null, "小节编号不能为空");
        EduLearningRecord record = learningMapper.selectOne(new LambdaQueryWrapper<EduLearningRecord>()
                .eq(EduLearningRecord::getUserId, currentUserId())
                .eq(EduLearningRecord::getCatalogId, lessonId)
                .orderByDesc(EduLearningRecord::getUpdateTime)
                .last("limit 1"));
        return record == null ? null : learningView(record);
    }

    /**
     * 购买或报名成功后创建学习记录。交易服务通过 Feign 调用该能力，
     * 避免交易服务直接写教育库；重复购买保持幂等，不会重复创建课表记录。
     */
    @Transactional
    public Map<String, Object> enrollCourse(Long courseId) {
        EduCourse course = requireCourse(courseId);
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

    /**
     * 重新开始指定课程。
     *
     * <p>一个课程可能同时存在课程汇总记录（catalog_id 为空）和小节记录，
     * 两类记录都要重置；学习计划只重置进度，不删除计划。这样不会破坏报名
     * 或支付关系，也能避免用户端继续学习时跳回旧的小节进度。</p>
     */
    @Transactional
    public Map<String, Object> restartLearning(Long courseId) {
        EduCourse course = requireCourse(courseId);
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

    public Map<String, Object> learningPage(long pageNo, long pageSize, boolean current) {
        Page<EduLearningRecord> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        LambdaQueryWrapper<EduLearningRecord> wrapper = new LambdaQueryWrapper<EduLearningRecord>()
                .eq(EduLearningRecord::getUserId, currentUserId());
        if (current) {
            wrapper.lt(EduLearningRecord::getProgressPercent, BigDecimal.valueOf(100));
        }
        wrapper.orderByDesc(EduLearningRecord::getLastLearnTime).orderByDesc(EduLearningRecord::getUpdateTime);
        learningMapper.selectPage(page, wrapper);
        return pageView(page.getTotal(), page.getRecords().stream().map(this::learningView).toList());
    }

    public List<Map<String, Object>> plans() {
        return planMapper.selectList(new LambdaQueryWrapper<EduLearningPlan>()
                .eq(EduLearningPlan::getUserId, currentUserId()).orderByAsc(EduLearningPlan::getTargetDate))
                .stream().map(this::planView).toList();
    }

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

    @Transactional
    public void removeLearning(Long courseId) {
        learningMapper.delete(new LambdaQueryWrapper<EduLearningRecord>()
                .eq(EduLearningRecord::getUserId, currentUserId()).eq(EduLearningRecord::getCourseId, courseId));
        planMapper.delete(new LambdaQueryWrapper<EduLearningPlan>()
                .eq(EduLearningPlan::getUserId, currentUserId()).eq(EduLearningPlan::getCourseId, courseId));
    }

    @Transactional
    public EduLearningRecord saveLearning(EduLearningRecord value) {
        require(value != null && value.getCourseId() != null, "课程不能为空");
        Long userId = currentUserId();
        EduLearningRecord old = learningMapper.selectOne(new LambdaQueryWrapper<EduLearningRecord>()
                .eq(EduLearningRecord::getUserId, userId).eq(EduLearningRecord::getCourseId, value.getCourseId())
                .eq(value.getCatalogId() != null, EduLearningRecord::getCatalogId, value.getCatalogId())
                .last("limit 1"));
        LocalDateTime now = LocalDateTime.now();
        if (old == null) {
            value.setId(newId());
            value.setUserId(userId);
            value.setProgressPercent(defaultValue(value.getProgressPercent(), BigDecimal.ZERO));
            value.setProgressSeconds(defaultValue(value.getProgressSeconds(), 0));
            value.setLearnDurationSeconds(defaultValue(value.getLearnDurationSeconds(), 0));
            value.setCompletedLessons(defaultValue(value.getCompletedLessons(), 0));
            value.setTotalLessons(defaultValue(value.getTotalLessons(), courseLessonCount(value.getCourseId())));
            value.setStatus(defaultValue(value.getStatus(), 1));
            value.setLastLearnTime(now);
            value.setCreateTime(now);
            value.setUpdateTime(now);
            value.setDelFlag(0);
            value.setVersion(0);
            learningMapper.insert(value);
            return value;
        }
        if (value.getProgressPercent() != null) old.setProgressPercent(value.getProgressPercent());
        if (value.getProgressSeconds() != null) old.setProgressSeconds(value.getProgressSeconds());
        if (value.getLearnDurationSeconds() != null) old.setLearnDurationSeconds(value.getLearnDurationSeconds());
        if (value.getCompletedLessons() != null) old.setCompletedLessons(value.getCompletedLessons());
        if (value.getTotalLessons() != null) old.setTotalLessons(value.getTotalLessons());
        if (value.getStatus() != null) old.setStatus(value.getStatus());
        old.setLastLearnTime(now);
        old.setUpdateTime(now);
        learningMapper.updateById(old);
        return old;
    }

    /** 题库旧接口和社区问答共用 /questions/page，带题型/分类参数时按题库查询。 */
    public Map<String, Object> questionPage(Map<String, ?> params) {
        if (isQuestionBankQuery(params)) return legacyQuestionPage(params);
        long pageNo = number(params, "pageNo", 1);
        long pageSize = number(params, "pageSize", 10);
        Page<EduQuestion> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        Long courseId = longValue(params.get("courseId"));
        Long sectionId = longValue(params.get("sectionId"));
        boolean onlyMine = bool(params.get("onlyMine"));
        String keyword = defaultText(params.get("keyword"), defaultText(params.get("title"), null));
        String solved = defaultText(params.get("solved"), defaultText(params.get("status"), null));
        LambdaQueryWrapper<EduQuestion> wrapper = new LambdaQueryWrapper<EduQuestion>()
                .eq(EduQuestion::getHidden, 0).eq(EduQuestion::getStatus, ENABLED)
                .eq(courseId != null && courseId > 0, EduQuestion::getCourseId, courseId)
                .eq(sectionId != null && sectionId > 0, EduQuestion::getId, sectionId)
                .eq(onlyMine, EduQuestion::getUserId, currentUserId())
                .and(StringUtils.hasText(keyword), item -> item.like(EduQuestion::getTitle, keyword)
                        .or().like(EduQuestion::getContent, keyword))
                .ge("solved".equalsIgnoreCase(solved), EduQuestion::getReplyCount, 1)
                .eq("unsolved".equalsIgnoreCase(solved), EduQuestion::getReplyCount, 0)
                .orderByDesc(EduQuestion::getCreateTime);
        questionMapper.selectPage(page, wrapper);
        return pageView(page.getTotal(), page.getRecords().stream().map(this::questionView).toList());
    }

    public Map<String, Object> legacyQuestionPage(Map<String, ?> params) {
        long pageNo = number(params, "pageNo", number(params, "pageNum", 1));
        long pageSize = number(params, "pageSize", 10);
        String type = defaultText(params == null ? null : params.get("type"),
                defaultText(params == null ? null : params.get("questionType"), null));
        String keyword = defaultText(params == null ? null : params.get("keyword"),
                defaultText(params == null ? null : params.get("title"), null));
        Page<EduExamQuestionBank> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        Long categoryId = longValue(params == null ? null : params.get("categoryId"));
        questionBankMapper.selectPage(page, new LambdaQueryWrapper<EduExamQuestionBank>()
                .like(StringUtils.hasText(keyword), EduExamQuestionBank::getStem, keyword)
                .eq(StringUtils.hasText(type), EduExamQuestionBank::getQuestionType, normalizeQuestionType(type))
                .eq(categoryId != null && categoryId > 0, EduExamQuestionBank::getCategoryId, categoryId)
                .eq(EduExamQuestionBank::getStatus, ENABLED)
                .orderByDesc(EduExamQuestionBank::getCreateTime));
        return pageView(page.getTotal(), page.getRecords().stream().map(this::legacyQuestionView).toList());
    }

    public boolean isQuestionBankPayload(Map<String, ?> payload) {
        if (payload == null) return false;
        return payload.containsKey("questionType") || payload.containsKey("options")
                || payload.containsKey("answer") || payload.containsKey("correctAnswer")
                || payload.containsKey("stem") || payload.containsKey("type") && !payload.containsKey("content");
    }

    @Transactional
    public Object saveQuestionPayload(Map<String, ?> payload) {
        if (!isQuestionBankPayload(payload)) {
            EduQuestion value = objectMapper.convertValue(payload, EduQuestion.class);
            return saveQuestion(value);
        }
        Long id = longValue(payload.get("id"));
        EduExamQuestionBank value = id == null ? new EduExamQuestionBank() : questionBankMapper.selectById(id);
        if (value == null) throw new ServiceException("题目不存在");
        String stem = defaultText(payload.get("title"), defaultText(payload.get("stem"), null));
        require(StringUtils.hasText(stem), "题目内容不能为空");
        value.setQuestionType(normalizeQuestionType(defaultText(payload.get("type"),
                defaultText(payload.get("questionType"), value.getQuestionType()))));
        value.setCategoryId(defaultValue(longValue(payload.get("categoryId")), value.getCategoryId()));
        value.setStem(stem);
        Object options = payload.get("options");
        if (options != null) {
            try { value.setOptionsJson(objectMapper.writeValueAsString(options)); }
            catch (Exception ex) { throw new ServiceException("题目选项格式不正确"); }
        }
        value.setCorrectAnswer(defaultText(payload.get("answer"),
                defaultText(payload.get("correctAnswer"), value.getCorrectAnswer())));
        value.setScore(decimalValue(payload.get("score"), defaultValue(value.getScore(), BigDecimal.TEN)));
        value.setDifficulty(intValue(payload.get("difficulty"), defaultValue(value.getDifficulty(), 2)));
        value.setStatus(defaultValue(value.getStatus(), ENABLED));
        LocalDateTime now = LocalDateTime.now();
        if (value.getId() == null) {
            value.setId(newId()); value.setCreateTime(now); value.setUpdateTime(now); value.setDelFlag(0); value.setVersion(0);
            questionBankMapper.insert(value);
        } else {
            value.setUpdateTime(now); questionBankMapper.updateById(value);
        }
        return legacyQuestionView(value);
    }

    public Map<String, Object> checkQuestionName(Map<String, ?> params) {
        String stem = defaultText(params == null ? null : params.get("title"),
                defaultText(params == null ? null : params.get("stem"), null));
        Long id = longValue(params == null ? null : params.get("id"));
        boolean existed = StringUtils.hasText(stem) && questionBankMapper.selectCount(new LambdaQueryWrapper<EduExamQuestionBank>()
                .eq(EduExamQuestionBank::getStem, stem.trim()).ne(id != null, EduExamQuestionBank::getId, id)) > 0;
        return new LinkedHashMap<>(Map.of("existed", existed, "name", defaultText(stem, "")));
    }

    public Object questionOrQuestionBank(Long id) {
        EduExamQuestionBank bank = id == null ? null : questionBankMapper.selectById(id);
        return bank == null ? question(id) : legacyQuestionView(bank);
    }

    @Transactional
    public void removeQuestionPayload(Long id) {
        EduExamQuestionBank bank = id == null ? null : questionBankMapper.selectById(id);
        if (bank != null) questionBankMapper.deleteById(id); else removeQuestion(id);
    }

    public List<Map<String, Object>> legacyBizQuestions(Long bizId) {
        if (bizId == null) return List.of();
        return catalogQuestionMapper.selectList(new LambdaQueryWrapper<EduCatalogQuestion>()
                .eq(EduCatalogQuestion::getCatalogId, bizId).orderByAsc(EduCatalogQuestion::getSortNum))
                .stream().map(item -> questionBankMapper.selectById(item.getQuestionId()))
                .filter(Objects::nonNull).map(this::legacyQuestionView).toList();
    }

    public Map<String, Object> question(Long id) {
        EduQuestion value = requireQuestion(id);
        value.setViewCount(defaultValue(value.getViewCount(), 0) + 1);
        questionMapper.updateById(value);
        return questionView(value);
    }

    @Transactional
    public EduQuestion saveQuestion(EduQuestion value) {
        require(value != null && StringUtils.hasText(value.getTitle()), "问题标题不能为空");
        require(StringUtils.hasText(value.getContent()), "问题内容不能为空");
        LocalDateTime now = LocalDateTime.now();
        if (value.getId() == null) {
            value.setId(newId());
            value.setUserId(currentUserId());
            value.setViewCount(0);
            value.setReplyCount(0);
            value.setLikeCount(0);
            value.setHidden(0);
            value.setStatus(ENABLED);
            value.setCreateTime(now);
            value.setUpdateTime(now);
            value.setDelFlag(0);
            value.setVersion(0);
            questionMapper.insert(value);
        } else {
            EduQuestion old = requireQuestion(value.getId());
            if (!Objects.equals(old.getUserId(), currentUserId()) && !SecurityUtils.isAdmin(currentUserId())) {
                throw new ServiceException("只能编辑自己的问题");
            }
            value.setUserId(old.getUserId());
            value.setUpdateTime(now);
            questionMapper.updateById(value);
        }
        return value;
    }

    @Transactional
    public void removeQuestion(Long id) {
        EduQuestion value = requireQuestion(id);
        if (!Objects.equals(value.getUserId(), currentUserId()) && !SecurityUtils.isAdmin(currentUserId())) {
            throw new ServiceException("只能删除自己的问题");
        }
        questionMapper.deleteById(id);
    }

    public Map<String, Object> reply(Long id) {
        EduReply value = id == null ? null : replyMapper.selectById(id);
        if (value == null) throw new ServiceException("回复不存在");
        return replyView(value);
    }

    @Transactional
    public void setQuestionHidden(Long id, boolean hidden) {
        EduQuestion value = requireQuestion(id);
        value.setHidden(hidden ? 1 : 0);
        value.setUpdateTime(LocalDateTime.now());
        questionMapper.updateById(value);
    }

    @Transactional
    public void setReplyHidden(Long id, boolean hidden) {
        EduReply value = id == null ? null : replyMapper.selectById(id);
        if (value == null) throw new ServiceException("回复不存在");
        value.setHidden(hidden ? 1 : 0);
        value.setUpdateTime(LocalDateTime.now());
        replyMapper.updateById(value);
    }

    public Map<String, Object> replyPage(Map<String, ?> params) {
        long pageNo = number(params, "pageNo", 1);
        long pageSize = number(params, "pageSize", 10);
        Long questionId = longValue(params.get("questionId"));
        Long answerId = longValue(params.get("answerId"));
        Page<EduReply> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        LambdaQueryWrapper<EduReply> wrapper = new LambdaQueryWrapper<EduReply>()
                .eq(EduReply::getHidden, 0).eq(EduReply::getStatus, ENABLED)
                .eq(questionId != null, EduReply::getQuestionId, questionId)
                .eq(answerId != null && answerId > 0, EduReply::getParentId, answerId)
                .orderByAsc(EduReply::getCreateTime);
        replyMapper.selectPage(page, wrapper);
        return pageView(page.getTotal(), page.getRecords().stream().map(this::replyView).toList());
    }

    @Transactional
    public EduReply saveReply(EduReply value) {
        require(value != null && value.getQuestionId() != null, "问题编号不能为空");
        require(StringUtils.hasText(value.getContent()), "回复内容不能为空");
        requireQuestion(value.getQuestionId());
        LocalDateTime now = LocalDateTime.now();
        value.setId(newId());
        value.setUserId(currentUserId());
        value.setLikeCount(0);
        value.setHidden(0);
        value.setStatus(ENABLED);
        value.setCreateTime(now);
        value.setUpdateTime(now);
        value.setDelFlag(0);
        value.setVersion(0);
        replyMapper.insert(value);
        EduQuestion question = requireQuestion(value.getQuestionId());
        question.setReplyCount(defaultValue(question.getReplyCount(), 0) + 1);
        question.setUpdateTime(now);
        questionMapper.updateById(question);
        return value;
    }

    public Map<String, Object> notePage(Map<String, ?> params) {
        long pageNo = number(params, "pageNo", 1);
        long pageSize = number(params, "pageSize", 10);
        Long courseId = longValue(params.get("courseId"));
        Long catalogId = longValue(params.get("sectionId"));
        if (catalogId == null) catalogId = longValue(params.get("catalogId"));
        boolean onlyMine = bool(params.get("onlyMine"));
        Page<EduNote> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        Long userId = currentUserId();
        boolean admin = SecurityUtils.isAdmin(userId);
        noteMapper.selectPage(page, new LambdaQueryWrapper<EduNote>()
                .eq(courseId != null && courseId > 0, EduNote::getCourseId, courseId)
                .eq(catalogId != null && catalogId > 0, EduNote::getCatalogId, catalogId)
                .eq(onlyMine, EduNote::getUserId, userId)
                .and(!onlyMine && !admin, item -> item.eq(EduNote::getVisibility, 1).or().eq(EduNote::getUserId, userId))
                .eq(EduNote::getHidden, 0).eq(EduNote::getStatus, ENABLED)
                .orderByDesc(EduNote::getCreateTime));
        return pageView(page.getTotal(), page.getRecords().stream().map(this::noteView).toList());
    }

    @Transactional
    public EduNote saveNote(EduNote value) {
        require(value != null && StringUtils.hasText(value.getContent()), "笔记内容不能为空");
        LocalDateTime now = LocalDateTime.now();
        if (value.getId() == null) {
            value.setId(newId());
            value.setUserId(currentUserId());
            value.setTitle(StringUtils.hasText(value.getTitle()) ? value.getTitle() : "学习笔记");
            value.setVisibility(defaultValue(value.getVisibility(), 1));
            value.setLikeCount(0);
            value.setCollectCount(0);
            value.setHidden(0);
            value.setStatus(ENABLED);
            value.setCreateTime(now);
            value.setUpdateTime(now);
            value.setDelFlag(0);
            value.setVersion(0);
            noteMapper.insert(value);
        } else {
            EduNote old = requireNote(value.getId());
            if (!Objects.equals(old.getUserId(), currentUserId()) && !SecurityUtils.isAdmin(currentUserId())) {
                throw new ServiceException("只能编辑自己的笔记");
            }
            value.setUserId(old.getUserId());
            value.setUpdateTime(now);
            noteMapper.updateById(value);
        }
        return value;
    }

    @Transactional
    public void removeNote(Long id) {
        EduNote value = requireNote(id);
        if (!Objects.equals(value.getUserId(), currentUserId()) && !SecurityUtils.isAdmin(currentUserId())) {
            throw new ServiceException("只能删除自己的笔记");
        }
        noteMapper.deleteById(id);
    }

    public Map<String, Object> note(Long id) {
        return noteView(requireNote(id));
    }

    @Transactional
    public void setNoteHidden(Long id, boolean hidden) {
        EduNote value = requireNote(id);
        value.setHidden(hidden ? 1 : 0);
        value.setUpdateTime(LocalDateTime.now());
        noteMapper.updateById(value);
    }

    @Transactional
    public void setNoteVisibility(Long id, boolean visible) {
        EduNote value = requireNote(id);
        value.setVisibility(visible ? 1 : 0);
        value.setUpdateTime(LocalDateTime.now());
        noteMapper.updateById(value);
    }

    @Transactional
    public boolean collectNote(Long noteId, boolean collect) {
        EduNote note = requireNote(noteId);
        Long userId = currentUserId();
        EduNoteCollect old = noteCollectMapper.selectOne(new LambdaQueryWrapper<EduNoteCollect>()
                .eq(EduNoteCollect::getNoteId, noteId).eq(EduNoteCollect::getUserId, userId));
        if (collect && old == null) {
            old = new EduNoteCollect();
            old.setId(newId());
            old.setNoteId(noteId);
            old.setUserId(userId);
            old.setCreateTime(LocalDateTime.now());
            noteCollectMapper.insert(old);
            note.setCollectCount(defaultValue(note.getCollectCount(), 0) + 1);
        } else if (!collect && old != null) {
            noteCollectMapper.deleteById(old.getId());
            note.setCollectCount(Math.max(defaultValue(note.getCollectCount(), 0) - 1, 0));
        }
        noteMapper.updateById(note);
        return collect;
    }

    @Transactional
    public boolean like(String bizType, Long bizId, boolean liked) {
        if ("NOTE".equalsIgnoreCase(bizType)) {
            EduNote note = requireNote(bizId);
            EduNoteLike old = noteLikeMapper.selectOne(new LambdaQueryWrapper<EduNoteLike>()
                    .eq(EduNoteLike::getNoteId, bizId).eq(EduNoteLike::getUserId, currentUserId()));
            if (liked && old == null) {
                old = new EduNoteLike();
                old.setId(newId()); old.setNoteId(bizId); old.setUserId(currentUserId()); old.setCreateTime(LocalDateTime.now());
                noteLikeMapper.insert(old);
                note.setLikeCount(defaultValue(note.getLikeCount(), 0) + 1);
            } else if (!liked && old != null) {
                noteLikeMapper.deleteById(old.getId());
                note.setLikeCount(Math.max(defaultValue(note.getLikeCount(), 0) - 1, 0));
            }
            noteMapper.updateById(note);
            return liked;
        }
        EduQuestion question = requireQuestion(bizId);
        EduQuestionLike old = questionLikeMapper.selectOne(new LambdaQueryWrapper<EduQuestionLike>()
                .eq(EduQuestionLike::getQuestionId, bizId).eq(EduQuestionLike::getUserId, currentUserId()));
        if (liked && old == null) {
            old = new EduQuestionLike();
            old.setId(newId()); old.setQuestionId(bizId); old.setUserId(currentUserId()); old.setCreateTime(LocalDateTime.now());
            questionLikeMapper.insert(old);
            question.setLikeCount(defaultValue(question.getLikeCount(), 0) + 1);
        } else if (!liked && old != null) {
            questionLikeMapper.deleteById(old.getId());
            question.setLikeCount(Math.max(defaultValue(question.getLikeCount(), 0) - 1, 0));
        }
        questionMapper.updateById(question);
        return liked;
    }

    public Map<String, Object> examsPage(Map<String, ?> params) {
        long pageNo = number(params, "pageNo", 1);
        long pageSize = number(params, "pageSize", 10);
        Page<EduExamRecord> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        examRecordMapper.selectPage(page, new LambdaQueryWrapper<EduExamRecord>()
                .eq(EduExamRecord::getUserId, currentUserId()).orderByDesc(EduExamRecord::getCreateTime));
        return pageView(page.getTotal(), page.getRecords().stream().map(this::examRecordView).toList());
    }

    /**
     * 查询一条考试记录及其答题明细。旧前端将考试记录详情当成题目明细使用，
     * 这里在新表结构之上补齐兼容视图，避免前端再依赖页面内的题目静态数据。
     */
    public Map<String, Object> examRecordDetails(Long recordId) {
        EduExamRecord record = recordId == null ? null : examRecordMapper.selectById(recordId);
        require(record != null, "考试记录不存在");
        Long userId = currentUserId();
        require(Objects.equals(record.getUserId(), userId) || SecurityUtils.isAdmin(userId), "无权查看该考试记录");

        EduExam exam = examMapper.selectById(record.getExamId());
        Map<String, Object> result = new LinkedHashMap<>(examRecordView(record));
        result.put("courseId", exam == null ? null : exam.getCourseId());
        result.put("courseName", exam == null ? "" : courseName(exam.getCourseId()));
        result.put("sectionName", exam == null ? "" : exam.getExamName());
        result.put("commitTime", record.getSubmittedAt());
        result.put("duration", durationSeconds(record.getStartedAt(), record.getSubmittedAt()));

        List<EduExamAnswer> answers = examAnswerMapper.selectList(new LambdaQueryWrapper<EduExamAnswer>()
                .eq(EduExamAnswer::getRecordId, record.getId()).orderByAsc(EduExamAnswer::getCreateTime));
        Map<Long, EduExamAnswer> answerByQuestion = answers.stream()
                .collect(Collectors.toMap(EduExamAnswer::getQuestionId, Function.identity(), (left, right) -> right,
                        LinkedHashMap::new));
        List<Long> questionIds = exam == null ? List.of() : examQuestionMapper.selectList(new LambdaQueryWrapper<EduExamQuestion>()
                .eq(EduExamQuestion::getExamId, exam.getId()).orderByAsc(EduExamQuestion::getSortNum))
                .stream().map(EduExamQuestion::getQuestionId).toList();
        if (questionIds.isEmpty()) {
            questionIds = answers.stream().map(EduExamAnswer::getQuestionId).filter(Objects::nonNull).toList();
        }
        if (questionIds.isEmpty()) {
            questionIds = questionBankMapper.selectList(new LambdaQueryWrapper<EduExamQuestionBank>()
                    .eq(EduExamQuestionBank::getStatus, ENABLED).orderByAsc(EduExamQuestionBank::getId))
                    .stream().map(EduExamQuestionBank::getId).toList();
        }

        List<Map<String, Object>> details = new ArrayList<>();
        for (Long questionId : questionIds) {
            EduExamQuestionBank question = questionBankMapper.selectById(questionId);
            if (question == null) continue;
            EduExamAnswer answer = answerByQuestion.get(questionId);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("question", legacyQuestionView(question));
            item.put("answer", answer == null ? "" : answer.getUserAnswer());
            item.put("correct", answer != null && Integer.valueOf(1).equals(answer.getIsCorrect()));
            item.put("score", answer == null ? BigDecimal.ZERO : defaultValue(answer.getScore(), BigDecimal.ZERO));
            details.add(item);
        }
        result.put("details", details);
        result.put("list", details);
        return result;
    }

    public Map<String, Object> exam(Long id) {
        EduExam exam = examMapper.selectById(id);
        if (exam == null) {
            EduExamRecord record = examRecordMapper.selectById(id);
            if (record != null) exam = examMapper.selectById(record.getExamId());
        }
        if (exam == null) throw new ServiceException("考试不存在");
        Map<String, Object> result = examView(exam);
        List<EduExamQuestion> relations = examQuestionMapper.selectList(new LambdaQueryWrapper<EduExamQuestion>()
                .eq(EduExamQuestion::getExamId, exam.getId()).orderByAsc(EduExamQuestion::getSortNum));
        List<Long> questionIds = relations.stream().map(EduExamQuestion::getQuestionId).toList();
        List<EduExamQuestionBank> questions = questionIds.isEmpty()
                ? questionBankMapper.selectList(new LambdaQueryWrapper<EduExamQuestionBank>()
                        .eq(EduExamQuestionBank::getStatus, ENABLED).orderByAsc(EduExamQuestionBank::getId))
                : questionIds.stream().map(questionBankMapper::selectById).filter(Objects::nonNull).toList();
        // /es/exams 是旧用户端答题页使用的接口，必须返回数字题型和数组选项。
        // 同时保留 questionBankQuestions 供新客户端使用规范化题库字段。
        result.put("questions", questions.stream().map(this::legacyQuestionView).toList());
        result.put("questionBankQuestions", questions.stream().map(this::questionBankView).toList());
        result.put("questionCount", questions.size());
        return result;
    }

    public Map<String, Object> examQuestions(Map<String, ?> params) {
        Map<String, ?> request = params == null ? Map.of() : params;
        Long examId = longValue(request.get("examId"));
        if (examId == null) examId = longValue(request.get("id"));
        if (examId != null) return exam(examId);
        Long courseId = longValue(request.get("courseId"));
        EduExam value = examMapper.selectOne(new LambdaQueryWrapper<EduExam>()
                .eq(courseId != null, EduExam::getCourseId, courseId)
                .eq(EduExam::getStatus, ENABLED).orderByAsc(EduExam::getId).last("limit 1"));
        if (value != null) return exam(value.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("questions", questionBankMapper.selectList(new LambdaQueryWrapper<EduExamQuestionBank>()
                .eq(EduExamQuestionBank::getStatus, ENABLED).orderByAsc(EduExamQuestionBank::getId)).stream()
                .map(this::legacyQuestionView).toList());
        return result;
    }

    @Transactional
    public EduExamRecord startExam(EduExamRecord value) {
        require(value != null && value.getExamId() != null, "考试编号不能为空");
        EduExam exam = examMapper.selectById(value.getExamId());
        require(exam != null, "考试不存在");
        value.setId(newId());
        value.setUserId(currentUserId());
        // 总分和题数必须以服务端考试配置为准，不能信任客户端提交的数值。
        value.setTotalScore(defaultValue(exam.getTotalScore(), BigDecimal.valueOf(100)));
        value.setQuestionCount(examQuestionCount(exam.getId()));
        value.setCorrectCount(0);
        value.setStatus(0);
        value.setStartedAt(LocalDateTime.now());
        value.setCreateTime(LocalDateTime.now());
        value.setUpdateTime(LocalDateTime.now());
        value.setDelFlag(0);
        value.setVersion(0);
        examRecordMapper.insert(value);
        return value;
    }

    @Transactional
    public Map<String, Object> submitExam(Map<String, ?> payload) {
        Map<String, ?> request = payload == null ? Map.of() : payload;
        Long recordId = longValue(request.get("recordId"));
        EduExamRecord record = recordId == null ? null : examRecordMapper.selectById(recordId);
        if (record != null) {
            Long userId = currentUserId();
            require(Objects.equals(record.getUserId(), userId) || SecurityUtils.isAdmin(userId), "无权提交该考试记录");
            // 已提交记录重复请求直接返回原结果，避免违反 uk_exam_answer 唯一索引或重复累计分数。
            if (!Integer.valueOf(0).equals(record.getStatus())) return examRecordView(record);
        }
        if (record == null) {
            Long examId = longValue(request.get("examId"));
            if (examId == null) examId = longValue(request.get("id"));
            require(examId != null, "考试编号不能为空");
            EduExamRecord newRecord = new EduExamRecord();
            newRecord.setExamId(examId);
            record = startExam(newRecord);
        }
        EduExam exam = examMapper.selectById(record.getExamId());
        require(exam != null, "考试不存在");

        List<EduExamQuestion> relations = examQuestionMapper.selectList(new LambdaQueryWrapper<EduExamQuestion>()
                .eq(EduExamQuestion::getExamId, exam.getId()).orderByAsc(EduExamQuestion::getSortNum));
        Map<Long, EduExamQuestion> relationByQuestion = relations.stream()
                .collect(Collectors.toMap(EduExamQuestion::getQuestionId, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));
        boolean hasExplicitQuestions = !relationByQuestion.isEmpty();
        List<EduExamQuestionBank> examQuestions = hasExplicitQuestions
                ? relationByQuestion.keySet().stream().map(questionBankMapper::selectById).filter(Objects::nonNull).toList()
                : questionBankMapper.selectList(new LambdaQueryWrapper<EduExamQuestionBank>()
                        .eq(EduExamQuestionBank::getStatus, ENABLED).orderByAsc(EduExamQuestionBank::getId));
        Map<Long, EduExamAnswer> existingAnswers = examAnswerMapper.selectList(new LambdaQueryWrapper<EduExamAnswer>()
                .eq(EduExamAnswer::getRecordId, record.getId())).stream()
                .collect(Collectors.toMap(EduExamAnswer::getQuestionId, Function.identity(), (left, right) -> right,
                        LinkedHashMap::new));

        Object answersObject = request.get("answers");
        // 原 tj-protal 使用 examDetails + id，新接口使用 answers + examId/recordId，两个契约都支持。
        if (!(answersObject instanceof List<?>)) answersObject = request.get("examDetails");
        int correct = 0;
        BigDecimal score = BigDecimal.ZERO;
        Set<Long> processedQuestions = new HashSet<>();
        if (answersObject instanceof List<?> answers) {
            for (Object item : answers) {
                if (!(item instanceof Map<?, ?> answer)) continue;
                Long questionId = longValue(answer.get("questionId"));
                require(questionId != null, "题目编号不能为空");
                require(processedQuestions.add(questionId), "同一题目不能重复提交");
                EduExamQuestionBank question = questionId == null ? null : questionBankMapper.selectById(questionId);
                EduExamQuestion relation = relationByQuestion.get(questionId);
                require(question != null && (!hasExplicitQuestions || relation != null), "提交的题目不属于该考试");
                String userAnswer = normalizeAnswer(text(answer.get("answer")), question.getQuestionType());
                boolean right = question != null && sameAnswer(question.getCorrectAnswer(), userAnswer, question.getQuestionType());
                BigDecimal itemScore = relation != null && relation.getScore() != null
                        && relation.getScore().compareTo(BigDecimal.ZERO) > 0
                        ? relation.getScore() : defaultValue(question.getScore(), BigDecimal.ZERO);
                if (right) { correct++; score = score.add(itemScore); }
                EduExamAnswer entity = existingAnswers.get(questionId);
                if (entity == null) {
                    entity = new EduExamAnswer();
                    entity.setId(newId()); entity.setRecordId(record.getId()); entity.setQuestionId(questionId);
                }
                entity.setUserAnswer(userAnswer); entity.setIsCorrect(right ? 1 : 0); entity.setScore(right ? itemScore : BigDecimal.ZERO);
                if (entity.getCreateTime() == null) entity.setCreateTime(LocalDateTime.now());
                if (existingAnswers.containsKey(questionId)) examAnswerMapper.updateById(entity);
                else { examAnswerMapper.insert(entity); existingAnswers.put(questionId, entity); }
            }
        }
        record.setScore(score);
        record.setCorrectCount(correct);
        BigDecimal passScore = defaultValue(exam.getPassScore(), BigDecimal.valueOf(60));
        record.setStatus(score.compareTo(passScore) >= 0 ? 1 : 2);
        record.setSubmittedAt(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        examRecordMapper.updateById(record);
        Map<String, Object> result = examRecordView(record);
        result.put("score", score);
        result.put("correctCount", correct);
        result.put("passScore", passScore);
        result.put("questions", examQuestions.stream().map(this::legacyQuestionView).toList());
        return result;
    }

    public Map<String, Object> signInfo() {
        Long userId = currentUserId();
        List<EduSignRecord> records = signMapper.selectList(new LambdaQueryWrapper<EduSignRecord>()
                .eq(EduSignRecord::getUserId, userId).orderByDesc(EduSignRecord::getSignDate));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalDays", records.size());
        result.put("continuousDays", records.stream().findFirst().map(EduSignRecord::getContinuousDays).orElse(0));
        result.put("todaySigned", records.stream().anyMatch(item -> LocalDate.now().equals(item.getSignDate())));
        result.put("records", records.stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", item.getSignDate()); row.put("points", item.getPoints()); row.put("continuousDays", item.getContinuousDays());
            return row;
        }).toList());
        return result;
    }

    @Transactional
    public Map<String, Object> sign() {
        Long userId = currentUserId();
        LocalDate today = LocalDate.now();
        EduSignRecord old = signMapper.selectOne(new LambdaQueryWrapper<EduSignRecord>()
                .eq(EduSignRecord::getUserId, userId).eq(EduSignRecord::getSignDate, today));
        if (old != null) return pointsToday();
        EduSignRecord previous = signMapper.selectOne(new LambdaQueryWrapper<EduSignRecord>()
                .eq(EduSignRecord::getUserId, userId).orderByDesc(EduSignRecord::getSignDate).last("limit 1"));
        int continuous = previous != null && today.minusDays(1).equals(previous.getSignDate())
                ? defaultValue(previous.getContinuousDays(), 0) + 1 : 1;
        EduSignRecord record = new EduSignRecord();
        record.setId(newId()); record.setUserId(userId); record.setSignDate(today); record.setPoints(SIGN_POINTS);
        record.setContinuousDays(continuous); record.setCreateTime(LocalDateTime.now());
        signMapper.insert(record);
        int balance = totalPoints(userId) + SIGN_POINTS;
        EduPointsLedger ledger = new EduPointsLedger();
        ledger.setId(newId()); ledger.setUserId(userId); ledger.setChangeAmount(SIGN_POINTS); ledger.setBalanceAfter(balance);
        ledger.setSourceType("sign"); ledger.setBizId(today.toString()); ledger.setRemark("每日签到"); ledger.setCreateTime(LocalDateTime.now());
        pointsMapper.insert(ledger);
        return pointsToday();
    }

    public Map<String, Object> pointsToday() {
        Long userId = currentUserId();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todayPoints", signMapper.selectOne(new LambdaQueryWrapper<EduSignRecord>()
                .eq(EduSignRecord::getUserId, userId).eq(EduSignRecord::getSignDate, LocalDate.now())));
        EduSignRecord today = signMapper.selectOne(new LambdaQueryWrapper<EduSignRecord>()
                .eq(EduSignRecord::getUserId, userId).eq(EduSignRecord::getSignDate, LocalDate.now()));
        result.put("todayPoints", today == null ? 0 : today.getPoints());
        result.put("totalPoints", totalPoints(userId));
        result.put("rank", pointsRank(userId));
        return result;
    }

    public List<Map<String, Object>> pointsBoard(Map<String, ?> params) {
        Map<Long, Integer> totals = latestPointsByUser();
        List<Map<String, Object>> result = new ArrayList<>();
        totals.entrySet().stream().sorted(Map.Entry.<Long, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey())).limit(50).forEach(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", item.getKey()); row.put("userName", item.getKey().equals(currentUserId()) ? currentUserName() : "学习者" + item.getKey());
            row.put("points", item.getValue()); row.put("rank", result.size() + 1); result.add(row);
        });
        if (result.isEmpty()) {
            Map<String, Object> row = new LinkedHashMap<>(); row.put("userId", currentUserId()); row.put("userName", currentUserName());
            row.put("points", totalPoints(currentUserId())); row.put("rank", 1); result.add(row);
        }
        return result;
    }

    public Map<String, Object> statistics() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCourses", courseMapper.selectCount(new LambdaQueryWrapper<>()));
        result.put("publishedCourses", courseMapper.selectCount(new LambdaQueryWrapper<EduCourse>().eq(EduCourse::getStatus, ENABLED)));
        result.put("totalCategories", categoryMapper.selectCount(new LambdaQueryWrapper<>()));
        result.put("totalTeachers", teacherMapper.selectCount(new LambdaQueryWrapper<EduTeacher>().eq(EduTeacher::getStatus, ENABLED)));
        result.put("totalStudents", defaultValue(dashboardDailyMapper.selectDistinctStudentCount(), 0L));
        result.put("totalQuestions", questionMapper.selectCount(new LambdaQueryWrapper<>()));
        result.put("totalNotes", noteMapper.selectCount(new LambdaQueryWrapper<>()));
        result.put("totalExams", examMapper.selectCount(new LambdaQueryWrapper<>()));
        return result;
    }

    /**
     * 查询工作台趋势数据。
     *
     * <p>返回固定长度的日期序列，缺失日期补零，避免前端依赖静态 Mock 数据或自行拼接日期。</p>
     */
    public List<EduDashboardDaily> dashboardDaily(int days) {
        int size = Math.max(1, Math.min(days, 31));
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(size - 1L);
        List<EduDashboardDaily> records = dashboardDailyMapper.selectList(new LambdaQueryWrapper<EduDashboardDaily>()
                .between(EduDashboardDaily::getStatDate, start, end)
                .orderByAsc(EduDashboardDaily::getStatDate));
        Map<LocalDate, EduDashboardDaily> byDate = records.stream()
                .filter(item -> item.getStatDate() != null)
                .collect(Collectors.toMap(EduDashboardDaily::getStatDate, Function.identity(), (left, right) -> right,
                        LinkedHashMap::new));
        List<EduDashboardDaily> result = new ArrayList<>(size);
        for (int offset = size - 1; offset >= 0; offset--) {
            LocalDate date = end.minusDays(offset);
            EduDashboardDaily item = byDate.get(date);
            result.add(item == null ? emptyDashboardDaily(date) : item);
        }
        return result;
    }

    /** 查询今日快照；本地演示数据缺少当天记录时，使用最近一条已汇总数据避免工作台空白。 */
    public EduDashboardDaily dashboardToday() {
        EduDashboardDaily today = dashboardDailyMapper.selectOne(new LambdaQueryWrapper<EduDashboardDaily>()
                .eq(EduDashboardDaily::getStatDate, LocalDate.now()));
        if (today != null) return today;
        return dashboardDailyMapper.selectOne(new LambdaQueryWrapper<EduDashboardDaily>()
                .orderByDesc(EduDashboardDaily::getStatDate).last("limit 1"));
    }

    /** 查询今日快照的上一日数据，用于计算工作台卡片的环比变化。 */
    public EduDashboardDaily dashboardPrevious(EduDashboardDaily current) {
        if (current == null || current.getStatDate() == null) return null;
        return dashboardDailyMapper.selectOne(new LambdaQueryWrapper<EduDashboardDaily>()
                .eq(EduDashboardDaily::getStatDate, current.getStatDate().minusDays(1L)));
    }

    private EduDashboardDaily emptyDashboardDaily(LocalDate date) {
        EduDashboardDaily result = new EduDashboardDaily();
        result.setStatDate(date);
        result.setVisits(0L);
        result.setOrderCount(0L);
        result.setOrderRevenue(BigDecimal.ZERO);
        result.setNewStudents(0L);
        result.setActiveUsers(0L);
        result.setTotalStudents(0L);
        return result;
    }

    public IPage<EduExam> pageExams(String keyword, Integer status, long pageNo, long pageSize) {
        Page<EduExam> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        return examMapper.selectPage(page, new LambdaQueryWrapper<EduExam>()
                .like(StringUtils.hasText(keyword), EduExam::getExamName, keyword)
                .eq(status != null, EduExam::getStatus, status).orderByDesc(EduExam::getCreateTime));
    }

    public IPage<EduExamQuestionBank> pageQuestionBank(String keyword, String questionType, Integer status,
            long pageNo, long pageSize) {
        Page<EduExamQuestionBank> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        return questionBankMapper.selectPage(page, new LambdaQueryWrapper<EduExamQuestionBank>()
                .like(StringUtils.hasText(keyword), EduExamQuestionBank::getStem, keyword)
                .eq(StringUtils.hasText(questionType), EduExamQuestionBank::getQuestionType, questionType)
                .eq(status != null, EduExamQuestionBank::getStatus, status).orderByDesc(EduExamQuestionBank::getCreateTime));
    }

    public EduExam saveExam(EduExam value) {
        require(value != null && StringUtils.hasText(value.getExamName()), "考试名称不能为空");
        LocalDateTime now = LocalDateTime.now();
        if (value.getId() == null) {
            value.setId(newId()); value.setTotalScore(defaultValue(value.getTotalScore(), BigDecimal.valueOf(100)));
            value.setPassScore(defaultValue(value.getPassScore(), BigDecimal.valueOf(60))); value.setDurationMinutes(defaultValue(value.getDurationMinutes(), 60));
            value.setStatus(defaultValue(value.getStatus(), ENABLED)); value.setCreateTime(now); value.setUpdateTime(now); value.setDelFlag(0); value.setVersion(0);
            examMapper.insert(value);
        } else { value.setUpdateTime(now); examMapper.updateById(value); }
        return value;
    }

    public EduExamQuestionBank saveQuestionBank(EduExamQuestionBank value) {
        require(value != null && StringUtils.hasText(value.getStem()), "题干不能为空");
        LocalDateTime now = LocalDateTime.now();
        if (value.getId() == null) {
            value.setId(newId()); value.setQuestionType(defaultText(value.getQuestionType(), "single")); value.setScore(defaultValue(value.getScore(), BigDecimal.ONE));
            value.setDifficulty(defaultValue(value.getDifficulty(), 2)); value.setStatus(defaultValue(value.getStatus(), ENABLED)); value.setCreateTime(now); value.setUpdateTime(now); value.setDelFlag(0); value.setVersion(0);
            questionBankMapper.insert(value);
        } else { value.setUpdateTime(now); questionBankMapper.updateById(value); }
        return value;
    }

    private Map<String, Object> categoryView(EduCategory item) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (item == null) return result;
        result.put("id", item.getId()); result.put("parentId", item.getParentId()); result.put("name", item.getCategoryName());
        result.put("categoryName", item.getCategoryName()); result.put("description", item.getDescription()); result.put("icon", item.getIcon());
        result.put("sort", item.getSortNum()); result.put("sortNum", item.getSortNum()); result.put("status", item.getStatus());
        result.put("createTime", item.getCreateTime()); return result;
    }

    private Map<String, Object> courseView(EduCourse item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId()); result.put("title", item.getCourseName()); result.put("courseName", item.getCourseName());
        result.put("cover", item.getCoverUrl()); result.put("coverUrl", item.getCoverUrl()); result.put("categoryId", item.getCategoryId());
        EduCategory category = categoryMapper.selectById(item.getCategoryId());
        result.put("categoryName", category == null ? "未分类" : category.getCategoryName());
        result.put("price", moneyCents(item.getPrice())); result.put("originalPrice", moneyCents(item.getOriginalPrice()));
        result.put("lessons", item.getLessonCount()); result.put("lessonCount", item.getLessonCount()); result.put("learners", item.getLearnerCount());
        result.put("learnerCount", item.getLearnerCount()); result.put("durationMinutes", item.getDurationMinutes()); result.put("rating", item.getRating());
        result.put("isFree", item.getIsFree()); result.put("status", item.getStatus()); result.put("description", item.getDescription() == null ? item.getShortDescription() : item.getDescription());
        result.put("shortDescription", item.getShortDescription()); result.put("createTime", item.getCreateTime());
        List<Map<String, Object>> teacherList = teachers(item.getId());
        if (!teacherList.isEmpty()) { result.put("teacherId", teacherList.get(0).get("id")); result.put("teacherName", teacherList.get(0).get("name")); }
        return result;
    }

    private Map<String, Object> teacherView(EduTeacher item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId()); result.put("userId", item.getUserId()); result.put("name", item.getTeacherName()); result.put("teacherName", item.getTeacherName());
        result.put("avatar", item.getAvatarUrl()); result.put("avatarUrl", item.getAvatarUrl()); result.put("title", item.getTitle()); result.put("introduction", item.getIntroduction());
        result.put("job", item.getTitle()); result.put("intro", item.getIntroduction());
        result.put("specialty", item.getSpecialty()); result.put("courses", item.getCourseCount()); result.put("courseCount", item.getCourseCount()); result.put("students", item.getStudentCount());
        result.put("studentCount", item.getStudentCount()); result.put("rating", item.getRating()); return result;
    }

    private Map<String, Object> catalogView(EduCourseCatalog item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId()); result.put("courseId", item.getCourseId()); result.put("parentId", item.getParentId()); result.put("catalogTitle", item.getCatalogTitle());
        result.put("title", item.getCatalogTitle()); result.put("index", item.getCatalogTitle()); result.put("catalogType", item.getCatalogType()); result.put("mediaId", item.getMediaId());
        result.put("mediaName", item.getMediaName()); result.put("durationSeconds", item.getDurationSeconds()); result.put("isFree", item.getIsFree());
        result.put("trailer", item.getTrailer()); result.put("sortNum", item.getSortNum()); return result;
    }

    private Map<String, Object> legacyCatalogView(EduCourseCatalog item) {
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

    private Map<String, Object> learningView(EduLearningRecord item) {
        Map<String, Object> result = new LinkedHashMap<>(); result.put("id", item.getId()); result.put("userId", item.getUserId()); result.put("courseId", item.getCourseId());
        result.put("catalogId", item.getCatalogId()); result.put("progress", item.getProgressPercent()); result.put("progressPercent", item.getProgressPercent()); result.put("progressSeconds", item.getProgressSeconds());
        result.put("learnDurationSeconds", item.getLearnDurationSeconds()); result.put("completedLessons", item.getCompletedLessons()); result.put("totalLessons", item.getTotalLessons()); result.put("status", item.getStatus()); result.put("lastLearnTime", item.getLastLearnTime());
        EduCourse course = courseMapper.selectById(item.getCourseId()); if (course != null) { result.put("courseName", course.getCourseName()); result.put("title", course.getCourseName()); result.put("cover", course.getCoverUrl()); }
        return result;
    }

    private Map<String, Object> planView(EduLearningPlan item) { Map<String, Object> result = new LinkedHashMap<>(); result.put("id", item.getId()); result.put("courseId", item.getCourseId()); result.put("planName", item.getPlanName()); result.put("courseName", Optional.ofNullable(courseMapper.selectById(item.getCourseId())).map(EduCourse::getCourseName).orElse("课程")); result.put("targetDate", item.getTargetDate()); result.put("planDate", item.getTargetDate()); result.put("dailyMinutes", item.getDailyMinutes()); result.put("progressPercent", item.getProgressPercent()); result.put("status", item.getStatus()); return result; }
    private Map<String, Object> questionView(EduQuestion item) { Map<String, Object> result = new LinkedHashMap<>(); result.put("id", item.getId()); result.put("userId", item.getUserId()); result.put("courseId", item.getCourseId()); result.put("title", item.getTitle()); result.put("content", item.getContent()); result.put("category", item.getCategory()); result.put("viewCount", item.getViewCount()); result.put("replyCount", item.getReplyCount()); result.put("replyTimes", item.getReplyCount()); result.put("likeCount", item.getLikeCount()); result.put("likedTimes", item.getLikeCount()); result.put("liked", false); result.put("userName", item.getUserId() != null && item.getUserId().equals(currentUserId()) ? currentUserName() : "学习者"); result.put("createTime", item.getCreateTime()); return result; }
    private Map<String, Object> replyView(EduReply item) { Map<String, Object> result = new LinkedHashMap<>(); result.put("id", item.getId()); result.put("questionId", item.getQuestionId()); result.put("answerId", item.getParentId()); result.put("parentId", item.getParentId()); result.put("userId", item.getUserId()); result.put("content", item.getContent()); result.put("liked", false); result.put("likedTimes", item.getLikeCount()); result.put("replyTimes", 0); result.put("userName", item.getUserId() != null && item.getUserId().equals(currentUserId()) ? currentUserName() : "学习者"); result.put("targetUserName", "提问者"); result.put("createTime", item.getCreateTime()); return result; }
    private Map<String, Object> noteView(EduNote item) { Long userId = currentUserId(); Map<String, Object> result = new LinkedHashMap<>(); result.put("id", item.getId()); result.put("userId", item.getUserId()); result.put("authorId", item.getUserId()); result.put("authorName", item.getUserId() != null && item.getUserId().equals(userId) ? currentUserName() : "学习者"); result.put("title", item.getTitle()); result.put("content", item.getContent()); result.put("courseId", item.getCourseId()); result.put("catalogId", item.getCatalogId()); result.put("visibility", item.getVisibility()); result.put("likedTimes", item.getLikeCount()); result.put("isGathered", noteCollectMapper.selectCount(new LambdaQueryWrapper<EduNoteCollect>().eq(EduNoteCollect::getNoteId, item.getId()).eq(EduNoteCollect::getUserId, userId)) > 0); result.put("liked", noteLikeMapper.selectCount(new LambdaQueryWrapper<EduNoteLike>().eq(EduNoteLike::getNoteId, item.getId()).eq(EduNoteLike::getUserId, userId)) > 0); result.put("createTime", item.getCreateTime()); return result; }
    private Map<String, Object> examView(EduExam item) { Map<String, Object> result = new LinkedHashMap<>(); result.put("id", item.getId()); result.put("courseId", item.getCourseId()); result.put("courseName", courseName(item.getCourseId())); result.put("examName", item.getExamName()); result.put("name", item.getExamName()); result.put("sectionName", item.getExamName()); result.put("description", item.getDescription()); result.put("totalScore", item.getTotalScore()); result.put("passScore", item.getPassScore()); result.put("durationMinutes", item.getDurationMinutes()); result.put("duration", item.getDurationMinutes() == null ? 0 : item.getDurationMinutes() * 60); result.put("status", item.getStatus()); return result; }
    private Map<String, Object> examRecordView(EduExamRecord item) { Map<String, Object> result = new LinkedHashMap<>(); result.put("id", item.getId()); result.put("examId", item.getExamId()); result.put("score", item.getScore()); result.put("totalScore", item.getTotalScore()); result.put("correctCount", item.getCorrectCount()); result.put("questionCount", item.getQuestionCount()); result.put("status", item.getStatus()); result.put("statusName", Integer.valueOf(1).equals(item.getStatus()) ? "通过" : Integer.valueOf(2).equals(item.getStatus()) ? "未通过" : "进行中"); result.put("startedAt", item.getStartedAt()); result.put("startTime", item.getStartedAt()); result.put("submittedAt", item.getSubmittedAt()); result.put("endTime", item.getSubmittedAt()); result.put("commitTime", item.getSubmittedAt()); result.put("duration", durationSeconds(item.getStartedAt(), item.getSubmittedAt())); EduExam exam = examMapper.selectById(item.getExamId()); if (exam != null) { result.put("examName", exam.getExamName()); result.put("sectionName", exam.getExamName()); result.put("courseId", exam.getCourseId()); result.put("courseName", courseName(exam.getCourseId())); } return result; }
    private Map<String, Object> questionBankView(EduExamQuestionBank item) { Map<String, Object> result = new LinkedHashMap<>(); result.put("id", item.getId()); result.put("questionType", item.getQuestionType()); result.put("type", item.getQuestionType()); result.put("stem", item.getStem()); result.put("title", item.getStem()); result.put("options", item.getOptionsJson()); result.put("optionsJson", item.getOptionsJson()); result.put("correctAnswer", item.getCorrectAnswer()); result.put("analysis", item.getAnalysis()); result.put("score", item.getScore()); result.put("difficulty", item.getDifficulty()); result.put("categoryId", item.getCategoryId()); return result; }

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

    private EduCourse requireCourse(Long id) { EduCourse value = courseMapper.selectById(id); if (value == null) throw new ServiceException("课程不存在"); return value; }
    private EduQuestion requireQuestion(Long id) { EduQuestion value = questionMapper.selectById(id); if (value == null) throw new ServiceException("问题不存在"); return value; }
    private EduNote requireNote(Long id) { EduNote value = noteMapper.selectById(id); if (value == null) throw new ServiceException("笔记不存在"); return value; }
    private void require(boolean condition, String message) { if (!condition) throw new ServiceException(message); }
    private long newId() { return IdWorker.getId(); }
    private Long currentUserId() { Long value = SecurityUtils.getUserId(); return value == null || value < 1 ? 1L : value; }
    private String currentUserName() { return StringUtils.hasText(SecurityUtils.getUsername()) ? SecurityUtils.getUsername() : "学习者"; }
    private int courseLessonCount(Long courseId) { return Optional.ofNullable(courseMapper.selectById(courseId)).map(EduCourse::getLessonCount).orElse(0); }
    private String courseName(Long courseId) { return Optional.ofNullable(courseId).map(courseMapper::selectById).map(EduCourse::getCourseName).orElse(""); }
    private int examQuestionCount(Long examId) { int count = examQuestionMapper.selectCount(new LambdaQueryWrapper<EduExamQuestion>().eq(EduExamQuestion::getExamId, examId)).intValue(); return count > 0 ? count : questionBankMapper.selectCount(new LambdaQueryWrapper<EduExamQuestionBank>().eq(EduExamQuestionBank::getStatus, ENABLED)).intValue(); }
    private int totalPoints(Long userId) { return pointsMapper.selectList(new LambdaQueryWrapper<EduPointsLedger>().eq(EduPointsLedger::getUserId, userId)).stream().mapToInt(item -> defaultValue(item.getChangeAmount(), 0)).sum(); }
    /** 每个用户只取按发生时间排序后的最新流水，避免历史最高余额被误当成当前余额。 */
    private Map<Long, Integer> latestPointsByUser() {
        Map<Long, Integer> result = new LinkedHashMap<>();
        pointsMapper.selectList(new LambdaQueryWrapper<EduPointsLedger>()
                .orderByDesc(EduPointsLedger::getCreateTime).orderByDesc(EduPointsLedger::getId))
                .forEach(item -> result.putIfAbsent(item.getUserId(), defaultValue(item.getBalanceAfter(), 0)));
        return result;
    }
    private int pointsRank(Long userId) {
        Map<Long, Integer> totals = latestPointsByUser();
        int current = totals.getOrDefault(userId, totalPoints(userId));
        return 1 + (int) totals.values().stream().filter(value -> value > current).count();
    }
    private boolean sameAnswer(String expected, String actual, String questionType) {
        if (!StringUtils.hasText(expected) || !StringUtils.hasText(actual)) return false;
        return normalizeAnswer(expected, questionType).equalsIgnoreCase(normalizeAnswer(actual, questionType));
    }
    /** 将新题库的类型映射为旧答题页使用的数字题型。 */
    private int legacyQuestionType(String value) {
        if (!StringUtils.hasText(value)) return 1;
        String normalized = normalizeQuestionType(value);
        return switch (normalized) {
            case "single" -> 1;
            case "multiple" -> 2;
            case "judge" -> 4;
            case "blank", "fill", "essay" -> 5;
            default -> intValue(value, 5);
        };
    }
    /** 兼容旧页面提交的 1/2/true 等选项值，并统一保存为 A/B/C 格式。 */
    private String normalizeAnswer(String value, String questionType) {
        if (!StringUtils.hasText(value)) return "";
        String type = normalizeQuestionType(questionType);
        String raw = value.trim();
        if (raw.startsWith("[") && raw.endsWith("]")) {
            try {
                List<?> values = objectMapper.readValue(raw, List.class);
                raw = values.stream().map(String::valueOf).collect(Collectors.joining(","));
            } catch (Exception ignored) { }
        }
        if ("judge".equals(type)) {
            if ("true".equalsIgnoreCase(raw) || "正确".equals(raw) || "1".equals(raw) || "A".equalsIgnoreCase(raw)) return "A";
            if ("false".equalsIgnoreCase(raw) || "错误".equals(raw) || "0".equals(raw) || "2".equals(raw) || "B".equalsIgnoreCase(raw)) return "B";
            return raw.replaceAll("[\\s,，、]", "").toUpperCase();
        }
        if (!"single".equals(type) && !"multiple".equals(type)) return raw;
        String[] tokens = raw.split("[,，、;；\\s]+");
        if (tokens.length == 1 && "multiple".equals(type) && tokens[0].length() > 1
                && tokens[0].matches("[A-Za-z0-9]+")) {
            tokens = tokens[0].split("");
        }
        List<String> normalized = new ArrayList<>();
        for (String token : tokens) {
            if (!StringUtils.hasText(token)) continue;
            String item = token.trim();
            if (item.matches("\\d+")) {
                int index = intValue(item, -1);
                if (index >= 1 && index <= 26) item = String.valueOf((char) ('A' + index - 1));
            } else {
                item = item.toUpperCase();
            }
            normalized.add(item);
            if ("single".equals(type)) break;
        }
        if ("multiple".equals(type)) {
            return normalized.stream().distinct().sorted().collect(Collectors.joining());
        }
        return normalized.isEmpty() ? "" : normalized.get(0);
    }
    private BigDecimal moneyCents(BigDecimal value) { return defaultValue(value, BigDecimal.ZERO).movePointRight(2).setScale(0, RoundingMode.HALF_UP); }
    private Map<String, Object> pageView(long total, List<?> list) { Map<String, Object> result = new LinkedHashMap<>(); result.put("total", total); result.put("list", list); return result; }
    private long safePage(long value) { return value < 1 ? 1 : Math.min(value, 100000); }
    private long safeSize(long value) { return value < 1 ? 10 : Math.min(value, 200); }
    private long number(Map<String, ?> params, String key, long fallback) { Object value = params == null ? null : params.get(key); if (value == null) return fallback; try { return Long.parseLong(String.valueOf(value)); } catch (NumberFormatException ignored) { return fallback; } }
    private Long longValue(Object value) { if (value == null || "".equals(String.valueOf(value))) return null; try { return Long.valueOf(String.valueOf(value)); } catch (NumberFormatException ignored) { return null; } }
    private Long firstLong(Object value) {
        if (value instanceof List<?> list) return list.isEmpty() ? null : firstLong(list.get(list.size() - 1));
        if (value != null && value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            return array.length == 0 ? null : firstLong(array[array.length - 1]);
        }
        return longValue(value);
    }
    private int intValue(Object value, int fallback) { Long parsed = longValue(value); return parsed == null ? fallback : parsed.intValue(); }
    private BigDecimal decimalValue(Object value, BigDecimal fallback) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) return fallback;
        try { return new BigDecimal(String.valueOf(value)); } catch (NumberFormatException ignored) { return fallback; }
    }
    private BigDecimal moneyYuan(Object value, BigDecimal fallback) {
        BigDecimal cents = decimalValue(value, null);
        return cents == null ? fallback : cents.movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
    }
    private int durationSeconds(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number number) return number.intValue();
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return fallback;
        if (text.contains(":")) {
            String[] parts = text.split(":");
            try {
                int seconds = Integer.parseInt(parts[parts.length - 1]);
                int minutes = parts.length > 1 ? Integer.parseInt(parts[parts.length - 2]) : 0;
                int hours = parts.length > 2 ? Integer.parseInt(parts[parts.length - 3]) : 0;
                return hours * 3600 + minutes * 60 + seconds;
            } catch (NumberFormatException ignored) { return fallback; }
        }
        try { return Integer.parseInt(text); } catch (NumberFormatException ignored) { return fallback; }
    }
    private int durationSeconds(LocalDateTime startedAt, LocalDateTime submittedAt) {
        if (startedAt == null || submittedAt == null || submittedAt.isBefore(startedAt)) return 0;
        long seconds = java.time.Duration.between(startedAt, submittedAt).getSeconds();
        return seconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seconds;
    }
    private int catalogType(Map<String, ?> source, int fallback) {
        int value = intValue(source.get("catalogType"), intValue(source.get("type"), fallback));
        return value < 1 ? fallback : value;
    }
    private Object unwrapData(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            if (map.containsKey("datas")) return map.get("datas");
            if (map.containsKey("data")) return map.get("data");
            if (map.containsKey("list")) return map.get("list");
        }
        return payload;
    }
    private List<Map<String, ?>> mapList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        List<Map<String, ?>> result = new ArrayList<>();
        for (Object item : list) if (item instanceof Map<?, ?> map) result.add(castMap(map));
        return result;
    }
    private List<?> listValues(Object value) {
        if (value instanceof List<?> list) return list;
        if (value == null) return List.of();
        String text = String.valueOf(value);
        if (!StringUtils.hasText(text)) return List.of();
        if (text.startsWith("[") && text.endsWith("]")) {
            try { return objectMapper.readValue(text, List.class); } catch (Exception ignored) { }
        }
        return Arrays.stream(text.split(",|，|、")).map(String::trim).filter(StringUtils::hasText).toList();
    }
    private boolean isQuestionBankQuery(Map<String, ?> params) {
        if (params == null) return false;
        return params.containsKey("type") || params.containsKey("questionType") || params.containsKey("categoryId")
                || bool(params.get("isBank"));
    }
    private String normalizeQuestionType(String value) {
        if (!StringUtils.hasText(value)) return value;
        return switch (value) {
            case "single", "single_choice", "1" -> "single";
            case "multiple", "multiple_choice", "2" -> "multiple";
            case "judge", "判断", "3" -> "judge";
            case "fill", "blank", "4" -> "blank";
            default -> value;
        };
    }
    private boolean bool(Object value) { return value instanceof Boolean b ? b : "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value)); }
    private String text(Object value) { return value == null ? null : String.valueOf(value); }
    private String defaultText(Object value, String fallback) { return value == null || !StringUtils.hasText(String.valueOf(value)) ? fallback : String.valueOf(value); }
    private String defaultText(String value, String fallback) { return StringUtils.hasText(value) ? value : fallback; }
    private <T> T defaultValue(T value, T fallback) { return value == null ? fallback : value; }
    @SuppressWarnings("unchecked")
    private Map<String, ?> castMap(Map<?, ?> value) {
        Map<String, Object> result = new LinkedHashMap<>();
        value.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }
}
