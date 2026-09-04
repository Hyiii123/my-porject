package com.share.education.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("edu_exam_question")
public class EduExamQuestion implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.INPUT) private Long id;
    private Long examId;
    private Long questionId;
    private Integer sortNum;
    private BigDecimal score;
}
