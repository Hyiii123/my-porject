package com.share.education.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.share.education.domain.EduCategory;
import com.share.education.domain.EduInterest;

import java.util.List;
import java.util.Map;

/**
 * 课程分类、轮播图与兴趣偏好领域接口
 */
public interface IEduCategoryService {

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

    Map<String, Object> categoryView(EduCategory item);
}
