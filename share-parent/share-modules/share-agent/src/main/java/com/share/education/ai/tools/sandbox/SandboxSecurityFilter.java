package com.share.education.ai.tools.sandbox;

import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 沙箱词法与安全拦截门禁。
 * 严格阻断提权、进程派生、文件破坏、动态反射逃逸、网络外联与命令执行代码。
 */
public final class SandboxSecurityFilter {

    private static final List<String> FORBIDDEN_GLOBAL = List.of(
            "System.exit",
            "Runtime.getRuntime",
            "ProcessBuilder",
            "sun.misc.Unsafe",
            "java.lang.reflect",
            "java.lang.invoke",
            "Class.forName",
            "ClassLoader",
            "URLClassLoader",
            "java.io.File",
            "java.io.FileInputStream",
            "java.io.FileOutputStream",
            "java.io.RandomAccessFile",
            "java.io.FileReader",
            "java.io.FileWriter",
            "java.nio.file",
            "java.nio.channels",
            "java.net.Socket",
            "java.net.ServerSocket",
            "java.net.URL",
            "java.net.URI",
            "java.net.HttpURLConnection",
            "java.net.http",
            "Thread.stop",
            "ThreadGroup",
            "SecurityManager",
            "System.load",
            "System.loadLibrary",
            "exec(",
            "fork(",
            "eval(",
            "/bin/sh",
            "/bin/bash",
            "cmd.exe",
            "powershell",
            "bash -i",
            "nc -",
            "ncat "
    );

    private static final List<String> FORBIDDEN_PYTHON = List.of(
            "__subclasses__",
            "__builtins__",
            "__globals__",
            "__class__",
            "__base__",
            "__bases__",
            "__mro__",
            "__code__",
            "sys.modules",
            "__import__",
            "importlib",
            "os.system",
            "os.popen",
            "os.spawn",
            "subprocess.",
            "shutil.",
            "open(",
            "getattr(",
            "setattr(",
            "delattr(",
            "compile(",
            "breakpoint(",
            "pty.",
            "socket.",
            "requests.",
            "urllib.",
            "http.client",
            "commands.",
            "ctypes"
    );

    private static final List<String> FORBIDDEN_JS = List.of(
            "child_process",
            "process.",
            "process[",
            "require(",
            "require.",
            "import(",
            "fs.",
            "fs/",
            "net.",
            "http.",
            "https.",
            "cluster.",
            "worker_threads",
            "WebSocket",
            "fetch(",
            "XMLHttpRequest",
            "Function(",
            "global.",
            "globalThis.",
            "constructor.constructor"
    );

    private SandboxSecurityFilter() {}

    public static void validate(String code, String language) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("待执行代码不能为空");
        }
        if (code.length() > 50000) {
            throw new IllegalArgumentException("代码长度超出 50000 字符安全限制");
        }

        String normalizedLang = language != null ? language.trim().toLowerCase() : "java";

        // 1. 全局危险词法审计
        for (String literal : FORBIDDEN_GLOBAL) {
            if (code.contains(literal)) {
                throw new SecurityException("检测到高危代码或敏感系统包调用: " + literal);
            }
        }

        // 2. Python 专属动态反射与逃逸防护
        if ("python".equals(normalizedLang) || "py".equals(normalizedLang)) {
            for (String literal : FORBIDDEN_PYTHON) {
                if (code.contains(literal)) {
                    throw new SecurityException("检测到 Python 沙箱逃逸或敏感系统调用: " + literal);
                }
            }
        }

        // 3. JavaScript / Node 专属进程派生与文件系统逃逸防护
        if ("javascript".equals(normalizedLang) || "js".equals(normalizedLang) || "node".equals(normalizedLang)) {
            for (String literal : FORBIDDEN_JS) {
                if (code.contains(literal)) {
                    throw new SecurityException("检测到 Node.js 沙箱逃逸或进程派生调用: " + literal);
                }
            }
        }
    }
}
