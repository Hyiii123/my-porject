package com.share.education.ai.rag.provider;

import com.share.education.ai.model.CourseDocItem;
import java.util.List;
import java.util.Map;

/**
 * 课程文档与密集向量检索 SPI 数据提供者契约。
 * 解耦 share-agent 领域模块与具体微服务业务数据库 (MyBatis/MySQL)。
 */
public interface ICourseDocumentProvider {

    /**
     * 加载全量或当前处于上架状态的生产课程文档项
     */
    List<CourseDocItem> loadAllActiveCourses();

    /**
     * 针对业务库执行密集语义/模糊候选检索 (Dense/Lexical Candidates)
     */
    List<Map<String, Object>> searchDenseCandidates(String query, int limit);
}