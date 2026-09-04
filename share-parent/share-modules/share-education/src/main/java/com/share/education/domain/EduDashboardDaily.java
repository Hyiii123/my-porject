package com.share.education.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 教育工作台按日汇总指标。
 *
 * <p>看板只读取汇总表，不把演示数字写进页面；实际生产环境可以由访问埋点、订单事件和
 * 学员注册事件按日汇总后写入同一张表。</p>
 */
@Data
@TableName("edu_dashboard_daily")
public class EduDashboardDaily implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    private LocalDate statDate;
    private Long visits;
    private Long orderCount;
    private BigDecimal orderRevenue;
    private Long newStudents;
    private Long activeUsers;
    private Long totalStudents;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @Version
    private Integer version;
}
