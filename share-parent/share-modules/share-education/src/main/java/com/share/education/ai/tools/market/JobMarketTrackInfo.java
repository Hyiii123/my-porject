package com.share.education.ai.tools.market;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 产业前沿岗位招聘行情与技能需求信息 DTO。
 */
@Data
public class JobMarketTrackInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 赛道编码 */
    private String trackCode;

    /** 赛道名称 */
    private String trackName;

    /** 市场热度指数 (0~100) */
    private Integer demandIndex;

    /** 招聘趋势: 爆发增长, 稳步上升, 供需平衡 */
    private String demandTrend;

    /** 平均薪酬区间 */
    private String avgSalaryRange;

    /** 核心热门技术框架与高频词 */
    private List<String> hotKeywords;

    /** 企业硬核胜任力要求 */
    private List<String> coreCompetencies;

    /** 推荐布鲁姆认知深度 */
    private String targetBloomLevel;

    /** 重点推荐实战项目类型 */
    private String recommendedProjectType;

    public JobMarketTrackInfo() {}

    public JobMarketTrackInfo(String trackCode, String trackName, Integer demandIndex, String demandTrend,
                              String avgSalaryRange, List<String> hotKeywords, List<String> coreCompetencies,
                              String targetBloomLevel, String recommendedProjectType) {
        this.trackCode = trackCode;
        this.trackName = trackName;
        this.demandIndex = demandIndex;
        this.demandTrend = demandTrend;
        this.avgSalaryRange = avgSalaryRange;
        this.hotKeywords = hotKeywords;
        this.coreCompetencies = coreCompetencies;
        this.targetBloomLevel = targetBloomLevel;
        this.recommendedProjectType = recommendedProjectType;
    }
}
