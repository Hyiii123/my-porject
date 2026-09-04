package com.share.customer.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/** 第三方 AI 配置，不向前端返回 api_key_ciphertext。 */
@Data
@TableName("cs_ai_config")
public class CustomerAiConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    private String provider;
    private String baseUrl;
    private String endpointPath;
    private String model;
    private String apiKeyCiphertext;
    private Integer enabled;
    private Integer timeoutMs;
    private Integer maxRetries;
    private String systemPrompt;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    @Version
    private Integer version;
}
