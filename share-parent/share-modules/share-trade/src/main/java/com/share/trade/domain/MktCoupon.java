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
@TableName("mkt_coupon")
public class MktCoupon implements Serializable {
    @TableId(value = "id", type = IdType.INPUT) private Long id;
    private String couponName;
    private Integer couponType;
    private Integer discountType;
    private BigDecimal discountValue;
    private BigDecimal thresholdAmount;
    private BigDecimal maxDiscountAmount;
    private Integer totalCount;
    private Integer receivedCount;
    private Integer usedCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private String description;
    private String legacyId;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;
    @Version private Integer version;
}
