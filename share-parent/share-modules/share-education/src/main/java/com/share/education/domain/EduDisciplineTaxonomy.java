package com.share.education.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学科领域技术字典与边界隔离实体类。
 */
@Data
@TableName("edu_discipline_taxonomy")
public class EduDisciplineTaxonomy implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 领域唯一标识编码，如 JAVA_BACKEND, FRONTEND, BIG_DATA, AI_LLM, GO_CLOUD_NATIVE, MOBILE */
    private String domainCode;

    /** 领域中文名称，如 Java 后端开发 */
    private String domainName;

    /** 允许召回的课程分类 ID 列表 JSON 数组，如 [2, 4] */
    private String allowedCategoryIds;

    /** 白名单技能关键词列表 JSON 数组 */
    private String whitelistKeywords;

    /** 强排他黑名单关键词列表 JSON 数组 */
    private String blacklistKeywords;

    /** 目标岗位匹配词条列表 JSON 数组 */
    private String targetRolePatterns;

    /** 是否为全局默认兜底领域 (1:是, 0:否) */
    private Integer isDefault;

    /** 排序权重 */
    private Integer sortNum;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;

    @Version
    private Integer version;
}
