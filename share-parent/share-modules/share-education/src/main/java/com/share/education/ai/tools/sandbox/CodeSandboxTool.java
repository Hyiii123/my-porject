package com.share.education.ai.tools.sandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * 智能体代码沙箱执行工具 (Spring AI Tool Callback)。
 * 支持 LLM 自主生成代码并直接在隔离沙箱中试跑、验证算法正确性并分析报错。
 */
@Component("codeSandboxTool")
public class CodeSandboxTool implements Function<CodeExecutionRequest, CodeExecutionResult> {

    private static final Logger log = LoggerFactory.getLogger(CodeSandboxTool.class);
    private final JavaSandboxRunner runner;

    public CodeSandboxTool(JavaSandboxRunner runner) {
        this.runner = runner;
    }

    @Override
    public CodeExecutionResult apply(CodeExecutionRequest request) {
        log.info("[CodeSandboxTool] 收到智能体代码执行申请, language: {}, length: {}",
                request.getLanguage(), request.getSourceCode() != null ? request.getSourceCode().length() : 0);
        return runner.execute(request);
    }
}
