package com.share.customer.service.support.security;

/**
 * 安全检查责任链执行契约 (Security Filter Chain Interface)
 */
public interface SecurityFilterChain {

    /**
     * 驱动下一个责任链节点执行过滤
     */
    void doFilter(SecurityCheckContext context);
}
