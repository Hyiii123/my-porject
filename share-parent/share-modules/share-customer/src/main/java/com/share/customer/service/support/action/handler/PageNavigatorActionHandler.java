package com.share.customer.service.support.action.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.customer.service.support.action.CustomerActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 页面穿梭与智能路由导航动作处理器
 */
@Slf4j
@Component
public class PageNavigatorActionHandler implements CustomerActionHandler {

    private final ObjectMapper objectMapper;

    public PageNavigatorActionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String content, String lower) {
        boolean hasNavVerb = lower.contains("去") || lower.contains("前往") || lower.contains("跳转")
                || lower.contains("打开") || lower.contains("进入") || lower.contains("导航") || lower.contains("带我去") || lower.contains("回");

        if (hasNavVerb) {
            if (lower.contains("设置") || lower.contains("密码") || lower.contains("个人中心")
                    || lower.contains("首页") || lower.contains("主页") || lower.contains("社区")
                    || lower.contains("问答") || lower.contains("搜索") || lower.contains("找课")) {
                return true;
            }
        }
        return lower.contains("去首页") || lower.contains("回首页") || lower.contains("打开页面")
                || lower.contains("个人设置页面") || lower.contains("修改密码");
    }

    @Override
    public String handle(String content, String lower, Long userId, String userName) {
        String targetRoute = "/personal/main/overview";
        String title = "个人中心";

        if (lower.contains("设置") || lower.contains("密码")) {
            targetRoute = "/personal/main/mySet";
            title = "个人设置";
        } else if (lower.contains("首页") || lower.contains("主页")) {
            targetRoute = "/main/index";
            title = "平台首页";
        } else if (lower.contains("问答") || lower.contains("提问")) {
            targetRoute = "/ask/index";
            title = "问答社区";
        } else if (lower.contains("搜索") || lower.contains("找课")) {
            targetRoute = "/search/index";
            title = "课程搜索";
        }

        try {
            Map<String, Object> card = new HashMap<>();
            card.put("action", "page_navigator");
            card.put("targetRoute", targetRoute);
            card.put("pageTitle", title);
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("🧭 **智能页面穿梭通道**\n\n");
            sb.append("已为您定位目标页面【").append(title).append("】。\n\n");
            sb.append("点击下方卡片即可立即直达：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成页面穿梭卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public int getOrder() {
        return 140;
    }
}
