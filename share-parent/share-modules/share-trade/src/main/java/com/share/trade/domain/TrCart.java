package com.share.trade.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("tr_cart")
public class TrCart implements Serializable {
    /**
     * 购物车使用逻辑删除。数据库通过 active_user_id/active_course_id 生成列唯一索引，
     * 只限制有效购物车，允许同一课程保留多条历史软删除记录。
     */
    @TableId(value = "id", type = IdType.INPUT) private Long id;
    private Long userId;
    private Long courseId;
    /** 加购时保存课程展示快照，课程下架后购物车仍可清晰展示。 */
    private String courseName;
    private String courseCoverUrl;
    private String teacherName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private Integer selected;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;
}
