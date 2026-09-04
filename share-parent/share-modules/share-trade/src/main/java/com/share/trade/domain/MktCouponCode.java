package com.share.trade.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/** 优惠券兑换码。旧管理端使用 codes/page 查询此表。 */
@Data
@TableName("mkt_coupon_code")
public class MktCouponCode implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    private Long couponId;
    private String couponCode;
    private Integer status;
    private Long exchangedUserId;
    private LocalDateTime exchangedTime;
    private LocalDateTime createTime;
}
