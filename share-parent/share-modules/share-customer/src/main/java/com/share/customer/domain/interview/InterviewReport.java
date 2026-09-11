package com.share.customer.domain.interview;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 终局多维能力诊断报告实体。
 */
@Data
@TableName("cs_interview_report")
public class InterviewReport implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;

    private String offerDecision; // Strong Hire / Hire / Weak Hire / Reject
    private String levelMatch;    // 对标阿里P6+ / 字节2-1 等
    private String radarData;      // JSON: {"core":88,"architecture":75,"storage":85,"distributed":70,"coding":80,"communication":85}
    private String overallSummary;
    private String coreStrengths;
    private String criticalWeaknesses;
    private String speechRefactoring;
    private String recommendedCourses; // JSON
    private LocalDateTime createTime;
}
