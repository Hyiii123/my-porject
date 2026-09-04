package com.share.customer.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/** 客服知识库条目。 */
@Data
@TableName("cs_knowledge")
public class CustomerKnowledge implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String question;
    private String answer;
    private String keywords;
    private String category;
    private Integer status;
    private Integer hitCount;
    private String legacyId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long createBy;
    private LocalDateTime createTime;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateBy;
    private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;
    @Version
    private Integer version;
}
