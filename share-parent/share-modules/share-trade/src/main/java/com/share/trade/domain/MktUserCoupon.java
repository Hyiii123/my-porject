package com.share.trade.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("mkt_user_coupon")
public class MktUserCoupon implements Serializable {
    @TableId(value = "id", type = IdType.INPUT) private Long id;
    private Long userId;
    private Long couponId;
    private String sourceType;
    private String receiveCode;
    private Integer status;
    private LocalDateTime receivedAt;
    private LocalDateTime expireAt;
    private LocalDateTime usedAt;
    private Long usedOrderId;
    private String legacyId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;
    @Version private Integer version;
}
