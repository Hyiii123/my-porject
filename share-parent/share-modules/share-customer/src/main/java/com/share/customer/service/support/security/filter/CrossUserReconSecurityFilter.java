package com.share.customer.service.support.security.filter;

import com.share.customer.service.support.security.SecurityCheckContext;
import com.share.customer.service.support.security.SecurityCheckFilter;
import com.share.customer.service.support.security.SecurityFilterChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 责任链节点 2：跨租户与越权窥探防御过滤器 (Cross-User Reconnaissance Security Filter)
 */
@Slf4j
@Component
public class CrossUserReconSecurityFilter implements SecurityCheckFilter {

    private static final Pattern CROSS_USER_RECON_PATTERN = Pattern.compile(
            "(?:查|看|调取|修改|删除|查查|查下|查询|获取).*(?:用户|学员|账号)\\s*(?:id|编号)?\\s*[:：=]?\\s*\\d+",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public void doFilter(SecurityCheckContext context, SecurityFilterChain chain) {
        if (context == null) {
            return;
        }
        String lower = context.getLowerContent();
        if (!StringUtils.hasText(lower) && StringUtils.hasText(context.getContent())) {
            lower = context.getContent().toLowerCase();
            context.setLowerContent(lower);
        }

        if (StringUtils.hasText(lower) && isCrossUserAttempt(lower)) {
            context.setCrossUserAttempt(true);
            String userName = (String) context.getAttributes().getOrDefault("userName", "当前学员");
            Long userId = context.getUserId();
            String blockMsg = "🔒 **系统安全与数据隐私保护拦截**\n\n"
                    + "智问学伴系统启用了严格的**租户与用户数据隐私隔离防护机制**：\n"
                    + "• **安全准则**：系统严格遵循权限隔离规范，智能体仅有权操作和展示属于您当前认证账号的数据，**严禁跨账号访问或操作其他学员的订单、简历、考场记录或资产**；\n"
                    + "• **当前认证账户**：【" + (userName != null ? userName : "当前学员") + "】（用户 ID: " + (userId != null ? userId : "未知") + "）；\n\n"
                    + "智能体将仅为您本人处理属于您名下的业务。如需管理您本人的数据，请直接对我说“查我的订单”、“看我的面试报告”或“帮我诊断简历”。";
            context.block(blockMsg);
            log.warn("拦截跨用户越权窥探尝试: userId={}, content={}", userId, context.getContent());
            return; // 阻断责任链传递
        }

        chain.doFilter(context);
    }

    public boolean isCrossUserAttempt(String lower) {
        if (!StringUtils.hasText(lower)) {
            return false;
        }
        if (lower.contains("我的") || lower.contains("我自己的") || lower.contains("本人")) {
            return false;
        }
        if (lower.contains("其他人") || lower.contains("别的用户") || lower.contains("其他用户")
                || lower.contains("别的人") || lower.contains("他人的") || lower.contains("别人的")
                || lower.contains("查张三") || lower.contains("查李四") || lower.contains("查王五")) {
            return true;
        }
        return CROSS_USER_RECON_PATTERN.matcher(lower).find();
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
