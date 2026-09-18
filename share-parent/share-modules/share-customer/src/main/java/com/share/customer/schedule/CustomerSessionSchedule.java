package com.share.customer.schedule;

import com.share.customer.config.CustomerSessionProperties;
import com.share.customer.service.ICustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 客服会话定时巡检与生命周期治理调度任务。
 *
 * <p>按照业务要求每 2 天排查一次，清理超过 2 天无对话的活跃会话。</p>
 */
@Slf4j
@Component
public class CustomerSessionSchedule {

    private final ICustomerService customerService;
    private final CustomerSessionProperties sessionProperties;

    public CustomerSessionSchedule(ICustomerService customerService, CustomerSessionProperties sessionProperties) {
        this.customerService = customerService;
        this.sessionProperties = sessionProperties;
    }

    /**
     * 每 2 天排查一次，自动清理超过指定天数（默认 2 天）无对话的活跃会话。
     * Cron 表达式可通过 customer.session.clean-cron 配置，默认 "0 0 2 * / 2 * ?"。
     */
    @Scheduled(cron = "${customer.session.clean-cron:0 0 2 */2 * ?}")
    public void scheduleCleanExpiredActiveSessions() {
        if (!sessionProperties.isAutoCleanEnabled()) {
            log.debug("【客服活跃会话巡检】自动清理任务已配置为关闭，跳过本次执行");
            return;
        }
        try {
            int expireDays = sessionProperties.getExpireDays() > 0 ? sessionProperties.getExpireDays() : 2;
            log.info("【客服活跃会话巡检】开始执行例行排查任务，清理阈值: 超过 {} 天无对话...", expireDays);
            int cleanedCount = customerService.cleanExpiredActiveSessions(expireDays);
            log.info("【客服活跃会话巡检】巡检完成，本次共自动清理 {} 个过期活跃会话", cleanedCount);
        } catch (Exception e) {
            log.error("【客服活跃会话巡检】执行自动清理异常: {}", e.getMessage(), e);
        }
    }
}
