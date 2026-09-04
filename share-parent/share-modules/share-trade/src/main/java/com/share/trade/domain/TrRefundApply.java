package com.share.trade.domain;

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
@TableName("tr_refund_apply")
public class TrRefundApply implements Serializable {
    @TableId(value = "id", type = IdType.INPUT) private Long id;
    private String refundNo;
    private Long orderId;
    private Long userId;
    private BigDecimal refundAmount;
    private String reason;
    private Integer status;
    private Long auditUserId;
    private String auditRemark;
    private LocalDateTime auditTime;
    private LocalDateTime refundedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @Version private Integer version;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;
}
