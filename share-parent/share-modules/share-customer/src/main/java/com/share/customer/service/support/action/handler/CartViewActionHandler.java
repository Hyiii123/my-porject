package com.share.customer.service.support.action.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.customer.service.support.action.CustomerActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 购物车资产查看动作处理器
 */
@Slf4j
@Component
public class CartViewActionHandler implements CustomerActionHandler {

    private final ObjectMapper objectMapper;

    public CartViewActionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String content, String lower) {
        String withoutCart = lower.replace("购物车", "").replace("购物车里", "").replace("购物车内", "");
        if (withoutCart.contains("加") || withoutCart.contains("进") || withoutCart.contains("放") || withoutCart.contains("买") || withoutCart.contains("购")) {
            return false;
        }
        return lower.contains("购物车") || lower.contains("车里有") || lower.contains("查看购物车") || lower.contains("清空购物车");
    }

    @Override
    public String handle(String content, String lower, Long userId, String userName) {
        try {
            Map<String, Object> card = new HashMap<>();
            card.put("action", "cart_view");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("🛒 **我的购物车资产清单**\n\n");
            sb.append("• **数据隔离**：购物车清单严格与您当前的登录凭证（用户 ID: ").append(userId).append("）绑定，仅您本人有权查看与结算。\n\n");
            sb.append("点击下方卡片即可直达购物车结算或挑选课程：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成购物车卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public int getOrder() {
        return 50;
    }
}
