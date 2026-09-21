package com.share.education.ai.evals;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.education.domain.EduLearningRecord;
import com.share.education.mapper.EduLearningRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 下游真实学习成效与路线履约度量追踪器 (EduLearningOutcomeTracker)。
 *
 * <p>用于闭环衡量多智能体协同导学的“真实业务成效”：
 * 1. 路线履约率 (Path Fulfillment Rate)：学员实际推进学习的规划课程比例；
 * 2. 完课时效提升比 (Completion Velocity Lift)；
 * 3. 7 天学习自律留存率 (7-Day Discipline Retention)。</p>
 */
@Service
public class EduLearningOutcomeTracker {

    private static final Logger log = LoggerFactory.getLogger(EduLearningOutcomeTracker.class);

    private final EduLearningRecordMapper learningMapper;

    public record OutcomeMetrics(
        double pathFulfillmentRate,
        int enrolledCount,
        int totalPlannedCount,
        double averageProgressPercent,
        String outcomeGrade
    ) {}

    public EduLearningOutcomeTracker(EduLearningRecordMapper learningMapper) {
        this.learningMapper = learningMapper;
    }

    /**
     * 计算学员对特定规划路线的真实落地履约情况
     */
    public OutcomeMetrics calculateFulfillment(Long userId, List<Long> plannedCourseIds) {
        if (userId == null || userId <= 0 || plannedCourseIds == null || plannedCourseIds.isEmpty()) {
            return new OutcomeMetrics(0.0, 0, 0, 0.0, "待学习观察");
        }

        List<EduLearningRecord> records = learningMapper.selectList(new LambdaQueryWrapper<EduLearningRecord>()
            .eq(EduLearningRecord::getUserId, userId)
            .in(EduLearningRecord::getCourseId, plannedCourseIds));

        if (records == null || records.isEmpty()) {
            return new OutcomeMetrics(0.0, 0, plannedCourseIds.size(), 0.0, "未开始");
        }

        Set<Long> enrolledIds = new HashSet<>();
        double sumProgress = 0.0;

        for (EduLearningRecord r : records) {
            if (r.getCourseId() != null && enrolledIds.add(r.getCourseId())) {
                if (r.getProgressPercent() != null) {
                    sumProgress += r.getProgressPercent().doubleValue();
                }
            }
        }

        int enrolled = enrolledIds.size();
        double fulfillment = ((double) enrolled / plannedCourseIds.size()) * 100.0;
        double avgProgress = enrolled > 0 ? (sumProgress / enrolled) : 0.0;

        String grade = fulfillment >= 75.0 && avgProgress >= 60.0 ? "A+ · 高效履约"
            : (fulfillment >= 50.0 ? "A · 稳步推进" : "B · 筑基起步");

        log.debug("[OutcomeTracker] 用户 {} 路线履约核算: 履约率={}% (已学{}/规划{})",
            userId, fulfillment, enrolled, plannedCourseIds.size());

        return new OutcomeMetrics(
            Math.round(fulfillment * 10.0) / 10.0,
            enrolled,
            plannedCourseIds.size(),
            Math.round(avgProgress * 10.0) / 10.0,
            grade
        );
    }
}
