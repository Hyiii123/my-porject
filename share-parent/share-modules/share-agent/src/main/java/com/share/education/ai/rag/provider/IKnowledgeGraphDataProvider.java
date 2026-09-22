package com.share.education.ai.rag.provider;

import java.util.List;
import java.util.Set;

/**
 * 知识图谱先修依赖网络与学情掌握度 SPI 数据提供者契约。
 */
public interface IKnowledgeGraphDataProvider {

    /**
     * 根据当前知识概念查询其直接先修依赖概念列表
     */
    List<String> findPrerequisites(String concept);

    /**
     * 查询学员已熟练掌握或已学完的高阶/先修课程 ID 集合
     */
    Set<Long> findMasteredCourseIds(Long userId);
}