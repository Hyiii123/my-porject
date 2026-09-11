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
 * 算法代码手撕与沙箱评测实体。
 */
@Data
@TableName("cs_interview_code")
public class InterviewCodeSubmission implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long turnId;

    private String problemTitle;
    private String language;
    private String userCode;
    private String executionStatus; // accepted/syntax_error/timeout/runtime_error
    private String timeComplexity;
    private String spaceComplexity;
    private String codeSmells;
    private String refactoredCode;
    private Integer passedTestCases;
    private Integer totalTestCases;
    private LocalDateTime createTime;
}
