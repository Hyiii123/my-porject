package com.share.education.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.education.domain.EduDashboardDaily;
import org.apache.ibatis.annotations.Select;

/** 教育工作台日统计 Mapper。 */
public interface EduDashboardDailyMapper extends BaseMapper<EduDashboardDaily> {
    /** 统计去重后的学习用户数，避免把同一用户的多门课程记录重复算作学员。 */
    @Select("SELECT COUNT(DISTINCT user_id) FROM edu_learning_record WHERE del_flag = 0")
    Long selectDistinctStudentCount();
}
