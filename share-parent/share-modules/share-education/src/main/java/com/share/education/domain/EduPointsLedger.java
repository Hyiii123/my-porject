package com.share.education.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("edu_points_ledger")
public class EduPointsLedger implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.INPUT) private Long id;
    private Long userId;
    private Integer changeAmount;
    private Integer balanceAfter;
    private String sourceType;
    private String bizId;
    private String remark;
    private LocalDateTime createTime;
}
