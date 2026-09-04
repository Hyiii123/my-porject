package com.share.education.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 课程小节与题库题目的关联。 */
@Data
@TableName("edu_catalog_question")
public class EduCatalogQuestion implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    private Long courseId;
    private Long catalogId;
    private Long questionId;
    private Integer sortNum;
    private BigDecimal score;
    private LocalDateTime createTime;
}
