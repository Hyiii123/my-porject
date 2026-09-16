package com.share.customer.service.support.action;

/**
 * 智能体自主操作卡片策略处理器契约 (Agent Action Card Handler Strategy)
 */
public interface CustomerActionHandler {

    /**
     * 判断当前策略是否匹配用户意图
     *
     * @param content      用户输入的原始内容
     * @param lowerContent 转为小写的文本，便于大小写无关匹配
     * @return 是否支持处理该意图
     */
    boolean supports(String content, String lowerContent);

    /**
     * 执行动作并返回包含卡片协议的完整回复内容
     *
     * @param content      用户输入的原始内容
     * @param lowerContent 转为小写的文本
     * @param userId       当前用户 ID
     * @param userName     当前用户名
     * @return 智能体回复字符串（含 [AGENT_ACTION_CARD:{...}] 协议），失败或不适用时返回 null
     */
    String handle(String content, String lowerContent, Long userId, String userName);

    /**
     * 策略优先级（数值越小优先级越高，按序匹配）
     */
    default int getOrder() {
        return 100;
    }
}
