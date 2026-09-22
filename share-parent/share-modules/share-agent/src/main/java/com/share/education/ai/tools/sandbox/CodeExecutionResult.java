package com.share.education.ai.tools.sandbox;

import lombok.Data;

import java.io.Serializable;

/**
 * 沙箱代码执行输出结果 DTO。
 */
@Data
public class CodeExecutionResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 执行状态: SUCCESS, COMPILE_ERROR, RUNTIME_ERROR, TIMEOUT, SECURITY_VIOLATION */
    private String status;

    /** 标准输出 stdout */
    private String stdout;

    /** 错误输出 stderr */
    private String stderr;

    /** 运行耗时毫秒 */
    private Long executionTimeMs;

    /** 退出代码 (0 正常) */
    private Integer exitCode;

    public CodeExecutionResult() {}

    public static CodeExecutionResult success(String stdout, long timeMs) {
        CodeExecutionResult r = new CodeExecutionResult();
        r.setStatus("SUCCESS");
        r.setStdout(stdout != null ? stdout : "");
        r.setStderr("");
        r.setExecutionTimeMs(timeMs);
        r.setExitCode(0);
        return r;
    }

    public static CodeExecutionResult compileError(String stderr) {
        CodeExecutionResult r = new CodeExecutionResult();
        r.setStatus("COMPILE_ERROR");
        r.setStdout("");
        r.setStderr(stderr);
        r.setExecutionTimeMs(0L);
        r.setExitCode(1);
        return r;
    }

    public static CodeExecutionResult runtimeError(String stderr, long timeMs) {
        CodeExecutionResult r = new CodeExecutionResult();
        r.setStatus("RUNTIME_ERROR");
        r.setStdout("");
        r.setStderr(stderr);
        r.setExecutionTimeMs(timeMs);
        r.setExitCode(2);
        return r;
    }

    public static CodeExecutionResult timeout(long timeMs) {
        CodeExecutionResult r = new CodeExecutionResult();
        r.setStatus("TIMEOUT");
        r.setStdout("");
        r.setStderr("执行超时 (限制 " + timeMs + "ms)，已被安全沙箱熔断终止");
        r.setExecutionTimeMs(timeMs);
        r.setExitCode(124);
        return r;
    }

    public static CodeExecutionResult securityViolation(String reason) {
        CodeExecutionResult r = new CodeExecutionResult();
        r.setStatus("SECURITY_VIOLATION");
        r.setStdout("");
        r.setStderr("【安全阻断】" + reason);
        r.setExecutionTimeMs(0L);
        r.setExitCode(126);
        return r;
    }
}
