package com.share.customer.service.support.action;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.share.customer.service.support.action.handler.*;
import com.share.customer.service.support.semantic.SemanticIntentRouter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 智能体自主操作卡片策略调度工厂 (Agent Action Card Strategy Factory)
 */
@Slf4j
@Component
public class CustomerActionFactory {

    private final List<CustomerActionHandler> handlers;
    private final SemanticIntentRouter semanticRouter;

    private final Map<SemanticIntentRouter.UserIntent, Class<? extends CustomerActionHandler>> intentHandlerMap = Map.ofEntries(
        Map.entry(SemanticIntentRouter.UserIntent.SIGN_IN, SignInActionHandler.class),
        Map.entry(SemanticIntentRouter.UserIntent.PAGE_NAVIGATOR, PageNavigatorActionHandler.class),
        Map.entry(SemanticIntentRouter.UserIntent.COUPON_CENTER, CouponCenterActionHandler.class),
        Map.entry(SemanticIntentRouter.UserIntent.CART_VIEW, CartViewActionHandler.class),
        Map.entry(SemanticIntentRouter.UserIntent.RESUME_DIAGNOSIS, ResumeActionHandler.class),
        Map.entry(SemanticIntentRouter.UserIntent.LEARNING_PORTRAIT, LearningPortraitActionHandler.class),
        Map.entry(SemanticIntentRouter.UserIntent.POINTS_RANKING, PointsRankingActionHandler.class),
        Map.entry(SemanticIntentRouter.UserIntent.EXAM_QUERY, ExamQueryActionHandler.class),
        Map.entry(SemanticIntentRouter.UserIntent.INTERVIEW_MOCK, InterviewLaunchActionHandler.class),
        Map.entry(SemanticIntentRouter.UserIntent.COURSE_PURCHASE, CoursePurchaseActionHandler.class)
    );

    public CustomerActionFactory(List<CustomerActionHandler> handlers,
                                 @org.springframework.beans.factory.annotation.Autowired(required = false) SemanticIntentRouter semanticRouter) {
        this.handlers = handlers.stream()
                .sorted(Comparator.comparingInt(CustomerActionHandler::getOrder))
                .collect(Collectors.toList());
        this.semanticRouter = semanticRouter;
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

        // 1. 优先采用 BAAI 向量嵌入语义意图路由
        if (semanticRouter != null) {
            SemanticIntentRouter.UserIntent intent = semanticRouter.routeIntent(content);
            if (intent == SemanticIntentRouter.UserIntent.PATH_PLANNING) {
                // 属于多智能体导学推演，放行至教育推演引擎
                return null;
            }
            Class<? extends CustomerActionHandler> targetClass = intentHandlerMap.get(intent);
            if (targetClass != null) {
                for (CustomerActionHandler handler : handlers) {
                    if (targetClass.isInstance(handler)) {
                        // 防御性校验：虽然语义向量命中意图，但对应 Handler 仍须进行语义特征与关键字规则二次确认，防止向量越界误判
                        if (!handler.supports(content, lowerContent)) {
                            log.warn("【语义向量路由】意图命中 [{}] 但对应处理器 [{}] 规则校验未通过，放弃派发并放行",
                                    intent, handler.getClass().getSimpleName());
                            continue;
                        }
                        log.info("【语义向量路由】成功命中动作卡片策略: intent={}, handler={}", intent, handler.getClass().getSimpleName());
                        try {
                            String result = handler.handle(content, lowerContent, userId, userName);
                            if (StringUtils.hasText(result)) {
                                return result;
                            }
                        } catch (Exception ex) {
                            log.error("语义策略处理器 [{}] 执行异常: {}", handler.getClass().getSimpleName(), ex.getMessage(), ex);
                        }
                    }
                }
            }
        }

        // 2. 降级走常规规则责任链匹配
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
