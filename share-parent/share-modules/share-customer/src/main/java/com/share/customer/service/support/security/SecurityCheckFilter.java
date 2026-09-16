package com.share.customer.service.support.security;

/**
 * 安全检查过滤器契约 (Security Check Filter Interface)
 */
public interface SecurityCheckFilter {

    /**
     * 执行当前安全防线节点核验
     */
    void doFilter(SecurityCheckContext context, SecurityFilterChain chain);

    /**
     * 过滤器优先级序号（越小越优先执行）
     */
    default int getOrder() {
        return 100;
    }
}
