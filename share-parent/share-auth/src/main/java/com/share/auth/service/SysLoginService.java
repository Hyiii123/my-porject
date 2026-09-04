package com.share.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;
import com.share.common.core.constant.CacheConstants;
import com.share.common.core.constant.Constants;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.constant.UserConstants;
import com.share.common.core.domain.R;
import com.share.common.core.enums.UserStatus;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.text.Convert;
import com.share.common.core.utils.StringUtils;
import com.share.common.core.utils.ip.IpUtils;
import com.share.common.redis.service.RedisService;
import com.share.common.security.utils.SecurityUtils;
import com.share.system.api.RemoteUserService;
import com.share.system.api.domain.SysUser;
import com.share.system.api.model.LoginUser;

/**
 * 登录校验方法
 *
 * @author share
 */
@Component
public class SysLoginService
{
    private static final String PHONE_CODE_PREFIX = "tianji:auth:verifycode:";
    private static final String LOCAL_DEMO_CODE = "123456";
    private static final long PHONE_CODE_TTL_SECONDS = 300L;

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private SysPasswordService passwordService;

    @Autowired
    private SysRecordLogService recordLogService;

    @Autowired
    private RedisService redisService;

    /**
     * 登录
     */
    public LoginUser login(String username, String password)
    {
        // 用户名或密码为空 错误
        if (StringUtils.isAnyBlank(username, password))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户/密码必须填写");
            throw new ServiceException("用户/密码必须填写");
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户密码不在指定范围");
            throw new ServiceException("用户密码不在指定范围");
        }
        // 用户名不在指定范围内 错误
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户名不在指定范围");
            throw new ServiceException("用户名不在指定范围");
        }
        // IP黑名单校验
        String blackStr = Convert.toStr(redisService.getCacheObject(CacheConstants.SYS_LOGIN_BLACKIPLIST));
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "很遗憾，访问IP已被列入系统黑名单");
            throw new ServiceException("很遗憾，访问IP已被列入系统黑名单");
        }
        // 查询用户信息
        R<LoginUser> userResult = remoteUserService.getUserInfo(username, SecurityConstants.INNER);

        if (StringUtils.isNull(userResult) || StringUtils.isNull(userResult.getData()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "登录用户不存在");
            throw new ServiceException("登录用户：" + username + " 不存在");
        }

        if (R.FAIL == userResult.getCode())
        {
            throw new ServiceException(userResult.getMsg());
        }

        LoginUser userInfo = userResult.getData();
        SysUser user = userResult.getData().getSysUser();
        if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "对不起，您的账号已被删除");
            throw new ServiceException("对不起，您的账号：" + username + " 已被删除");
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户已停用，请联系管理员");
            throw new ServiceException("对不起，您的账号：" + username + " 已停用");
        }
        passwordService.validate(userInfo, password);
        recordLogService.recordLogininfor(username, Constants.LOGIN_SUCCESS, "登录成功");
        return userInfo;
    }

    /**
     * 本地演示短信登录。短信服务尚未接入时，验证码接口会在 Redis 中保存
     * 123456；为兼容直接打开短信登录页的演示流程，同时允许该演示码登录。
     */
    public LoginUser loginByPhoneCode(String phone, String code)
    {
        if (StringUtils.isAnyBlank(phone, code))
        {
            recordLogService.recordLogininfor(phone, Constants.LOGIN_FAIL, "手机号/验证码必须填写");
            throw new ServiceException("手机号/验证码必须填写");
        }
        String cachedCode = redisService.getCacheObject(PHONE_CODE_PREFIX + phone);
        if (!LOCAL_DEMO_CODE.equals(code) && !code.equals(cachedCode))
        {
            recordLogService.recordLogininfor(phone, Constants.LOGIN_FAIL, "验证码错误或已过期");
            throw new ServiceException("验证码错误或已过期");
        }
        if (cachedCode != null)
        {
            redisService.deleteObject(PHONE_CODE_PREFIX + phone);
        }

        R<LoginUser> userResult = remoteUserService.getUserInfoByPhone(phone, SecurityConstants.INNER);
        if (StringUtils.isNull(userResult) || StringUtils.isNull(userResult.getData()))
        {
            recordLogService.recordLogininfor(phone, Constants.LOGIN_FAIL, "手机号对应的用户不存在");
            throw new ServiceException("手机号对应的用户不存在");
        }
        if (R.FAIL == userResult.getCode())
        {
            throw new ServiceException(userResult.getMsg());
        }

        LoginUser userInfo = userResult.getData();
        SysUser user = userInfo.getSysUser();
        if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
        {
            recordLogService.recordLogininfor(phone, Constants.LOGIN_FAIL, "账号已删除");
            throw new ServiceException("对不起，您的账号已被删除");
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            recordLogService.recordLogininfor(phone, Constants.LOGIN_FAIL, "用户已停用");
            throw new ServiceException("对不起，您的账号已停用");
        }
        recordLogService.recordLogininfor(phone, Constants.LOGIN_SUCCESS, "短信登录成功");
        return userInfo;
    }

    /**
     * 生成并保存手机号验证码。
     *
     * <p>本地演示环境不接入真实短信服务，固定使用 123456；验证码仍然写入 Redis
     * 并设置有效期，保证前端流程和生产替换点保持一致。</p>
     */
    public String issuePhoneCode(String phone)
    {
        if (StringUtils.isBlank(phone))
        {
            throw new ServiceException("手机号不能为空");
        }
        String normalizedPhone = phone.trim();
        redisService.setCacheObject(PHONE_CODE_PREFIX + normalizedPhone, LOCAL_DEMO_CODE,
                PHONE_CODE_TTL_SECONDS, TimeUnit.SECONDS);
        return LOCAL_DEMO_CODE;
    }

    public void logout(String loginName)
    {
        recordLogService.recordLogininfor(loginName, Constants.LOGOUT, "退出成功");
    }

    /**
     * 注册
     */
    public void register(String username, String password)
    {
        // 用户名或密码为空 错误
        if (StringUtils.isAnyBlank(username, password))
        {
            throw new ServiceException("用户/密码必须填写");
        }
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            throw new ServiceException("账户长度必须在2到20个字符之间");
        }
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            throw new ServiceException("密码长度必须在5到20个字符之间");
        }

        // 注册用户信息
        SysUser sysUser = new SysUser();
        sysUser.setUserName(username);
        sysUser.setNickName(username);
        sysUser.setPassword(SecurityUtils.encryptPassword(password));
        R<?> registerResult = remoteUserService.registerUserInfo(sysUser, SecurityConstants.INNER);

        if (R.FAIL == registerResult.getCode())
        {
            throw new ServiceException(registerResult.getMsg());
        }
        recordLogService.recordLogininfor(username, Constants.REGISTER, "注册成功");
    }
}
