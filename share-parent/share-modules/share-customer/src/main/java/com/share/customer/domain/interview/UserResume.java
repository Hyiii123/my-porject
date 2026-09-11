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
 * 个人中心用户简历及 AI 深度诊断实体。
 */
@Data
@TableName("cs_user_resume")
public class UserResume implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String userName;

    /**
     * 原始上传文件名（如：张三_Java高级_5年.pdf）。
     */
    private String fileName;

    /**
     * 简历完整原始纯文本/Markdown内容。
     */
    private String rawContent;

    /**
     * 当前对标的目标岗位。
     */
    private String targetJob;

    /**
     * 当前对标的目标大厂企业。
     */
    private String targetCompany;

    /**
     * 岗位综合匹配度评分（0~100）。
     */
    private Integer matchScore;

    /**
     * 综合评级（如：阿里P6+/字节2-1 潜质）。
     */
    private String matchLevel;

    /**
     * 萃取的核心技能标签（JSON 数组格式）。
     */
    private String techTags;

    /**
     * 项目核心高光点挖掘（JSON 数组格式）。
     */
    private String projectHighlights;

    /**
     * 简历潜在薄弱项与面试风险点（JSON 数组格式）。
     */
    private String resumeGaps;

    /**
     * 大厂面试官预测深挖必考题（JSON 数组格式）。
     */
    private String predictedQuestions;

    /**
     * STAR 原则简历重塑与优化建议。
     */
    private String starAdvice;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
