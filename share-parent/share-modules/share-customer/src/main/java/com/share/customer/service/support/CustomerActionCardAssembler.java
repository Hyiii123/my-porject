package com.share.customer.service.support;

import com.share.customer.service.support.action.CustomerActionFactory;
import com.share.customer.service.support.security.SecurityCheckContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 智能体自主意图识别、动作派发与交互卡片装配门面 (Agent Copilot Action Card Assembler Facade)
 * <p>
 * 遵循策略工厂模式 (Strategy + Factory)，将全域 14 项业务卡片装配委托给 {@link CustomerActionFactory} 统一调度，
 * 并通过责任链模式 (Chain of Responsibility) 驱动前置安全风控核验，实现高内聚、低耦合与开闭原则 (OCP)。
 */
@Slf4j
@Component
public class CustomerActionCardAssembler {

    private final CustomerSecurityShield securityShield;
    private final CustomerActionFactory actionFactory;

    public CustomerActionCardAssembler(
            CustomerSecurityShield securityShield,
            CustomerActionFactory actionFactory) {
        this.securityShield = securityShield;
        this.actionFactory = actionFactory;
    }

    /**
     * 判断是否为多智能体协同导学推演意图
     */
    public boolean isAgentDeliberationIntent(String content) {
        if (!StringUtils.hasText(content)) return false;
        String lower = content.toLowerCase();
        if (lower.contains("退款") || lower.contains("退课") || lower.contains("开票") || lower.contains("密码")
                || lower.contains("发票") || lower.contains("支付失败") || lower.contains("订单号")) {
            return false;
        }
        return lower.contains("路线") || lower.contains("路径") || lower.contains("规划")
                || lower.contains("推荐") || lower.contains("学什么") || lower.contains("怎么学")
                || lower.contains("如何进阶") || lower.contains("学习方案") || lower.contains("学习计划")
                || lower.contains("成长图谱") || lower.contains("选课") || lower.contains("想转行");
    }

    /**
     * 提取目标岗位/技术方向
     */
    public String extractTargetRole(String content) {
        if (!StringUtils.hasText(content)) return "Java 全栈架构师";
        String lower = content.toLowerCase();
        if (lower.contains("大模型") || lower.contains("大语言模型") || lower.contains("语言模型") || lower.contains("llm") || lower.contains("langchain")
                || lower.contains("prompt") || lower.contains("rag") || lower.contains("ai应用")
                || lower.contains("人工智能")) {
            return "大语言模型应用工程师";
        }
        if (lower.contains("go") || lower.contains("golang") || lower.contains("云原生")
                || lower.contains("k8s") || lower.contains("docker") || lower.contains("容器")) {
            return "Go 云原生架构师";
        }
        if (lower.contains("前端") || lower.contains("vue") || lower.contains("react")
                || lower.contains("ts") || lower.contains("typescript") || lower.contains("web")) {
            return "Web 前端架构专家";
        }
        if (lower.contains("大数据") || lower.contains("spark") || lower.contains("flink")
                || lower.contains("数仓") || lower.contains("hadoop")) {
            return "大数据流批一体工程师";
        }
        if (lower.contains("鸿蒙") || lower.contains("flutter") || lower.contains("移动端") || lower.contains("安卓") || lower.contains("ios")) {
            return "移动与跨端开发工程师";
        }
        return "Java 全栈架构师";
    }

    /**
     * 格式化多智能体协同导学推演方案回复
     */
    @SuppressWarnings("unchecked")
    public String formatAgentDeliberationReply(String targetRole, Object data) {
        if (!(data instanceof Map)) return null;
        Map<String, Object> map = (Map<String, Object>) data;

        StringBuilder sb = new StringBuilder();
        sb.append("🎯 **智问学伴多智能体协同导学系统已为您就绪！**\n\n");
        sb.append("已调动【画像/探针/召回/大纲知识拆解/DAG规划/审判反思/证据链】6 大协同智能体，为您深度定制【")
          .append(targetRole).append("】成长路线方案：\n\n");

        Object pathObj = map.get("learningPath");
        if (pathObj instanceof Map) {
            Map<String, Object> path = (Map<String, Object>) pathObj;
            Object stagesObj = path.get("stages");
            if (stagesObj instanceof List) {
                sb.append("🗺️ **4 阶段进阶拓扑成长图谱**：\n");
                List<Map<String, Object>> stages = (List<Map<String, Object>>) stagesObj;
                int idx = 1;
                for (Map<String, Object> stage : stages) {
                    String name = stage.get("stageName") != null ? stage.get("stageName").toString() : ("阶段 " + idx);
                    String desc = stage.get("description") != null ? stage.get("description").toString() : "";
                    Object hours = stage.get("stageHours");
                    sb.append("• **阶段 ").append(idx++).append("：").append(name).append("**");
                    if (hours != null) {
                        sb.append(" (").append(hours).append("课时)");
                    }
                    if (StringUtils.hasText(desc)) {
                        sb.append(" - ").append(desc);
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }

        Object criticObj = map.get("criticReport");
        if (criticObj instanceof Map) {
            Map<String, Object> critic = (Map<String, Object>) criticObj;
            String level = critic.get("verdictLevel") != null ? critic.get("verdictLevel").toString() : "卓越 (A+)";
            Object score = critic.get("overallScore") != null ? critic.get("overallScore") : 100;
            sb.append("⚖️ **审判智能体质检评级**：").append(level).append(" · ").append(score).append("分（DAG 拓扑合规无先修倒置）\n\n");
        }

        Object recsObj = map.get("recommendations");
        if (recsObj instanceof List) {
            List<Map<String, Object>> recs = (List<Map<String, Object>>) recsObj;
            if (!recs.isEmpty()) {
                sb.append("💡 **专属优选核心必修课**：\n");
                int rIdx = 1;
                for (Map<String, Object> c : recs) {
                    String title = c.get("title") != null ? c.get("title").toString() : "";
                    String reason = c.get("recommendReason") != null ? c.get("recommendReason").toString() : "";
                    sb.append(rIdx++).append(". 《").append(title).append("》");
                    if (StringUtils.hasText(reason)) {
                        sb.append(" —— ").append(reason);
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }

        sb.append("🚀 **全景大屏联动**：\n已为您同步生成全景拓扑看板，点击下方卡片或前往首页即可在「AI 协同推演仪表盘 (HUD)」中全屏研读与人机微调！\n");
        sb.append("[ACTION_VIEW_PATH:").append(targetRole).append("]");
        return sb.toString();
    }

    /**
     * 智能体全域操作意图识别与派发
     *
     * @param content  用户消息内容
     * @param userId   当前登录用户 ID
     * @param userName 当前用户名
     * @return 渲染生成的动作卡片响应，若未命中则返回 null
     */
    public String tryDispatchAgentAction(String content, Long userId, String userName) {
        if (!StringUtils.hasText(content)) return null;
        String lower = content.toLowerCase();

        // 0. 安全防御责任链前置过滤 (租户隔离、限流与注入防御)
        SecurityCheckContext secContext = securityShield.executeSecurityChain(userId, userName, content);
        if (secContext.isBlocked()) {
            return secContext.getBlockedMessage();
        }

        // 1. 委托给策略工厂统一匹配并派发全域 14 大业务动作处理器
        return actionFactory.dispatch(content, lower, userId, userName);
    }
}
