package com.share.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.share.auth.form.LoginBody;
import com.share.auth.form.RegisterBody;
import com.share.auth.service.SysLoginService;
import com.share.common.core.domain.R;
import com.share.common.core.utils.JwtUtils;
import com.share.common.core.utils.StringUtils;
import com.share.common.security.auth.AuthUtil;
import com.share.common.security.service.TokenService;
import com.share.common.security.utils.SecurityUtils;
import com.share.system.api.model.LoginUser;

/**
 * token 控制
 * 
 * @author share
 */
@RestController
public class TokenController
{
    @Autowired
    private TokenService tokenService;

    @Autowired
    private SysLoginService sysLoginService;

    @PostMapping({"login", "accounts/login", "accounts/admin/login"})
    public R<?> login(@RequestBody(required = false) LoginBody form,
            @RequestParam Map<String, String> params)
    {
        LoginBody loginBody = form == null ? new LoginBody() : form;
        // 兼容天机前端的旧契约：账号登录使用 JSON，短信登录页面使用查询参数。
        String username = firstNonBlank(loginBody.getUsername(), params.get("username"),
                params.get("userName"), params.get("cellPhone"), params.get("phone"));
        String password = firstNonBlank(loginBody.getPassword(), params.get("password"), params.get("code"));
        // 用户登录；type=2 是天机前端的本地演示短信登录协议。
        LoginUser userInfo = "2".equals(params.get("type"))
                ? sysLoginService.loginByPhoneCode(username, password)
                : sysLoginService.login(username, password);
        // 获取登录token
        return R.ok(tokenService.createToken(userInfo));
    }

    @DeleteMapping({"logout", "accounts/logout"})
    public R<?> logout(HttpServletRequest request)
    {
        String token = SecurityUtils.getToken(request);
        if (StringUtils.isNotEmpty(token))
        {
            String username = JwtUtils.getUserName(token);
            // 删除用户缓存记录
            AuthUtil.logoutByToken(token);
            // 记录用户退出日志
            sysLoginService.logout(username);
        }
        return R.ok();
    }

    /** 兼容天机前端使用 POST 退出登录。 */
    @PostMapping("accounts/logout")
    public R<?> legacyLogout(HttpServletRequest request)
    {
        return logout(request);
    }

    @PostMapping({"refresh", "accounts/refresh"})
    public R<?> refresh(HttpServletRequest request)
    {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser))
        {
            // 刷新令牌有效期
            tokenService.refreshToken(loginUser);
            return R.ok(refreshView(request));
        }
        return R.fail(401, "登录状态已失效");
    }

    /** 兼容天机前端 GET /as/accounts/refresh 调用。 */
    @GetMapping({"refresh", "accounts/refresh"})
    public R<?> refreshByGet(HttpServletRequest request)
    {
        return refresh(request);
    }

    private Map<String, Object> refreshView(HttpServletRequest request)
    {
        String token = SecurityUtils.getToken(request);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("access_token", token);
        result.put("token", token);
        result.put("expires_in", 720);
        return result;
    }

    @PostMapping({"register", "users/register"})
    public R<?> register(@RequestBody(required = false) RegisterBody registerBody,
            @RequestParam Map<String, String> params)
    {
        RegisterBody body = registerBody == null ? new RegisterBody() : registerBody;
        String username = firstNonBlank(body.getUsername(), body.getCellPhone(), params.get("username"),
                params.get("userName"), params.get("phone"), params.get("cellPhone"));
        String password = firstNonBlank(body.getPassword(), params.get("password"));
        // 用户注册
        sysLoginService.register(username, password);
        return R.ok();
    }

    /**
     * 天机前端旧验证码接口的兼容实现。
     *
     * <p>当前本地环境关闭图形验证码，返回一次性演示验证码，正式环境应接入短信/图形验证码服务。</p>
     */
    @PostMapping("code/verifycode")
    public R<?> verifyCode(@RequestParam Map<String, String> params)
    {
        String phone = firstNonBlank(params.get("cellPhone"), params.get("phone"),
                params.get("phonenumber"), params.get("mobile"));
        String code = phone == null ? "123456" : sysLoginService.issuePhoneCode(phone);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("uuid", UUID.randomUUID().toString());
        result.put("code", code);
        result.put("message", phone == null ? "本地开发环境验证码为 123456" : "验证码已写入 Redis，有效期 5 分钟");
        return R.ok(result);
    }

    private String firstNonBlank(String... values)
    {
        for (String value : values)
        {
            if (value != null && !value.isBlank())
            {
                return value.trim();
            }
        }
        return null;
    }
}
