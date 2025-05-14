package com.share.user.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.domain.UserInfo;
import com.share.user.domain.UpdateUserLogin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.share.user.mapper.UserInfoMapper;
import com.share.user.service.IUserInfoService;

/**
 * 用户Service业务层处理
 *
 * @author atguigu
 * @date 2025-05-09
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService
{
    @Autowired
    private UserInfoMapper userInfoMapper;

    /**
     * 查询用户列表
     *
     * @param userInfo 用户
     * @return 用户
     */
    @Override
    public List<UserInfo> selectUserInfoList(UserInfo userInfo)
    {
        return userInfoMapper.selectUserInfoList(userInfo);
    }

    @Override
    public UserInfo wxLogin(String code) {
        return null;
    }

    @Override
    public Boolean updateUserLogin(UpdateUserLogin updateUserLogin) {
        return null;
    }

}
