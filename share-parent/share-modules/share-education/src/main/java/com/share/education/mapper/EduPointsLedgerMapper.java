package com.share.education.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.education.domain.EduPointsLedger;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface EduPointsLedgerMapper extends BaseMapper<EduPointsLedger> {

    @Select("SELECT COALESCE(SUM(change_amount), 0) FROM edu_points_ledger WHERE user_id = #{userId}")
    Integer selectTotalPointsByUserId(@Param("userId") Long userId);

    @Select("SELECT user_id AS userId, COALESCE(SUM(change_amount), 0) AS points " +
            "FROM edu_points_ledger GROUP BY user_id ORDER BY points DESC, user_id ASC LIMIT #{limit}")
    List<Map<String, Object>> selectPointsLeaderboard(@Param("limit") int limit);

    @Select("SELECT COUNT(*) + 1 FROM (" +
            "SELECT user_id, SUM(change_amount) AS pts FROM edu_points_ledger " +
            "GROUP BY user_id HAVING pts > (" +
            "SELECT COALESCE(SUM(change_amount), 0) FROM edu_points_ledger WHERE user_id = #{userId}" +
            ")) t")
    Integer selectUserRank(@Param("userId") Long userId);
}
