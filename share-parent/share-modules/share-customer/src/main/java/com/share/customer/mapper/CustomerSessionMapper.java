package com.share.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.customer.domain.CustomerSession;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/** 会话 Mapper。 */
public interface CustomerSessionMapper extends BaseMapper<CustomerSession> {

    /**
     * 查询超过指定时间无任何对话的活跃会话 ID 列表。
     *
     * 规则说明：
     * 1. 必须为未软删除的会话 (del_flag = 0)；
     * 2. 必须为活跃状态会话（非已归档，即 status IS NULL OR status != 4）；
     * 3. 会话自身有效更新时间（updated_at 优先，为空回退 create_time / started_at）早于等于 cutoffTime；
     * 4. 且在 cutoffTime 之后不存在任何该会话的新增消息。
     *
     * @param cutoffTime 截止过期时间点
     * @param userId     可选用户 ID 过滤（为 null 时检索全域所有用户）
     * @return 超期无对话的活跃会话 ID 列表
     */
    @Select("<script>" +
            "SELECT s.id FROM cs_session s " +
            "WHERE s.del_flag = 0 " +
            "  AND (s.status IS NULL OR s.status != 4) " +
            "  AND IFNULL(s.updated_at, IFNULL(s.create_time, s.started_at)) &lt;= #{cutoffTime} " +
            "<if test='userId != null'> AND s.user_id = #{userId} </if>" +
            "  AND NOT EXISTS ( " +
            "      SELECT 1 FROM cs_message m " +
            "      WHERE m.session_id = s.id AND m.create_time &gt; #{cutoffTime} " +
            "  )" +
            "</script>")
    List<Long> selectExpiredActiveSessionIds(@Param("cutoffTime") LocalDateTime cutoffTime, @Param("userId") Long userId);
}
