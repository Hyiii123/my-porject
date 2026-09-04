package com.share.customer.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** AI 调用审计日志，不保存用户消息正文或 API Key。 */
@Data
@TableName("cs_ai_call_log")
public class CustomerAiCallLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String requestNo;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;
    private String provider;
    private String model;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer latencyMs;
    private Integer resultStatus;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
