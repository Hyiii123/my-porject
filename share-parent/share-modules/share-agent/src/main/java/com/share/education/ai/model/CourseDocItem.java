package com.share.education.ai.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 智能体检索与向量/图谱索引通用课程文档项 DTO (CourseDocItem)。
 */
@Data
public class CourseDocItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String courseName;
    private String description;
    private String shortDescription;
    private String skills;
    private String targetRole;
    private String prerequisites;
    private BigDecimal price;
    private String coverUrl;
    private Integer status;
    private Integer learnerCount;

    public CourseDocItem() {}

    public CourseDocItem(Long id, String courseName, String skills, BigDecimal price, String coverUrl) {
        this.id = id;
        this.courseName = courseName;
        this.skills = skills;
        this.price = price;
        this.coverUrl = coverUrl;
        this.status = 1;
    }
}
