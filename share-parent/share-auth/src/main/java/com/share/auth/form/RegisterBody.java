package com.share.auth.form;

/**
 * 用户注册对象
 *
 * @author share
 */
public class RegisterBody extends LoginBody
{
    /** 智问前端旧注册接口使用的手机号字段。 */
    private String cellPhone;

    /** 短信验证码字段，当前本地环境由兼容接口校验。 */
    private String code;

    public String getCellPhone()
    {
        return cellPhone;
    }

    public void setCellPhone(String cellPhone)
    {
        this.cellPhone = cellPhone;
    }

    public String getCode()
    {
        return code;
    }

    /** 邮箱字段，支持QQ邮箱注册。 */
    private String email;

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }
}
