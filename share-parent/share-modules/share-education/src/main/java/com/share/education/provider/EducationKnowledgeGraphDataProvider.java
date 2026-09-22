package com.share.education.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.education.ai.rag.provider.IKnowledgeGraphDataProvider;
import com.share.education.domain.EduKnowledgePrerequisite;
import com.share.education.domain.EduLearningRecord;
import com.share.education.mapper.EduKnowledgePrerequisiteMapper;
import com.share.education.mapper.EduLearningRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 生产级知识图谱与学情记录 SPI 适配提供者。
 * 连接 share-agent 领域模块与 share-education 先修拓扑与学情数据库。
 */
@Component
public class EducationKnowledgeGraphDataProvider implements IKnowledgeGraphDataProvider {

    private static final Logger log = LoggerFactory.getLogger(EducationKnowledgeGraphDataProvider.class);

    @Autowired(required = false)
    private EduKnowledgePrerequisiteMapper prerequisiteMapper;

    @Autowired(required = false)
    private EduLearningRecordMapper learningRecordMapper;

    @Override
    public List<String> findPrerequisites(String concept) {
        if (prerequisiteMapper == null || !StringUtils.hasText(concept)) {
            return Collections.emptyList();
        }
        try {
            List<EduKnowledgePrerequisite> edges = prerequisiteMapper.selectList(
                    new LambdaQueryWrapper<EduKnowledgePrerequisite>()
                            .like(EduKnowledgePrerequisite::getConceptName, concept)
                            .or()
                            .apply("LOCATE(concept_name, {0}) > 0", concept));

            return edges.stream()
                    .map(EduKnowledgePrerequisite::getPrerequisiteConcept)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.warn("[EducationKnowledgeGraphDataProvider] 查询先修依赖边异常: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Set<Long> findMasteredCourseIds(Long userId) {
        if (learningRecordMapper == null || userId == null) {
            return Collections.emptySet();
        }
        try {
            List<EduLearningRecord> records = learningRecordMapper.selectList(
                    new LambdaQueryWrapper<EduLearningRecord>()
                            .eq(EduLearningRecord::getUserId, userId)
                            .ge(EduLearningRecord::getProgressPercent, 80));

            return records.stream()
                    .map(EduLearningRecord::getCourseId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (Exception ex) {
            log.warn("[EducationKnowledgeGraphDataProvider] 查询学员完课记录异常: {}", ex.getMessage());
            return Collections.emptySet();
        }
    }
}