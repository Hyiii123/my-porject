package com.share.education.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("edu_course")
public class EduCourse implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.INPUT) private Long id;
    private Long categoryId;
    private String courseCode;
    private String courseName;
    private String coverUrl;
    private String shortDescription;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer lessonCount;
    private Integer learnerCount;
    private Integer durationMinutes;
    private BigDecimal rating;
    @TableField("is_free") private Integer isFree;
    @TableField("is_recommended") private Integer isRecommended;
    @TableField("is_hot") private Integer isHot;
    @TableField("is_new") private Integer isNew;
    private Integer sortNum;
    private Integer status;
    private LocalDateTime publishTime;
    private String legacyId;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;
    @Version private Integer version;
}
