package com.share.customer.service.support.action.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.customer.service.support.action.CustomerActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 优惠券领取与福利中心动作处理器
 */
@Slf4j
@Component
public class CouponCenterActionHandler implements CustomerActionHandler {

    private final ObjectMapper objectMapper;

    public CouponCenterActionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String content, String lower) {
        return lower.contains("优惠券") || lower.contains("领券") || lower.contains("卡券")
                || lower.contains("减免") || lower.contains("打折券") || lower.contains("兑换码");
    }

    @Override
    public String handle(String content, String lower, Long userId, String userName) {
        try {
            Map<String, Object> card = new HashMap<>();
            card.put("action", "coupon_center");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("🎟️ **优惠券与专属权益中心**\n\n");
            sb.append("• **权益归属**：系统已匹配当前认证账户（ID: ").append(userId).append("）可领取的平台热门好券，领取后直接绑定您个人卡券包，购课结算时自动按最优策略抵扣现金。\n\n");
            sb.append("点击下方卡片即可一键直达领券中心或查看我的卡券：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成优惠券卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public int getOrder() {
        return 40;
    }
}
