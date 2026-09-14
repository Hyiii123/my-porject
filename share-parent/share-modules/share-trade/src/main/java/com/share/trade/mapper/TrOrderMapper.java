package com.share.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.trade.domain.TrOrder;
import java.util.Map;
import org.apache.ibatis.annotations.Select;

public interface TrOrderMapper extends BaseMapper<TrOrder> {

    @Select("SELECT " +
            "COUNT(*) AS totalOrders, " +
            "COALESCE(SUM(total_amount), 0) AS totalAmount, " +
            "COALESCE(SUM(CASE WHEN order_status = 0 THEN payable_amount ELSE 0 END), 0) AS pendingAmount, " +
            "COALESCE(SUM(CASE WHEN payment_status = 1 THEN paid_amount ELSE 0 END), 0) AS paidAmount, " +
            "COUNT(CASE WHEN order_status = 0 THEN 1 END) AS pendingOrders, " +
            "COUNT(CASE WHEN payment_status = 1 THEN 1 END) AS paidOrders " +
            "FROM tr_order WHERE del_flag = 0")
    Map<String, Object> selectOrderStats();
}
