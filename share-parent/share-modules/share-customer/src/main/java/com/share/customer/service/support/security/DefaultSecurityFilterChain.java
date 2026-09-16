package com.share.customer.service.support.security;

import java.util.List;

/**
 * 默认安全检查责任链执行器 (Default Security Filter Chain)
 * <p>
 * 遵循 GoF 责任链模式 (Chain of Responsibility)，按优先级顺序驱动各安全过滤节点执行。
 */
public class DefaultSecurityFilterChain implements SecurityFilterChain {

    private final List<SecurityCheckFilter> filters;
    private int index = 0;

    public DefaultSecurityFilterChain(List<SecurityCheckFilter> filters) {
        this.filters = filters != null ? filters : List.of();
    }

    @Override
    public void doFilter(SecurityCheckContext context) {
        if (context == null || context.isBlocked()) {
            return;
        }
        if (index < filters.size()) {
            SecurityCheckFilter filter = filters.get(index++);
            filter.doFilter(context, this);
        }
    }
}
