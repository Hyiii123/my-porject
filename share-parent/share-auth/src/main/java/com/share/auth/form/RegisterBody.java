package com.share.auth.form;

/**
 * 用户注册对象
 *
 * @author share
 */
public class RegisterBody extends LoginBody
{
    /** 天机前端旧注册接口使用的手机号字段。 */
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

    public void setCode(String code)
    {
        this.code = code;
    }

}
