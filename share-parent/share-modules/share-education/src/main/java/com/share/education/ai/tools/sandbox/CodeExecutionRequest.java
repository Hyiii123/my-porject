package com.share.education.ai.tools.sandbox;

import lombok.Data;

import java.io.Serializable;

/**
 * 沙箱代码执行请求 DTO。
 */
@Data
public class CodeExecutionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 待执行源代码 */
    private String sourceCode;

    /** 编程语言: java, python, javascript */
    private String language = "java";

    /** 标准输入 stdin (可选) */
    private String stdin;

    /** 超时时间毫秒 (默认 3000ms，最高 5000ms) */
    private Integer timeoutMs = 3000;

    public CodeExecutionRequest() {}

    public CodeExecutionRequest(String sourceCode, String language) {
        this.sourceCode = sourceCode;
        this.language = language;
    }
}
