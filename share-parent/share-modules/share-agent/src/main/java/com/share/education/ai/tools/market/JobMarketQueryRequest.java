package com.share.education.ai.tools.market;

import lombok.Data;

import java.io.Serializable;

/**
 * 招聘行情查询请求 DTO。
 */
@Data
public class JobMarketQueryRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 目标岗位或技术栈关键字，如 "大模型", "Java架构师", "DevOps" */
    private String roleOrKeyword;

    public JobMarketQueryRequest() {}

    public JobMarketQueryRequest(String roleOrKeyword) {
        this.roleOrKeyword = roleOrKeyword;
    }
}
