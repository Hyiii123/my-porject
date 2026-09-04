package com.share.trade.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("tr_order_item")
public class TrOrderItem implements Serializable {
    @TableId(value = "id", type = IdType.INPUT) private Long id;
    private Long orderId;
    private Long courseId;
    private String courseName;
    private String courseCoverUrl;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal payableAmount;
    private Integer quantity;
    private LocalDateTime createTime;
}
