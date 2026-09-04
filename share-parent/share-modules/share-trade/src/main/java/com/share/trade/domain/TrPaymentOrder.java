package com.share.trade.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("tr_payment_order")
public class TrPaymentOrder implements Serializable {
    @TableId(value = "id", type = IdType.INPUT) private Long id;
    private String paymentNo;
    private Long orderId;
    private String paymentChannel;
    private BigDecimal amount;
    private Integer status;
    private String thirdPartyNo;
    private String failureReason;
    private LocalDateTime expireTime;
    private LocalDateTime paidTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @Version private Integer version;
}
