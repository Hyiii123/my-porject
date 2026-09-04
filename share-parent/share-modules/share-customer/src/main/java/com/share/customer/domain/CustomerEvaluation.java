package com.share.customer.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 会话服务评价。 */
@Data
@TableName(value = "cs_evaluation", autoResultMap = true)
public class CustomerEvaluation implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;
    private Integer score;
    /** JSON 字符串，避免引入额外 JSON TypeHandler。 */
    private String tagsJson;
    @TableField(exist = false)
    private List<String> tags;
    private String comment;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
