package com.share.customer.schedule;

import com.share.customer.config.CustomerSessionProperties;
import com.share.customer.mapper.CustomerSessionMapper;
import com.share.customer.service.ICustomerService;
import com.share.customer.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * 客服活跃会话超过两天自动删除与每两天排查定时任务单元测试。
 */
class CustomerSessionScheduleTest {

    @Test
    void testCronExpressionEveryTwoDays() {
        // 验证 Cron 表达式符合 Spring 规范
        String cron = "0 0 2 */2 * ?";
        assertThat(CronExpression.isValidExpression(cron)).isTrue();

        CronExpression expression = CronExpression.parse(cron);
        assertThat(expression).isNotNull();

        // 验证下次触发时间
        LocalDateTime base = LocalDateTime.of(2026, 9, 18, 0, 0, 0);
        LocalDateTime next = expression.next(base);
        assertThat(next).isNotNull();
        assertThat(next.getHour()).isEqualTo(2);
        assertThat(next.getMinute()).isEqualTo(0);
        assertThat(next.getSecond()).isEqualTo(0);
    }

    @Test
    void testSessionPropertiesDefaultValues() {
        CustomerSessionProperties properties = new CustomerSessionProperties();
        assertThat(properties.getExpireDays()).isEqualTo(2);
        assertThat(properties.getCleanCron()).isEqualTo("0 0 2 */2 * ?");
        assertThat(properties.isAutoCleanEnabled()).isTrue();
    }

    @Test
    void testScheduleInvocation() {
        ICustomerService customerService = mock(ICustomerService.class);
        CustomerSessionProperties properties = new CustomerSessionProperties();
        when(customerService.cleanExpiredActiveSessions(2)).thenReturn(5);

        CustomerSessionSchedule schedule = new CustomerSessionSchedule(customerService, properties);
        schedule.scheduleCleanExpiredActiveSessions();

        verify(customerService, times(1)).cleanExpiredActiveSessions(2);
    }

    @Test
    void testScheduleDisabled() {
        ICustomerService customerService = mock(ICustomerService.class);
        CustomerSessionProperties properties = new CustomerSessionProperties();
        properties.setAutoCleanEnabled(false);

        CustomerSessionSchedule schedule = new CustomerSessionSchedule(customerService, properties);
        schedule.scheduleCleanExpiredActiveSessions();

        verify(customerService, never()).cleanExpiredActiveSessions(anyInt());
    }

    @Test
    void testServiceImplCleanExpiredActiveSessions() {
        CustomerSessionMapper sessionMapper = mock(CustomerSessionMapper.class);
        CustomerSessionProperties properties = new CustomerSessionProperties();
        List<Long> mockIds = List.of(101L, 102L, 103L);
        when(sessionMapper.selectExpiredActiveSessionIds(any(LocalDateTime.class), isNull())).thenReturn(mockIds);

        CustomerServiceImpl service = new CustomerServiceImpl(
                null, null, sessionMapper, null, null, null, null, null,
                null, properties, null, null, null, null, null, null
        );

        int count = service.cleanExpiredActiveSessions(2);
        assertThat(count).isEqualTo(3);
        verify(sessionMapper, times(1)).deleteBatchIds(mockIds);
    }

    @Test
    void testServiceImplCleanExpiredActiveSessionsForUser() {
        CustomerSessionMapper sessionMapper = mock(CustomerSessionMapper.class);
        CustomerSessionProperties properties = new CustomerSessionProperties();
        List<Long> mockIds = List.of(201L);
        when(sessionMapper.selectExpiredActiveSessionIds(any(LocalDateTime.class), eq(88L))).thenReturn(mockIds);

        CustomerServiceImpl service = new CustomerServiceImpl(
                null, null, sessionMapper, null, null, null, null, null,
                null, properties, null, null, null, null, null, null
        );

        int count = service.cleanExpiredActiveSessionsForUser(88L, 2);
        assertThat(count).isEqualTo(1);
        verify(sessionMapper, times(1)).deleteBatchIds(mockIds);
    }

    @Test
    void testServiceImplCleanWhenNoExpiredSessions() {
        CustomerSessionMapper sessionMapper = mock(CustomerSessionMapper.class);
        when(sessionMapper.selectExpiredActiveSessionIds(any(LocalDateTime.class), isNull())).thenReturn(List.of());

        CustomerServiceImpl service = new CustomerServiceImpl(
                null, null, sessionMapper, null, null, null, null, null,
                null, new CustomerSessionProperties(), null, null, null, null, null, null
        );

        int count = service.cleanExpiredActiveSessions(2);
        assertThat(count).isEqualTo(0);
        verify(sessionMapper, never()).deleteBatchIds(any());
    }
}
