package com.share.customer.domain.interview;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 模拟面试场次实体。
 */
@Data
@TableName("cs_interview_session")
public class InterviewSession implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String userName;
    private String targetJob;
    private String companyTarget;
    private String interviewerStyle;
    private Integer status; // 1-进行中, 2-已完成, 3-已终止
    private Integer currentTurn;
    private Integer totalTurns;
    private Integer score;
    private Integer durationSeconds;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<InterviewTurn> turns;

    @TableField(exist = false)
    private InterviewReport report;

    @TableField(exist = false)
    private List<InterviewCodeSubmission> codeSubmissions;
}
