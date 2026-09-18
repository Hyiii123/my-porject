package com.share.education.service;

import com.share.education.domain.EduCourse;
import com.share.education.domain.EduDisciplineTaxonomy;

import java.util.List;

/**
 * 学科领域技术字典与知识图谱前驱依赖动态服务接口。
 * 替代 Java 代码中手写的黑白名单与 PREREQ_RULES。
 */
public interface IDisciplineTaxonomyService {

    /**
     * 根据目标角色动态解析学科领域分类元数据
     */
    EduDisciplineTaxonomy resolveTaxonomy(String intendedRole);

    /**
     * 校验课程是否符合该目标角色的领域边界
     */
    boolean isCourseAllowedForDomain(EduCourse course, String intendedRole);

    /**
     * 根据课程名称与技能标签，动态检索知识图谱中的先修前驱依赖
     */
    List<String> getPrerequisitesForCourse(String courseName, String skills);

    /**
     * 刷新领域元数据与知识图谱缓存
     */
    void refreshCache();
}
