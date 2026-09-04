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
@TableName("tr_order")
public class TrOrder implements Serializable {
    @TableId(value = "id", type = IdType.INPUT) private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal payableAmount;
    private BigDecimal paidAmount;
    private Long couponId;
    private Integer orderStatus;
    private Integer paymentStatus;
    private String paymentChannel;
    private LocalDateTime expireTime;
    private LocalDateTime paidTime;
    private LocalDateTime refundTime;
    private String refundReason;
    private String legacyId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;
    @Version private Integer version;
}
