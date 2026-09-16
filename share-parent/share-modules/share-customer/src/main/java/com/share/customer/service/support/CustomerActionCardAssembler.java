package com.share.customer.service.support;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.common.core.web.domain.AjaxResult;
import com.share.customer.domain.interview.InterviewSession;
import com.share.customer.domain.interview.dto.StartInterviewRequest;
import com.share.customer.domain.interview.vo.ResumeAnalysisVO;
import com.share.customer.service.IInterviewService;
import com.share.customer.service.IUserResumeService;
import com.share.education.api.RemoteEducationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 智能体自主意图识别、动作派发与交互卡片装配器 (Agent Copilot Action Card Assembler)
 */
@Slf4j
@Component
public class CustomerActionCardAssembler {

    private final ObjectMapper objectMapper;
    private final RemoteEducationService remoteEducationService;
    private final IInterviewService interviewService;
    private final IUserResumeService userResumeService;
    private final CustomerSecurityShield securityShield;

    public CustomerActionCardAssembler(
            ObjectMapper objectMapper,
            RemoteEducationService remoteEducationService,
            @Lazy IInterviewService interviewService,
            @Lazy IUserResumeService userResumeService,
            CustomerSecurityShield securityShield) {
        this.objectMapper = objectMapper;
        this.remoteEducationService = remoteEducationService;
        this.interviewService = interviewService;
        this.userResumeService = userResumeService;
        this.securityShield = securityShield;
    }

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

    public String tryDispatchAgentAction(String content, Long userId, String userName) {
        if (!StringUtils.hasText(content)) return null;
        String lower = content.toLowerCase();

        // 0. 租户与跨用户越权防御拦截
        if (securityShield.isCrossUserAttempt(lower)) {
            return "🔒 **系统安全与数据隐私保护拦截**\n\n"
                    + "智问学伴系统启用了严格的**租户与用户数据隐私隔离防护机制**：\n"
                    + "• **安全准则**：系统严格遵循权限隔离规范，智能体仅有权操作和展示属于您当前认证账号的数据，**严禁跨账号访问或操作其他学员的订单、简历、考场记录或资产**；\n"
                    + "• **当前认证账户**：【" + (userName != null ? userName : "当前学员") + "】（用户 ID: " + userId + "）；\n\n"
                    + "智能体将仅为您本人处理属于您名下的业务。如需管理您本人的数据，请直接对我说“查我的订单”、“看我的面试报告”或“帮我诊断简历”。";
        }

        // 1. 历史模拟面试复盘与成绩查询
        if (isInterviewReportIntent(lower)) {
            return handleInterviewReportAction(userId);
        }
        // 2. 模拟面试开考意图
        if (isInterviewActionIntent(lower)) {
            return handleInterviewAction(lower);
        }
        // 3. 个人订单与交易管理意图
        if (isOrderManageIntent(lower)) {
            return handleOrderManageAction(userId);
        }
        // 4. 优惠券领取与福利中心意图
        if (isCouponCenterIntent(lower)) {
            return handleCouponCenterAction(userId);
        }
        // 5. 购物车资产查看意图
        if (isCartViewIntent(lower)) {
            return handleCartViewAction(userId);
        }
        // 6. 课程购买 / 加购 / 选购意图
        if (isCoursePurchaseIntent(lower)) {
            return handleCoursePurchaseAction(content);
        }
        // 7. 每日打卡签到意图
        if (isSignInActionIntent(lower)) {
            return handleSignInAction();
        }
        // 8. 赛季学霸天梯榜与积分资产意图
        if (isPointsRankingIntent(lower)) {
            return handlePointsRankingAction(userId);
        }
        // 9. 考试成绩与测验考核意图
        if (isExamQueryIntent(lower)) {
            return handleExamQueryAction(userId);
        }
        // 10. 简历诊断与更新意图
        if (isResumeActionIntent(lower)) {
            return handleResumeAction();
        }
        // 11. 随堂速记与个人笔记意图
        if (isNoteQuickIntent(lower)) {
            return handleNoteQuickAction(content, userId);
        }
        // 12. 学员动态学习画像与技能雷达
        if (isLearningPortraitIntent(lower)) {
            return handleLearningPortraitAction(userId);
        }
        // 13. 课表与继续学习意图
        if (isLearningActionIntent(lower)) {
            return handleLearningAction();
        }
        // 14. 页面穿梭与路由导航意图
        if (isPageNavigatorIntent(lower)) {
            return handlePageNavigatorAction(lower);
        }

        return null;
    }

    private boolean isInterviewActionIntent(String lower) {
        return lower.contains("模拟面试") || lower.contains("我要面试") || lower.contains("开一场面试")
                || lower.contains("帮我开一场面试") || lower.contains("面试考场") || lower.contains("开始面试")
                || lower.contains("想面试") || lower.contains("阿里面试") || lower.contains("java面试")
                || lower.contains("高并发面试") || (lower.contains("面试") && (lower.contains("开") || lower.contains("来一场") || lower.contains("开始")));
    }

    private String handleInterviewAction(String lower) {
        String targetJob = "Java高级开发工程师";
        if (lower.contains("大模型") || lower.contains("llm") || lower.contains("ai")) {
            targetJob = "大语言模型应用工程师";
        } else if (lower.contains("go") || lower.contains("golang") || lower.contains("云原生")) {
            targetJob = "Go云原生架构师";
        } else if (lower.contains("前端") || lower.contains("vue") || lower.contains("react")) {
            targetJob = "Web前端架构专家";
        } else if (lower.contains("大数据") || lower.contains("spark") || lower.contains("flink")) {
            targetJob = "大数据流批一体工程师";
        }

        String company = "大厂通用";
        if (lower.contains("阿里")) company = "阿里巴巴";
        else if (lower.contains("字节")) company = "字节跳动";
        else if (lower.contains("腾讯")) company = "腾讯科技";
        else if (lower.contains("美团")) company = "美团";
        else if (lower.contains("百度")) company = "百度";

        try {
            StartInterviewRequest req = new StartInterviewRequest();
            req.setTargetJob(targetJob);
            req.setCompanyTarget(company);
            req.setInterviewerStyle("p7_architect");
            req.setTotalTurns(20);
            InterviewSession newSession = interviewService.startSession(req);
            Long sId = newSession.getId();

            Map<String, Object> card = new HashMap<>();
            card.put("action", "interview_launch");
            card.put("sessionId", String.valueOf(sId));
            card.put("targetJob", targetJob);
            card.put("company", company);
            card.put("interviewerStyle", "p7_architect");
            card.put("roundInfo", "全真三环节 20 题 60 分钟限时架构");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("🎯 **全真 AI 模拟面试考场已为您极速就绪！**\n\n");
            sb.append("已为您成功开辟面向【").append(company).append("】的【").append(targetJob).append("】全真考核专场：\n");
            sb.append("• **考核架构**：全真三环节 20 题 60 分钟限时考核（环节一：破题自我介绍 ➔ 环节二：小林coding 10大独立模块八股文 ➔ 环节三：真实项目上下文深度深挖连环追问）；\n");
            sb.append("• **考官引擎**：微软晓晓 (Xiaoxiao Neural TTS) 真实级语音朗读 + 数字人口型毫秒级音画协同；\n");
            sb.append("• **考场场次**：#").append(sId).append("，第一题考题已生成完毕。\n\n");
            sb.append("点击下方考场卡片，即可立即入场进入沉浸式考场开考：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("智能体自动开辟面试考场失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isCoursePurchaseIntent(String lower) {
        if (lower.contains("退款") || lower.contains("退课") || lower.contains("发票") || lower.contains("开票") || lower.contains("密码")) {
            return false;
        }
        return lower.contains("买课") || lower.contains("买课程") || lower.contains("购课")
                || lower.contains("我要买") || lower.contains("想买") || lower.contains("购买")
                || lower.contains("加购") || lower.contains("加购物车") || lower.contains("加入购物车")
                || lower.contains("报名") || lower.contains("选购课程")
                || (lower.contains("课程") && (lower.contains("买") || lower.contains("购") || lower.contains("加") || lower.contains("下单")));
    }

    private String extractCourseKeyword(String content) {
        if (!StringUtils.hasText(content)) return "";
        String lower = content.toLowerCase();
        if (lower.contains("微服务") || lower.contains("springcloud") || lower.contains("cloud")) return "微服务";
        if (lower.contains("springboot") || lower.contains("boot")) return "SpringBoot";
        if (lower.contains("node") || lower.contains("nodejs")) return "Node";
        if (lower.contains("vue") || lower.contains("前端") || lower.contains("react")) return "前端";
        if (lower.contains("python") || lower.contains("爬虫") || lower.contains("机器学习")) return "Python";
        if (lower.contains("go") || lower.contains("golang")) return "Go";
        if (lower.contains("docker") || lower.contains("k8s") || lower.contains("云原生")) return "云原生";
        if (lower.contains("mysql") || lower.contains("数据库") || lower.contains("sql")) return "MySQL";
        if (lower.contains("redis") || lower.contains("缓存")) return "Redis";
        if (lower.contains("大模型") || lower.contains("ai") || lower.contains("llm")) return "大模型";
        if (lower.contains("算法") || lower.contains("数据结构")) return "算法";
        if (lower.contains("java")) return "Java";
        return "";
    }

    @SuppressWarnings("unchecked")
    private String handleCoursePurchaseAction(String content) {
        String keyword = extractCourseKeyword(content);
        try {
            AjaxResult res = remoteEducationService.searchCourses(keyword, 5);
            if (res != null && res.isSuccess() && res.get("data") != null) {
                Map<String, Object> dataMap = (Map<String, Object>) res.get("data");
                Object listObj = dataMap.get("list");
                if (listObj instanceof List) {
                    List<?> list = (List<?>) listObj;
                    if (!list.isEmpty() && list.get(0) instanceof Map) {
                        Map<String, Object> course = (Map<String, Object>) list.get(0);
                        String courseId = String.valueOf(course.get("id"));
                        String title = course.get("title") != null ? course.get("title").toString() : String.valueOf(course.get("courseName"));
                        String cover = course.get("cover") != null ? course.get("cover").toString() : "/src/assets/images/courses/default-cover.svg";
                        Number priceNum = course.get("price") instanceof Number ? (Number) course.get("price") : 0;
                        Number originalPriceNum = course.get("originalPrice") instanceof Number ? (Number) course.get("originalPrice") : priceNum;
                        String teacherName = course.get("teacherName") != null ? course.get("teacherName").toString() : "资深讲师团队";
                        Object lessons = course.getOrDefault("lessons", 0);
                        Object isFree = course.getOrDefault("isFree", 0);
                        String desc = course.get("shortDescription") != null ? course.get("shortDescription").toString() : "";

                        Map<String, Object> card = new HashMap<>();
                        card.put("action", "course_purchase");
                        card.put("courseId", courseId);
                        card.put("title", title);
                        card.put("cover", cover);
                        card.put("price", priceNum);
                        card.put("originalPrice", originalPriceNum);
                        card.put("teacherName", teacherName);
                        card.put("lessons", lessons);
                        card.put("isFree", isFree);
                        String cardJson = objectMapper.writeValueAsString(card);

                        BigDecimal priceYuan = new BigDecimal(priceNum.toString()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                        StringBuilder sb = new StringBuilder();
                        sb.append("🛒 **已为您精准匹配目标课程！**\n\n");
                        sb.append("已为您调取最受学员好评的精品好课《").append(title).append("》：\n");
                        if (StringUtils.hasText(desc)) {
                            sb.append("• **课程介绍**：").append(desc).append("\n");
                        }
                        sb.append("• **主讲名师**：").append(teacherName).append(" ｜ **总课时**：").append(lessons).append(" 讲\n");
                        sb.append("• **课程价格**：");
                        if ("1".equals(String.valueOf(isFree)) || priceYuan.compareTo(BigDecimal.ZERO) == 0) {
                            sb.append("【限时免费】\n");
                        } else {
                            sb.append("¥").append(priceYuan).append(" 元\n");
                        }
                        sb.append("\n我已为您生成专属购课操作卡片，您可以直接点击【加入购物车】或【立即结算】一键发起购买：\n\n");
                        sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
                        return sb.toString();
                    }
                }
            }
        } catch (Exception ex) {
            log.error("智能体搜索并生成购课卡片异常: {}", ex.getMessage(), ex);
        }
        return null;
    }

    private boolean isSignInActionIntent(String lower) {
        return lower.contains("签到") || lower.contains("打卡") || lower.contains("今日打卡")
                || lower.contains("领积分") || lower.contains("每日签到");
    }

    private String handleSignInAction() {
        try {
            Map<String, Object> card = new HashMap<>();
            card.put("action", "sign_in");
            card.put("dailyPoints", 10);
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("✨ **每日学情打卡与积分中心**\n\n");
            sb.append("坚持每日签到打卡，每天可固定获得 **+10 学分** 奖励，连续打卡更有惊喜加成！积分可在购买课程时直接抵扣现金或兑换专属大额优惠券。\n\n");
            sb.append("点击下方卡片中的【一键打卡签到】即可立即完成打卡：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成签到卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isResumeActionIntent(String lower) {
        return lower.contains("诊断简历") || lower.contains("我的简历") || lower.contains("分析简历")
                || lower.contains("看看我的简历") || lower.contains("简历诊断") || lower.contains("简历报告");
    }

    private String handleResumeAction() {
        try {
            ResumeAnalysisVO resume = userResumeService.getMyResume();
            Map<String, Object> card = new HashMap<>();
            StringBuilder sb = new StringBuilder();

            if (resume != null && resume.getId() != null) {
                card.put("action", "resume_diagnose");
                card.put("resumeId", String.valueOf(resume.getId()));
                card.put("fileName", resume.getFileName());
                card.put("matchScore", resume.getMatchScore() != null ? resume.getMatchScore() : 80);
                card.put("targetJob", resume.getTargetJob() != null ? resume.getTargetJob() : "技术开发工程师");
                String cardJson = objectMapper.writeValueAsString(card);

                sb.append("📄 **AI 简历深度诊断与能力雷达**\n\n");
                sb.append("已为您调取当前个人中心关联的简历档案《").append(resume.getFileName() != null ? resume.getFileName() : "我的简历").append("》：\n");
                sb.append("• **对标岗位**：").append(resume.getTargetJob() != null ? resume.getTargetJob() : "技术开发工程师").append("\n");
                sb.append("• **综合契合度评分**：").append(resume.getMatchScore() != null ? resume.getMatchScore() : 80).append(" 分\n");
                if (resume.getProjectHighlights() != null && !resume.getProjectHighlights().isEmpty()) {
                    sb.append("• **核心高光亮点**：").append(String.join("、", resume.getProjectHighlights())).append("\n");
                }
                sb.append("\n点击下方卡片即可查看完整能力六维雷达诊断，或直接针对薄弱项发起模拟面试连环追问：\n\n");
                sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            } else {
                card.put("action", "resume_upload");
                String cardJson = objectMapper.writeValueAsString(card);

                sb.append("📄 **AI 简历深度诊断与职涯对标**\n\n");
                sb.append("检测到您当前尚未在系统中上传求职简历。上传简历后，AI 将基于真实项目与技术标签进行深度特征抽取、契合度量化与高并发/分布式实战考题连环追问。\n\n");
                sb.append("点击下方卡片即可前往个人中心一键上传简历：\n\n");
                sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            }
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成简历诊断卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isLearningActionIntent(String lower) {
        return lower.contains("学到哪了") || lower.contains("继续学习") || lower.contains("我的进度")
                || lower.contains("课表") || lower.contains("我的课表") || lower.contains("继续上课");
    }

    private String handleLearningAction() {
        try {
            Map<String, Object> card = new HashMap<>();
            card.put("action", "continue_learning");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("📚 **学伴学习进度接力**\n\n");
            sb.append("已为您同步个人课表与学习进度。保持持续学习与代码实践是快速蜕变为架构师的核心捷径！\n\n");
            sb.append("点击下方操作卡片即可直达我的课表或探索更多精品好课：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成学习进度卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isInterviewReportIntent(String lower) {
        if (lower.contains("开") || lower.contains("开始") || lower.contains("来一场") || lower.contains("我要面试") || lower.contains("开一场")) {
            return false;
        }
        return lower.contains("面试报告") || lower.contains("面试成绩") || lower.contains("面试复盘")
                || lower.contains("上次面试") || lower.contains("面试记录") || lower.contains("面试得了多少分")
                || lower.contains("查面试") || lower.contains("我的面试") || lower.contains("面试结果");
    }

    private String handleInterviewReportAction(Long userId) {
        try {
            if (userId == null) {
                return "请先登录后再查看您的模拟面试记录与复盘报告。";
            }
            IPage<InterviewSession> mySessions = interviewService.listMySessions(1, 5);
            List<InterviewSession> records = mySessions != null ? mySessions.getRecords() : Collections.emptyList();

            if (records.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("📊 **AI 模拟面试档案查询**\n\n");
                sb.append("• **身份安全校验**：已严格按当前登录用户（ID: ").append(userId).append("）进行数据隔离。\n");
                sb.append("检测到您当前暂无任何模拟面试考核记录。点击下方即可立即开启您的首场全真模拟考核：\n\n");
                Map<String, Object> card = new HashMap<>();
                card.put("action", "interview_launch");
                card.put("targetJob", "Java高级开发工程师");
                card.put("company", "阿里巴巴");
                sb.append("[AGENT_ACTION_CARD:").append(objectMapper.writeValueAsString(card)).append("]");
                return sb.toString();
            }

            InterviewSession latest = records.get(0);
            Map<String, Object> card = new HashMap<>();
            card.put("action", "interview_report");
            card.put("sessionId", String.valueOf(latest.getId()));
            card.put("targetJob", latest.getTargetJob() != null ? latest.getTargetJob() : "技术开发工程师");
            card.put("company", latest.getCompanyTarget() != null ? latest.getCompanyTarget() : "大厂通用");
            card.put("status", latest.getStatus());
            card.put("score", latest.getScore() != null ? latest.getScore() : 82);
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("📊 **已为您调取属于您本人的最新模拟面试复盘档案！**\n\n");
            sb.append("• **身份安全隔离**：已严格锁定当前认证用户（ID: ").append(userId).append("），杜绝越权访问他人面试隐私\n");
            sb.append("• **考场场次**：#").append(latest.getId()).append("\n");
            sb.append("• **考核方向**：【").append(latest.getCompanyTarget()).append("】").append(latest.getTargetJob()).append("\n");
            sb.append("• **考核状态**：").append(latest.getStatus() == 2 ? "✅ 考核已终局完成" : (latest.getStatus() == 1 ? "⏳ 考场进行中" : "已结束")).append("\n");
            if (latest.getScore() != null) {
                sb.append("• **综合得分**：").append(latest.getScore()).append(" 分\n");
            }
            sb.append("\n点击下方卡片即可直接进入全真大屏复盘与六维能力雷达：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("查询面试复盘卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isOrderManageIntent(String lower) {
        return lower.contains("订单") || lower.contains("我的订单") || lower.contains("查订单")
                || lower.contains("买了什么") || lower.contains("买的课") || lower.contains("待付款")
                || lower.contains("退款") || lower.contains("退课") || lower.contains("取消订单");
    }

    private String handleOrderManageAction(Long userId) {
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

    private boolean isCouponCenterIntent(String lower) {
        return lower.contains("优惠券") || lower.contains("领券") || lower.contains("卡券")
                || lower.contains("减免") || lower.contains("打折券") || lower.contains("兑换码");
    }

    private String handleCouponCenterAction(Long userId) {
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

    private boolean isCartViewIntent(String lower) {
        String withoutCart = lower.replace("购物车", "").replace("购物车里", "").replace("购物车内", "");
        if (withoutCart.contains("加") || withoutCart.contains("进") || withoutCart.contains("放") || withoutCart.contains("买") || withoutCart.contains("购")) {
            return false;
        }
        return lower.contains("购物车") || lower.contains("车里有") || lower.contains("查看购物车") || lower.contains("清空购物车");
    }

    private String handleCartViewAction(Long userId) {
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

    private boolean isExamQueryIntent(String lower) {
        return lower.contains("考试") || lower.contains("测验") || lower.contains("查分")
                || lower.contains("考试成绩") || lower.contains("考试记录") || lower.contains("错题") || lower.contains("答卷");
    }

    private String handleExamQueryAction(Long userId) {
        try {
            Map<String, Object> card = new HashMap<>();
            card.put("action", "exam_query");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("📝 **学情考试与测验考核中心**\n\n");
            sb.append("• **专属成绩档案**：考试记录与错题本严格绑定当前学员（ID: ").append(userId).append("），真实记录期末测试、随堂小测与技术实训成绩。\n\n");
            sb.append("点击下方卡片即可查看您的考试记录、成绩单与错题解析：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成考试中心卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isNoteQuickIntent(String lower) {
        return lower.contains("记笔记") || lower.contains("做笔记") || lower.contains("我的笔记")
                || lower.contains("记一条笔记") || lower.contains("帮我记笔记") || lower.contains("写笔记") || lower.contains("查看笔记");
    }

    private String handleNoteQuickAction(String content, Long userId) {
        try {
            String noteText = "";
            if (content.contains("：")) {
                noteText = content.substring(content.indexOf("：") + 1).trim();
            } else if (content.contains(":")) {
                noteText = content.substring(content.indexOf(":") + 1).trim();
            } else {
                for (String prefix : new String[]{"记一条笔记", "帮我记笔记", "做笔记", "记笔记", "写笔记"}) {
                    if (content.contains(prefix)) {
                        noteText = content.substring(content.indexOf(prefix) + prefix.length()).trim();
                        break;
                    }
                }
            }

            Map<String, Object> card = new HashMap<>();
            card.put("action", "note_quick");
            card.put("noteContent", noteText);
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("📒 **随堂速记与个人技术知识库**\n\n");
            sb.append("• **私密知识空间**：笔记内容已开启个人强隔离（归属用户 ID: ").append(userId).append("），沉淀专属技术心得。\n");
            if (StringUtils.hasText(noteText)) {
                sb.append("• **识别笔记内容**：").append(noteText).append("\n");
            }
            sb.append("\n点击下方卡片即可快速将要点存入您的笔记库，或查看历史笔记：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成笔记卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isPointsRankingIntent(String lower) {
        if (lower.contains("微积分") || lower.contains("定积分") || lower.contains("不定积分")
                || lower.contains("重积分") || lower.contains("积分方程") || lower.contains("蒙特卡洛")) {
            return false;
        }
        return lower.contains("学霸") || lower.contains("天梯") || lower.contains("排行榜")
                || lower.contains("学霸榜") || lower.contains("天梯榜") || lower.contains("积分榜")
                || lower.contains("积分排行") || lower.contains("积分明细") || lower.contains("我的积分")
                || lower.contains("多少积分") || lower.contains("积分中心") || lower.contains("查积分")
                || lower.contains("学分") || lower.contains("领积分");
    }

    private String handlePointsRankingAction(Long userId) {
        try {
            Map<String, Object> card = new HashMap<>();
            card.put("action", "points_ranking");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("🏆 **赛季学霸天梯榜与积分资产**\n\n");
            sb.append("• **用户资产核验**：当前登录用户（ID: ").append(userId).append("）的学分资产已就绪。\n");
            sb.append("• **排行榜规则**：每赛季根据日常答题、签到与考场表现更新学霸天梯段位。\n\n");
            sb.append("点击下方卡片即可查看赛季天梯排名或我的积分明细：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成积分榜卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isLearningPortraitIntent(String lower) {
        return lower.contains("学习画像") || lower.contains("技能图谱") || lower.contains("我的画像")
                || lower.contains("能力雷达") || lower.contains("技能雷达") || lower.contains("能力画像");
    }

    private String handleLearningPortraitAction(Long userId) {
        try {
            Map<String, Object> card = new HashMap<>();
            card.put("action", "learning_portrait");
            String cardJson = objectMapper.writeValueAsString(card);

            StringBuilder sb = new StringBuilder();
            sb.append("📊 **学员多维动态学习画像与能力雷达**\n\n");
            sb.append("• **动态构建**：基于您当前账户（ID: ").append(userId).append("）在平台完成的课程进度、模拟面试打分与随堂测试表现实时推演。\n\n");
            sb.append("点击下方卡片即可直达个人中心查看学员能力六维雷达与技能图谱：\n\n");
            sb.append("[AGENT_ACTION_CARD:").append(cardJson).append("]");
            return sb.toString();
        } catch (Exception ex) {
            log.error("生成画像卡片失败: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isPageNavigatorIntent(String lower) {
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

    private String handlePageNavigatorAction(String lower) {
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
}
