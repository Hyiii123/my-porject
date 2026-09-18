package com.share.customer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 客服会话生命周期与自动排查清理配置。
 *
 * <p>支持配置无对话超时天数与定时排查 cron 表达式，默认超过 2 天无对话自动删除，且每 2 天排查一次。</p>
 */
@Data
@ConfigurationProperties(prefix = "customer.session")
public class CustomerSessionProperties {

    /**
     * 活跃会话无对话超时天数（超过此天数无任何对话，自动删除），默认 2 天。
     */
    private int expireDays = 2;

    /**
     * 活跃会话定时排查清理 Cron 表达式（默认每 2 天执行一次，如每天凌晨 2 点每 2 天执行：0 0 2 * / 2 * ?）。
     */
    private String cleanCron = "0 0 2 */2 * ?";

    /**
     * 是否开启定时清理任务，默认 true 开启。
     */
    private boolean autoCleanEnabled = true;
}
