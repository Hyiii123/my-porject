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
@TableName("edu_exam")
public class EduExam implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.INPUT) private Long id;
    private Long courseId;
    private String examName;
    private String description;
    private BigDecimal totalScore;
    private BigDecimal passScore;
    private Integer durationMinutes;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String legacyId;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;
    @Version private Integer version;
}
