package com.share.customer.service.support.action;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 智能体自主操作卡片策略调度工厂 (Agent Action Card Strategy Factory)
 */
@Slf4j
@Component
public class CustomerActionFactory {

    private final List<CustomerActionHandler> handlers;

    public CustomerActionFactory(List<CustomerActionHandler> handlers) {
        this.handlers = handlers.stream()
                .sorted(Comparator.comparingInt(CustomerActionHandler::getOrder))
                .collect(Collectors.toList());
        log.info("【CustomerActionFactory】成功装配全域智能体动作卡片策略处理器，共加载 {} 个策略: {}",
                this.handlers.size(),
                this.handlers.stream().map(h -> h.getClass().getSimpleName() + "(order=" + h.getOrder() + ")").collect(Collectors.toList()));
    }

    /**
     * 根据输入意图动态匹配并派发至对应的动作卡片策略处理器
     *
     * @param content      用户原始文本
     * @param lowerContent 转小写的文本
     * @param userId       当前用户 ID
     * @param userName     当前用户名
     * @return 生成的卡片及文本回复，无匹配策略或处理失败返回 null
     */
    public String dispatch(String content, String lowerContent, Long userId, String userName) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        for (CustomerActionHandler handler : handlers) {
            try {
                if (handler.supports(content, lowerContent)) {
                    log.debug("命中动作卡片策略: {}", handler.getClass().getSimpleName());
                    String result = handler.handle(content, lowerContent, userId, userName);
                    if (StringUtils.hasText(result)) {
                        return result;
                    }
                }
            } catch (Exception ex) {
                log.error("策略处理器 [{}] 执行异常: {}", handler.getClass().getSimpleName(), ex.getMessage(), ex);
            }
        }
        return null;
    }

    public List<CustomerActionHandler> getHandlers() {
        return handlers;
    }
}
