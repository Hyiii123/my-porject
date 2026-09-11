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
 * AI 面试问答轮次与深度追问实体。
 */
@Data
@TableName("cs_interview_turn")
public class InterviewTurn implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;

    private Integer turnNum;
    private String dimension; // Java核心/系统设计/高并发/算法/线上排障/HR胜任力
    private String question;
    private String userAnswer;
    private Integer depthLevel; // 1-概念摸底, 2-底层原理, 3-线上极限排障
    @JsonSerialize(using = ToStringSerializer.class)
    private Long matchedKnowledgeId;
    private String aiFeedback;
    private String standardReference;
    private Integer turnScore;
    private LocalDateTime createTime;
    private LocalDateTime answerTime;
}
