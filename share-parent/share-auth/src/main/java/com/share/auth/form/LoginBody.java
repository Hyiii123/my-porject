package com.share.auth.form;

/**
 * 用户登录对象
 *
 * @author share
 */
public class LoginBody
{
    /**
     * 用户名
     */
    private String username;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 邮箱字段，支持 QQ 邮箱验证码登录
     */
    private String email;

    /**
     * 验证码
     */
    private String code;

    /**
     * 登录类型：1-密码登录，2-短信登录，email-邮箱验证码登录
     */
    private String type;

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }
}
