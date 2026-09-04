package com.share.customer.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/** 客服消息。 */
@Data
@TableName("cs_message")
public class CustomerMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;
    private Integer messageType;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long senderId;
    private String senderName;
    private String content;
    private String aiModel;
    private Integer tokenUsage;
    @TableField("is_fallback")
    private Integer isFallback;
    private String legacyId;
    private LocalDateTime createTime;
}
