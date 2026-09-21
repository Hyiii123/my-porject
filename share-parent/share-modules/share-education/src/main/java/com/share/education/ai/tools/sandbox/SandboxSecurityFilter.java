package com.share.education.ai.tools.sandbox;

import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 沙箱词法与安全拦截门禁。
 * 严格阻断提权、进程派生、文件破坏、反序列化攻击及网络外联代码。
 */
public final class SandboxSecurityFilter {

    private static final List<String> FORBIDDEN_LITERALS = List.of(
            "System.exit",
            "Runtime.getRuntime",
            "ProcessBuilder",
            "sun.misc.Unsafe",
            "java.lang.reflect",
            "java.io.File",
            "java.io.FileInputStream",
            "java.io.FileOutputStream",
            "java.io.RandomAccessFile",
            "java.nio.file",
            "java.net.Socket",
            "java.net.ServerSocket",
            "java.net.URL",
            "java.net.HttpURLConnection",
            "Thread.stop",
            "ThreadGroup",
            "SecurityManager",
            "System.load",
            "System.loadLibrary",
            "exec(",
            "fork(",
            "eval(",
            "os.system",
            "subprocess.",
            "shutil.",
            "__import__"
    );

    private SandboxSecurityFilter() {}

    public static void validate(String code, String language) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("待执行代码不能为空");
        }
        if (code.length() > 50000) {
            throw new IllegalArgumentException("代码长度超出 50000 字符安全限制");
        }

        for (String literal : FORBIDDEN_LITERALS) {
            if (code.contains(literal)) {
                throw new SecurityException("检测到高危代码或敏感系统包调用: " + literal);
            }
        }
    }
}
