package com.share.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.common.redis.service.RedisService;
import com.share.education.domain.*;
import com.share.education.mapper.*;
import com.share.education.service.IEduDashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.share.education.service.support.EduUtils.*;

/**
 * 仪表盘统计、每日签到与积分账本领域服务实现
 */
@Slf4j
@Service
@Primary
public class EduDashboardServiceImpl implements IEduDashboardService {

    private static final int ENABLED = 1;
    private static final int SIGN_POINTS = 10;

    private final EduSignRecordMapper signMapper;
    private final EduPointsLedgerMapper pointsMapper;
    private final EduDashboardDailyMapper dashboardDailyMapper;
    private final EduCourseMapper courseMapper;
    private final EduCategoryMapper categoryMapper;
    private final EduTeacherMapper teacherMapper;
    private final EduQuestionMapper questionMapper;
    private final EduNoteMapper noteMapper;
    private final EduExamMapper examMapper;
    private final RedisService redisService;

    public EduDashboardServiceImpl(EduSignRecordMapper signMapper,
                                  EduPointsLedgerMapper pointsMapper,
                                  EduDashboardDailyMapper dashboardDailyMapper,
                                  EduCourseMapper courseMapper,
                                  EduCategoryMapper categoryMapper,
                                  EduTeacherMapper teacherMapper,
                                  EduQuestionMapper questionMapper,
                                  EduNoteMapper noteMapper,
                                  EduExamMapper examMapper,
                                  RedisService redisService) {
        this.signMapper = signMapper;
        this.pointsMapper = pointsMapper;
        this.dashboardDailyMapper = dashboardDailyMapper;
        this.courseMapper = courseMapper;
        this.categoryMapper = categoryMapper;
        this.teacherMapper = teacherMapper;
        this.questionMapper = questionMapper;
        this.noteMapper = noteMapper;
        this.examMapper = examMapper;
        this.redisService = redisService;
    }

    @Override
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
            row.put("date", item.getSignDate());
            row.put("points", item.getPoints());
            row.put("continuousDays", item.getContinuousDays());
            return row;
        }).toList());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> sign() {
        Long userId = currentUserId();
        LocalDate today = LocalDate.now();
        String lockKey = "education:sign:lock:" + userId + ":" + today;
        boolean acquired = Boolean.TRUE.equals(redisService.setCacheObjectIfAbsent(lockKey, "1", 10L, TimeUnit.SECONDS));
        if (!acquired) {
            return pointsToday();
        }
        try {
            EduSignRecord old = signMapper.selectOne(new LambdaQueryWrapper<EduSignRecord>()
                    .eq(EduSignRecord::getUserId, userId).eq(EduSignRecord::getSignDate, today));
            if (old != null) return pointsToday();
            EduSignRecord previous = signMapper.selectOne(new LambdaQueryWrapper<EduSignRecord>()
                    .eq(EduSignRecord::getUserId, userId).orderByDesc(EduSignRecord::getSignDate).last("limit 1"));
            int continuous = previous != null && today.minusDays(1).equals(previous.getSignDate())
                    ? defaultValue(previous.getContinuousDays(), 0) + 1 : 1;
            EduSignRecord record = new EduSignRecord();
            record.setId(newId());
            record.setUserId(userId);
            record.setSignDate(today);
            record.setPoints(SIGN_POINTS);
            record.setContinuousDays(continuous);
            record.setCreateTime(LocalDateTime.now());
            signMapper.insert(record);
            int balance = totalPoints(userId) + SIGN_POINTS;
            EduPointsLedger ledger = new EduPointsLedger();
            ledger.setId(newId());
            ledger.setUserId(userId);
            ledger.setChangeAmount(SIGN_POINTS);
            ledger.setBalanceAfter(balance);
            ledger.setSourceType("sign");
            ledger.setBizId(today.toString());
            ledger.setRemark("每日签到");
            ledger.setCreateTime(LocalDateTime.now());
            pointsMapper.insert(ledger);
            return pointsToday();
        } finally {
            redisService.deleteObject(lockKey);
        }
    }

    @Override
    public Map<String, Object> pointsToday() {
        Long userId = currentUserId();
        Map<String, Object> result = new LinkedHashMap<>();
        EduSignRecord today = signMapper.selectOne(new LambdaQueryWrapper<EduSignRecord>()
                .eq(EduSignRecord::getUserId, userId).eq(EduSignRecord::getSignDate, LocalDate.now()));
        result.put("todayPoints", today == null ? 0 : today.getPoints());
        result.put("totalPoints", totalPoints(userId));
        result.put("rank", pointsRank(userId));
        return result;
    }

    @Override
    public List<Map<String, Object>> pointsBoard(Map<String, ?> params) {
        List<Map<String, Object>> rows = pointsMapper.selectPointsLeaderboard(50);
        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> item : rows) {
            Long uid = longValue(item.get("userId"));
            int points = intValue(item.get("points"), 0);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", uid);
            row.put("userName", uid != null && uid.equals(currentUserId()) ? currentUserName() : "学习者" + uid);
            row.put("points", points);
            row.put("rank", rank++);
            result.add(row);
        }
        if (result.isEmpty()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", currentUserId());
            row.put("userName", currentUserName());
            row.put("points", totalPoints(currentUserId()));
            row.put("rank", 1);
            result.add(row);
        }
        return result;
    }

    @Override
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

    @Override
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

    @Override
    public EduDashboardDaily dashboardToday() {
        EduDashboardDaily today = dashboardDailyMapper.selectOne(new LambdaQueryWrapper<EduDashboardDaily>()
                .eq(EduDashboardDaily::getStatDate, LocalDate.now()));
        if (today != null) return today;
        return dashboardDailyMapper.selectOne(new LambdaQueryWrapper<EduDashboardDaily>()
                .orderByDesc(EduDashboardDaily::getStatDate).last("limit 1"));
    }

    @Override
    public EduDashboardDaily dashboardPrevious(EduDashboardDaily current) {
        if (current == null || current.getStatDate() == null) return null;
        return dashboardDailyMapper.selectOne(new LambdaQueryWrapper<EduDashboardDaily>()
                .eq(EduDashboardDaily::getStatDate, current.getStatDate().minusDays(1L)));
    }

    @Override
    public int totalPoints(Long userId) {
        if (userId == null) return 0;
        Integer pts = pointsMapper.selectTotalPointsByUserId(userId);
        return pts == null ? 0 : pts;
    }

    @Override
    public int pointsRank(Long userId) {
        if (userId == null) return 1;
        Integer rank = pointsMapper.selectUserRank(userId);
        return rank == null ? 1 : rank;
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
}
