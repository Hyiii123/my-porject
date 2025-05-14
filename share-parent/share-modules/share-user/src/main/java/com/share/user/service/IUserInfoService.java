package com.share.user.service;

import java.util.List;

import com.share.domain.UserInfo;
import com.share.user.domain.UpdateUserLogin;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户Service接口
 *
 * @author atguigu
 * @date 2025-05-09
 */
public interface IUserInfoService extends IService<UserInfo>
{

    /**
     * 查询用户列表
     *
     * @param userInfo 用户
     * @return 用户集合
     */
    public List<UserInfo> selectUserInfoList(UserInfo userInfo);

    UserInfo wxLogin(String code);

    Boolean updateUserLogin(UpdateUserLogin updateUserLogin);

}
