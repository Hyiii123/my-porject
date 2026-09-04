package com.share.customer.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.io.Serializable;
import java.util.List;
import java.time.LocalDateTime;
import lombok.Data;

/** 客服会话。 */
@Data
@TableName("cs_session")
public class CustomerSession implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String sessionNo;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String userName;
    private String source;
    private Integer status;
    private String lastMessage;
    private Integer satisfactionScore;
    private LocalDateTime startedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    private String legacyId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;
    @Version
    private Integer version;

    /** 详情接口的非持久化扩展字段，避免前端再发起一次请求。 */
    @TableField(exist = false)
    private List<CustomerMessage> messages;

    /** 详情接口的非持久化扩展字段。 */
    @TableField(exist = false)
    private CustomerEvaluation evaluation;
}
