package com.share.common.core.utils.sql;

import com.share.common.core.exception.UtilException;
import com.share.common.core.utils.StringUtils;

/**
 * sql操作工具类
 *
 * @author share
 */
public class SqlUtil
{
    /**
     * 定义常用的 sql关键字
     */
    public static String SQL_REGEX = "and |extractvalue|updatexml|exec |insert |select |delete |update |drop |count |chr |mid |master |truncate |char |declare |or |+|user()";

    /**
     * 仅支持字母、数字、下划线、空格、逗号、小数点（支持多个字段排序）
     */
    public static String SQL_PATTERN = "[a-zA-Z0-9_\\ \\,\\.]+";

    /**
     * 限制orderBy最大长度
     */
    private static final int ORDER_BY_MAX_LENGTH = 500;

    /**
     * 检查字符，防止注入绕过
     */
    public static String escapeOrderBySql(String value)
    {
        if (StringUtils.isNotEmpty(value) && !isValidOrderBySql(value))
        {
            throw new UtilException("参数不符合规范，不能进行查询");
        }
        if (StringUtils.length(value) > ORDER_BY_MAX_LENGTH)
        {
            throw new UtilException("参数已超过最大限制，不能进行查询");
        }
        return value;
    }

    /**
     * 危险 SQL 关键字与注释符号正则（防御大小写混合、注释符、换行符及敏感内置函数逃逸）
     */
    private static final java.util.regex.Pattern DANGEROUS_SQL_PATTERN = java.util.regex.Pattern.compile(
            "(?i)\\b(and|or|exec|execute|insert|select|delete|update|drop|truncate|declare|grant|revoke|union|sleep|benchmark)\\b|[;'\"]|/\\*|\\*/|--|#"
    );

    /**
     * 验证 order by 语法是否符合规范
     */
    public static boolean isValidOrderBySql(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return true;
        }
        return value.matches(SQL_PATTERN) && !DANGEROUS_SQL_PATTERN.matcher(value).find();
    }

    /**
     * SQL关键字检查
     */
    public static void filterKeyword(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return;
        }
        if (DANGEROUS_SQL_PATTERN.matcher(value).find())
        {
            throw new UtilException("参数存在SQL注入风险");
        }
    }
}
