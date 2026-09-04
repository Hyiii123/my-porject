package com.share.education.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("edu_exam_record")
public class EduExamRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.INPUT) private Long id;
    private Long userId;
    private Long examId;
    private BigDecimal score;
    private BigDecimal totalScore;
    private Integer correctCount;
    private Integer questionCount;
    private Integer status;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private String legacyId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;
    @Version private Integer version;
}
