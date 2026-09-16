package com.share.customer.service.support.action.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.customer.service.support.action.CustomerActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 个人订单与交易管理动作处理器
 */
@Slf4j
@Component
public class OrderManageActionHandler implements CustomerActionHandler {

    private final ObjectMapper objectMapper;

    public OrderManageActionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String content, String lower) {
        return lower.contains("订单") || lower.contains("我的订单") || lower.contains("查订单")
                || lower.contains("买了什么") || lower.contains("买的课") || lower.contains("待付款")
                || lower.contains("退款") || lower.contains("退课") || lower.contains("取消订单");
    }

    @Override
    public String handle(String content, String lower, Long userId, String userName) {
        try {
            Map<String, Object> card = new HashMap<>();
            card.put("action", "order_manage");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("📦 **个人订单与交易管理中枢**\n\n");
            sb.append("• **防越权安全保障**：系统已严格绑定当前登录用户（ID: ").append(userId).append("），所有订单数据与售后操作均受行级权限保护，无法操作他人资产。\n");
            sb.append("• **操作支持**：一键查看我的全部订单列表、处理待支付订单极速结账、以及课程售后退款申请。\n\n");
            sb.append("点击下方卡片即可一键直达您的专属订单管理大厅：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成订单管理卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public int getOrder() {
        return 30;
    }
}
