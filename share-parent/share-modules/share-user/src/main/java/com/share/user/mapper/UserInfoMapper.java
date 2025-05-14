package com.share.user.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.domain.UserInfo;

/**
 * 用户Mapper接口
 *
 * @author atguigu
 * @date 2025-05-09
 */
public interface UserInfoMapper extends BaseMapper<UserInfo>
{

    /**
     * 查询用户列表
     *
     * @param userInfo 用户
     * @return 用户集合
     */
    public List<UserInfo> selectUserInfoList(UserInfo userInfo);

}
