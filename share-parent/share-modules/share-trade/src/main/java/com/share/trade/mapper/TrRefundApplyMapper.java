package com.share.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.trade.domain.TrRefundApply;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Select;

public interface TrRefundApplyMapper extends BaseMapper<TrRefundApply> {

    @Select("SELECT COALESCE(SUM(refund_amount), 0) FROM tr_refund_apply WHERE status IN (1, 3) AND del_flag = 0")
    BigDecimal selectTotalRefundAmount();
}
