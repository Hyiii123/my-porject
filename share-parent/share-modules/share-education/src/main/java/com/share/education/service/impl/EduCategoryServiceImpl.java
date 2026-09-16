package com.share.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.share.common.core.exception.ServiceException;
import com.share.education.domain.EduBanner;
import com.share.education.domain.EduCategory;
import com.share.education.domain.EduCourse;
import com.share.education.domain.EduInterest;
import com.share.education.mapper.EduBannerMapper;
import com.share.education.mapper.EduCategoryMapper;
import com.share.education.mapper.EduCourseMapper;
import com.share.education.mapper.EduInterestMapper;
import com.share.education.service.IEduCategoryService;
import com.share.education.service.IEduCourseService;
import com.share.education.service.support.EduUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

import static com.share.education.service.support.EduUtils.*;

/**
 * 课程分类、轮播图与兴趣领域服务实现
 */
@Slf4j
@Service
@Primary
public class EduCategoryServiceImpl implements IEduCategoryService {

    private static final int ENABLED = 1;

    private final EduCategoryMapper categoryMapper;
    private final EduBannerMapper bannerMapper;
    private final EduInterestMapper interestMapper;
    private final EduCourseMapper courseMapper;
    private final IEduCourseService courseService;

    public EduCategoryServiceImpl(EduCategoryMapper categoryMapper,
                                  EduBannerMapper bannerMapper,
                                  EduInterestMapper interestMapper,
                                  EduCourseMapper courseMapper,
                                  @Lazy IEduCourseService courseService) {
        this.categoryMapper = categoryMapper;
        this.bannerMapper = bannerMapper;
        this.interestMapper = interestMapper;
        this.courseMapper = courseMapper;
        this.courseService = courseService;
    }

    @Override
    public List<Map<String, Object>> listCategories(boolean includeDisabled) {
        LambdaQueryWrapper<EduCategory> wrapper = new LambdaQueryWrapper<EduCategory>()
                .eq(!includeDisabled, EduCategory::getStatus, ENABLED)
                .orderByAsc(EduCategory::getSortNum).orderByAsc(EduCategory::getId);
        List<EduCategory> list = categoryMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> countMap = new HashMap<>();
        try {
            List<Map<String, Object>> counts = courseMapper.selectMaps(new QueryWrapper<EduCourse>()
                    .select("category_id as categoryId, count(*) as total")
                    .groupBy("category_id"));
            if (counts != null) {
                for (Map<String, Object> m : counts) {
                    if (m != null && m.get("categoryId") != null) {
                        Long catId = Long.valueOf(m.get("categoryId").toString());
                        int total = m.get("total") != null ? Integer.parseInt(m.get("total").toString()) : 0;
                        countMap.put(catId, total);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("统计分类课程数量异常: {}", e.getMessage());
        }

        return list.stream().map(cat -> {
            Map<String, Object> view = categoryView(cat);
            int count = countMap.getOrDefault(cat.getId(), 0);
            view.put("courseCount", count);
            view.put("courseNum", count);
            view.put("courses", count);
            return view;
        }).toList();
    }

    @Override
    public Map<String, Object> category(Long id) {
        EduCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new ServiceException("课程分类不存在");
        }
        Map<String, Object> view = categoryView(category);
        int count = courseMapper.selectCount(new LambdaQueryWrapper<EduCourse>().eq(EduCourse::getCategoryId, id)).intValue();
        view.put("courseCount", count);
        view.put("courseNum", count);
        view.put("courses", count);
        return view;
    }

    @Override
    public IPage<EduCategory> pageCategories(String keyword, Integer status, long pageNo, long pageSize) {
        Page<EduCategory> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        return categoryMapper.selectPage(page, new LambdaQueryWrapper<EduCategory>()
                .like(StringUtils.hasText(keyword), EduCategory::getCategoryName, keyword)
                .eq(status != null, EduCategory::getStatus, status)
                .orderByAsc(EduCategory::getSortNum).orderByAsc(EduCategory::getId));
    }

    @Override
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

    @Override
    public void removeCategories(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            categoryMapper.deleteBatchIds(ids);
        }
    }

    @Override
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

    @Override
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

    @Override
    @Transactional
    public void updateLegacyCategoryStatus(Map<String, ?> body) {
        Long id = longValue(body == null ? null : body.get("id"));
        EduCategory value = id == null ? null : categoryMapper.selectById(id);
        require(value != null, "课程分类不存在");
        value.setStatus(defaultValue(intValue(body == null ? null : body.get("status"), value.getStatus()), value.getStatus()));
        value.setUpdateTime(LocalDateTime.now());
        categoryMapper.updateById(value);
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public List<Map<String, Object>> interestCourses(Long categoryId) {
        return courseMapper.selectList(new LambdaQueryWrapper<EduCourse>()
                .eq(EduCourse::getCategoryId, categoryId).eq(EduCourse::getStatus, ENABLED)
                .orderByDesc(EduCourse::getPublishTime).last("limit 20"))
                .stream().map(courseService::courseView).toList();
    }

    @Override
    public Map<String, Object> categoryView(EduCategory item) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (item == null) return result;
        result.put("id", item.getId());
        result.put("parentId", item.getParentId());
        result.put("name", item.getCategoryName());
        result.put("categoryName", item.getCategoryName());
        result.put("description", item.getDescription());
        result.put("icon", item.getIcon());
        result.put("sort", item.getSortNum());
        result.put("sortNum", item.getSortNum());
        result.put("status", item.getStatus());
        result.put("createTime", item.getCreateTime());
        return result;
    }
}
