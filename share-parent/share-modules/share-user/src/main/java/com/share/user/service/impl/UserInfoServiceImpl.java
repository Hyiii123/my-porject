package com.share.user.service.impl;

import java.util.List;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.utils.StringUtils;
import com.share.domain.UserInfo;
import com.share.user.domain.UpdateUserLogin;
import com.share.user.domain.UserLoginLog;
import com.share.user.mapper.UserInfoMapper;
import com.share.user.mapper.UserLoginLogMapper;
import com.share.user.service.IUserInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户Service业务层处理
 *
 * @author atguigu
 * @date 2025-05-09
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService
{
    private static final Logger log = LoggerFactory.getLogger(UserInfoServiceImpl.class);

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserLoginLogMapper userLoginLogMapper;

    @Autowired(required = false)
    private WxMaService wxMaService;

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
        if (StringUtils.isEmpty(code)) {
            return null;
        }
        String openId = null;
        if (wxMaService != null) {
            try {
                WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
                if (sessionInfo != null) {
                    openId = sessionInfo.getOpenid();
                }
            } catch (Exception e) {
                log.warn("微信授权登录换取 session 失败，降级使用模拟 openid: code={}, error={}", code, e.getMessage());
            }
        }
        if (StringUtils.isEmpty(openId)) {
            openId = code.startsWith("mock_") ? code : ("mock_wx_" + code);
        }

        UserInfo userInfo = this.getOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getWxOpenId, openId));
        if (userInfo == null) {
            userInfo = new UserInfo();
            userInfo.setWxOpenId(openId);
            String suffix = openId.length() > 6 ? openId.substring(openId.length() - 6) : openId;
            userInfo.setNickname("微信用户_" + suffix);
            userInfo.setStatus("1");
            this.save(userInfo);
        }
        return userInfo;
    }

    @Override
    public Boolean updateUserLogin(UpdateUserLogin updateUserLogin) {
        if (updateUserLogin == null || updateUserLogin.getUserId() == null) {
            return false;
        }
        try {
            UserLoginLog loginLog = new UserLoginLog();
            loginLog.setUserId(updateUserLogin.getUserId());
            loginLog.setIpaddr(updateUserLogin.getLastLoginIp());
            loginLog.setStatus(0);
            loginLog.setMsg("登录成功");
            userLoginLogMapper.insert(loginLog);
            return true;
        } catch (Exception e) {
            log.error("记录用户登录日志失败: userId={}, error={}", updateUserLogin.getUserId(), e.getMessage());
            return false;
        }
    }

}
