package com.share.customer.schedule;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.share.common.core.context.SecurityContextHolder;
import com.share.customer.config.CustomerSessionProperties;
import com.share.customer.domain.CustomerSession;
import com.share.customer.mapper.CustomerSessionMapper;
import com.share.customer.service.ICustomerService;
import com.share.customer.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

        // 验证下下次触发时间间隔为 2 天 (48 小时)
        LocalDateTime nextNext = expression.next(next);
        assertThat(nextNext).isNotNull();
        assertThat(nextNext.getHour()).isEqualTo(2);
        assertThat(nextNext.getDayOfMonth() - next.getDayOfMonth()).isEqualTo(2);
    }

    @Test
    void testCronExpressionSequence() {
        CronExpression expression = CronExpression.parse("0 0 2 */2 * ?");
        LocalDateTime current = LocalDateTime.of(2026, 9, 1, 0, 0, 0);
        List<Integer> triggerDays = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            current = expression.next(current);
            triggerDays.add(current.getDayOfMonth());
        }
        // 验证每 2 天执行一次（1号, 3号, 5号, 7号, 9号）
        assertThat(triggerDays).containsExactly(1, 3, 5, 7, 9);
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
    void testServiceImplCleanExpiredActiveSessionsWithBatching() {
        CustomerSessionMapper sessionMapper = mock(CustomerSessionMapper.class);
        CustomerSessionProperties properties = new CustomerSessionProperties();
        // 构造超过 200 条的 ID 列表，验证分片批处理（205 条应分两批：200 + 5）
        List<Long> mockIds = new ArrayList<>();
        for (long i = 1; i <= 205; i++) {
            mockIds.add(1000L + i);
        }
        when(sessionMapper.selectExpiredActiveSessionIds(any(LocalDateTime.class), isNull())).thenReturn(mockIds);

        CustomerServiceImpl service = new CustomerServiceImpl(
                null, null, sessionMapper, null, null, null, null, null,
                null, properties, null, null, null, null, null, null
        );

        int count = service.cleanExpiredActiveSessions(2);
        assertThat(count).isEqualTo(205);
        // 验证分片两次调用
        verify(sessionMapper, times(2)).deleteBatchIds(anyList());
    }

    @Test
    void testServiceImplCleanExpiredActiveSessionsForUserWithBatching() {
        CustomerSessionMapper sessionMapper = mock(CustomerSessionMapper.class);
        CustomerSessionProperties properties = new CustomerSessionProperties();
        // 构造 210 条数据验证学员端分批删除安全机制
        List<Long> mockIds = new ArrayList<>();
        for (long i = 1; i <= 210; i++) {
            mockIds.add(2000L + i);
        }
        when(sessionMapper.selectExpiredActiveSessionIds(any(LocalDateTime.class), eq(88L))).thenReturn(mockIds);

        CustomerServiceImpl service = new CustomerServiceImpl(
                null, null, sessionMapper, null, null, null, null, null,
                null, properties, null, null, null, null, null, null
        );

        int count = service.cleanExpiredActiveSessionsForUser(88L, 2);
        assertThat(count).isEqualTo(210);
        verify(sessionMapper, times(2)).deleteBatchIds(anyList());
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

    @Test
    void testCleanExpiredActiveSessionsFallbackToProperties() {
        CustomerSessionMapper sessionMapper = mock(CustomerSessionMapper.class);
        CustomerSessionProperties properties = new CustomerSessionProperties();
        properties.setExpireDays(3); // 自定义默认 3 天
        when(sessionMapper.selectExpiredActiveSessionIds(any(LocalDateTime.class), isNull())).thenReturn(List.of(99L));

        CustomerServiceImpl service = new CustomerServiceImpl(
                null, null, sessionMapper, null, null, null, null, null,
                null, properties, null, null, null, null, null, null
        );

        // 传 0 时应回退到 properties 配置的 3 天
        int count = service.cleanExpiredActiveSessions(0);
        assertThat(count).isEqualTo(1);
        verify(sessionMapper, times(1)).deleteBatchIds(List.of(99L));
    }

    @Test
    void testListMySessionsRespectsAutoCleanDisabled() {
        CustomerSessionMapper sessionMapper = mock(CustomerSessionMapper.class);
        CustomerSessionProperties properties = new CustomerSessionProperties();
        properties.setAutoCleanEnabled(false); // 明确关闭自动清理开关

        Page<CustomerSession> mockPage = new Page<>(1, 10);
        when(sessionMapper.selectPage(any(), any())).thenReturn(mockPage);

        CustomerServiceImpl service = new CustomerServiceImpl(
                null, null, sessionMapper, null, null, null, null, null,
                null, properties, null, null, null, null, null, null
        );

        try {
            com.share.common.core.context.SecurityContextHolder.setUserId("1001");

            IPage<CustomerSession> result = service.listMySessions(1, 10, null);
            assertThat(result).isNotNull();

            // 验证当 autoCleanEnabled = false 时，不会触发 selectExpiredActiveSessionIds 查询或删除
            verify(sessionMapper, never()).selectExpiredActiveSessionIds(any(), any());
            verify(sessionMapper, never()).deleteBatchIds(any());
        } finally {
            com.share.common.core.context.SecurityContextHolder.remove();
        }
    }
}
