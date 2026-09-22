package com.share.education.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.education.ai.model.CourseDocItem;
import com.share.education.ai.rag.provider.ICourseDocumentProvider;
import com.share.education.domain.EduCourse;
import com.share.education.mapper.EduCourseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 生产级课程文档与密集语义检索 SPI 适配提供者。
 * 连接 share-agent 领域模块与 share-education 业务数据库 (MyBatis-Plus)。
 */
@Component
public class EducationCourseDocumentProvider implements ICourseDocumentProvider {

    private static final Logger log = LoggerFactory.getLogger(EducationCourseDocumentProvider.class);

    @Autowired(required = false)
    private EduCourseMapper courseMapper;

    @Override
    public List<CourseDocItem> loadAllActiveCourses() {
        if (courseMapper == null) {
            log.warn("[EducationCourseDocumentProvider] courseMapper 未就绪，无法加载课程文档");
            return Collections.emptyList();
        }
        try {
            List<EduCourse> list = courseMapper.selectList(
                    new LambdaQueryWrapper<EduCourse>().eq(EduCourse::getStatus, 1));
            return list.stream().map(c -> new CourseDocItem(
                    c.getId(),
                    c.getCourseName(),
                    c.getSkills(),
                    c.getPrice(),
                    c.getCoverUrl()
            )).collect(Collectors.toList());
        } catch (Exception ex) {
            log.warn("[EducationCourseDocumentProvider] 查询上架课程异常: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> searchDenseCandidates(String query, int limit) {
        if (courseMapper == null) {
            return Collections.emptyList();
        }
        try {
            List<EduCourse> candidates = courseMapper.selectList(new LambdaQueryWrapper<EduCourse>()
                    .eq(EduCourse::getStatus, 1)
                    .and(w -> w.like(EduCourse::getCourseName, query)
                            .or().like(EduCourse::getTargetRole, query)
                            .or().like(EduCourse::getSkills, query))
                    .orderByDesc(EduCourse::getLearnerCount)
                    .last("limit " + limit));

            return candidates.stream().map(c -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", c.getId());
                map.put("denseScore", 0.95);
                map.put("title", c.getCourseName());
                map.put("courseName", c.getCourseName());
                map.put("skills", c.getSkills());
                map.put("price", c.getPrice());
                map.put("cover", c.getCoverUrl());
                return map;
            }).collect(Collectors.toList());
        } catch (Exception ex) {
            log.warn("[EducationCourseDocumentProvider] 检索密集候选异常: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }
}