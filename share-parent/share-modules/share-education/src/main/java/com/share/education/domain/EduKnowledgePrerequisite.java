package com.share.education.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识点先修前驱依赖图谱实体类。
 */
@Data
@TableName("edu_knowledge_prerequisite")
public class EduKnowledgePrerequisite implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 所属领域编码 (可为空代表跨领域通用) */
    private String domainCode;

    /** 后置核心概念/课程技能词，如 SpringCloud微服务, Kubernetes, Redis高并发缓存 */
    private String conceptName;

    /** 必须前置修读的先修概念，如 SpringBoot基础, Docker容器, SQL基础 */
    private String prerequisiteConcept;

    /** 依赖强弱权重 (0.00 ~ 1.00，越高代表越不可跳过) */
    private BigDecimal strengthWeight;

    /** 先修依赖教学法依据说明 */
    private String rationale;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;

    @Version
    private Integer version;
}
