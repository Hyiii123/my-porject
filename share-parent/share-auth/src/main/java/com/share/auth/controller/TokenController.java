package com.share.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.share.auth.form.LoginBody;
import com.share.auth.form.RegisterBody;
import com.share.auth.service.QQMailService;
import com.share.auth.service.SysLoginService;
import com.share.common.core.domain.R;
import com.share.common.core.utils.JwtUtils;
import com.share.common.core.utils.StringUtils;
import com.share.common.redis.service.RedisService;
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
    private static final String EMAIL_CODE_PREFIX = "zhiwen:auth:emailcode:";
    private static final long EMAIL_CODE_TTL_SECONDS = 300L;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private SysLoginService sysLoginService;

    @Autowired
    private QQMailService qqMailService;

    @Autowired
    private RedisService redisService;

    @PostMapping({"login", "accounts/login", "accounts/admin/login"})
    public R<?> login(@RequestBody(required = false) LoginBody form,
            @RequestParam Map<String, String> params)
    {
        LoginBody loginBody = form == null ? new LoginBody() : form;
        // 兼容智问前端契约：账号登录使用 JSON，短信/邮箱登录使用 JSON 或查询参数。
        String username = firstNonBlank(loginBody.getUsername(), params.get("username"),
                params.get("userName"), loginBody.getEmail(), params.get("email"),
                params.get("qqEmail"), params.get("cellPhone"), params.get("phone"));
        String password = firstNonBlank(loginBody.getPassword(), params.get("password"),
                loginBody.getCode(), params.get("code"));
        String loginType = firstNonBlank(loginBody.getType(), params.get("type"));

        // 用户登录：区分 密码登录、邮箱验证码登录、手机号登录
        LoginUser userInfo;
        if ("email".equalsIgnoreCase(loginType) || (username != null && username.contains("@") && "2".equals(loginType)))
        {
            userInfo = sysLoginService.loginByEmailCode(username, password);
        }
        else if ("2".equals(loginType))
        {
            userInfo = sysLoginService.loginByPhoneCode(username, password);
        }
        else
        {
            userInfo = sysLoginService.login(username, password);
        }
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

    /** 兼容智问前端使用 POST 退出登录。 */
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

    /** 兼容智问前端 GET /as/accounts/refresh 调用。 */
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
            @RequestParam(required = false) Map<String, String> params)
    {
        RegisterBody body = registerBody == null ? new RegisterBody() : registerBody;
        Map<String, String> p = params == null ? Map.of() : params;
        String email = firstNonBlank(body.getEmail(), p.get("email"), p.get("qqEmail"));
        String code = firstNonBlank(body.getCode(), p.get("code"));
        String password = firstNonBlank(body.getPassword(), p.get("password"));

        // QQ 邮箱注册分支
        if (email != null && !email.isBlank())
        {
            String normalizedEmail = email.trim().toLowerCase();
            if (code == null || code.isBlank())
            {
                return R.fail("请输入邮箱验证码");
            }
            String cachedCode = redisService.getCacheObject(EMAIL_CODE_PREFIX + normalizedEmail);
            if (cachedCode == null || !cachedCode.equals(code.trim()))
            {
                return R.fail("邮箱验证码错误或已失效");
            }
            // 校验通过后销毁验证码，防止重放
            redisService.deleteObject(EMAIL_CODE_PREFIX + normalizedEmail);

            // 用户注册
            sysLoginService.registerWithEmail(normalizedEmail, password);
            return R.ok();
        }

        // 普通 / 手机号注册分支
        String username = firstNonBlank(body.getUsername(), body.getCellPhone(), p.get("username"),
                p.get("userName"), p.get("phone"), p.get("cellPhone"));
        sysLoginService.register(username, password);
        return R.ok();
    }

    /**
     * 验证码接口：支持 QQ 邮箱验证码发送与手机号验证码兼容。
     */
    @PostMapping("code/verifycode")
    public R<?> verifyCode(@RequestParam(required = false) Map<String, String> params,
            @RequestBody(required = false) Map<String, String> body)
    {
        Map<String, String> merged = new LinkedHashMap<>();
        if (body != null) merged.putAll(body);
        if (params != null) merged.putAll(params);

        String email = firstNonBlank(merged.get("email"), merged.get("qqEmail"), merged.get("mail"));
        if (email != null && !email.isBlank())
        {
            String normalizedEmail = email.trim();
            if (!normalizedEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
            {
                return R.fail("请输入格式正确的邮箱地址");
            }
            if (!normalizedEmail.toLowerCase().endsWith("@qq.com") && !normalizedEmail.toLowerCase().endsWith("@foxmail.com"))
            {
                return R.fail("当前支持 QQ 邮箱登录与注册，请输入 @qq.com 邮箱");
            }

            // 生成强随机 6 位数字验证码
            int randomNum = new java.security.SecureRandom().nextInt(900000) + 100000;
            String code = String.valueOf(randomNum);

            // 写入 Redis，有效期 5 分钟
            redisService.setCacheObject(EMAIL_CODE_PREFIX + normalizedEmail.toLowerCase(), code,
                    EMAIL_CODE_TTL_SECONDS, TimeUnit.SECONDS);

            // 调用 QQ 邮箱 SMTP 服务发送邮件
            boolean sent = qqMailService.sendVerificationCode(normalizedEmail, code);

            Map<String, String> result = new LinkedHashMap<>();
            result.put("uuid", UUID.randomUUID().toString());
            result.put("email", normalizedEmail);
            if (sent)
            {
                result.put("message", "验证码已成功发送至您的 QQ 邮箱，请查收");
            }
            else
            {
                result.put("code", code);
                result.put("message", "验证码已生成（调试直显: " + code + "）。配置 QQ 邮箱授权码即可真实投递");
            }
            return R.ok(result);
        }

        String phone = firstNonBlank(merged.get("cellPhone"), merged.get("phone"),
                merged.get("phonenumber"), merged.get("mobile"));
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
