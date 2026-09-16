package com.share.education.service;

import com.share.education.domain.EduDashboardDaily;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计、每日签到与积分账本领域接口
 */
public interface IEduDashboardService {

    Map<String, Object> signInfo();

    Map<String, Object> sign();

    Map<String, Object> pointsToday();

    List<Map<String, Object>> pointsBoard(Map<String, ?> params);

    Map<String, Object> statistics();

    List<EduDashboardDaily> dashboardDaily(int days);

    EduDashboardDaily dashboardToday();

    EduDashboardDaily dashboardPrevious(EduDashboardDaily current);

    int totalPoints(Long userId);

    int pointsRank(Long userId);
}
