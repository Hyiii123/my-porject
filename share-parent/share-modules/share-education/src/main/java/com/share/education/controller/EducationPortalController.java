package com.share.education.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.security.annotation.InnerAuth;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.education.domain.EduExamRecord;
import com.share.education.domain.EduInterest;
import com.share.education.domain.EduLearningPlan;
import com.share.education.domain.EduLearningRecord;
import com.share.education.domain.EduNote;
import com.share.education.domain.EduQuestion;
import com.share.education.domain.EduReply;
import com.share.education.service.IEducationService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面向用户端的教育领域兼容接口。
 *
 * <p>Gateway 将原有 /cs、/ss、/ls、/es、/rs 前缀去掉后转发到这里，
 * 所以接口路径仍与原 Vue 项目的调用约定一致。</p>
 */
@RestController
public class EducationPortalController extends BaseController {
    private final IEducationService educationService;

    public EducationPortalController(IEducationService educationService) {
        this.educationService = educationService;
    }

    @GetMapping({"/categorys/all", "/categorys/list"})
    public AjaxResult categories(@RequestParam(required = false, defaultValue = "false") boolean includeDisabled) {
        return success(educationService.listCategories(includeDisabled));
    }

    @GetMapping("/categorys/{id}")
    public AjaxResult category(@PathVariable Long id) {
        return success(educationService.category(id));
    }

    @PostMapping("/categorys/add")
    @RequiresPermissions("education:category:add")
    public AjaxResult addLegacyCategory(@RequestBody Map<String, Object> body) {
        return success(educationService.saveLegacyCategory(body));
    }

    @PutMapping("/categorys/update")
    @RequiresPermissions("education:category:edit")
    public AjaxResult updateLegacyCategory(@RequestBody Map<String, Object> body) {
        return success(educationService.saveLegacyCategory(body));
    }

    @PutMapping("/categorys/disableOrEnable")
    @RequiresPermissions("education:category:edit")
    public AjaxResult updateLegacyCategoryStatus(@RequestBody(required = false) Map<String, Object> body) {
        educationService.updateLegacyCategoryStatus(body == null ? Map.of() : body);
        return success();
    }

    @DeleteMapping("/categorys/{id}")
    @RequiresPermissions("education:category:remove")
    public AjaxResult deleteLegacyCategory(@PathVariable Long id) {
        educationService.removeLegacyCategory(id);
        return success();
    }

    @GetMapping("/courses/page")
    public AjaxResult coursePage(@RequestParam Map<String, Object> params) {
        return success(educationService.portalCourses(params));
    }

    @GetMapping({"/courses/statistics", "/courses/stats"})
    public AjaxResult courseStatistics() {
        return success(educationService.courseStatistics());
    }

    @GetMapping("/courses/simpleInfo/list")
    public AjaxResult simpleCourses() {
        return success(educationService.simpleCourses());
    }

    @GetMapping("/courses/portal")
    public AjaxResult coursePortal(@RequestParam Map<String, Object> params) {
        return success(educationService.portalCourses(params));
    }

    @GetMapping("/courses/ranking/likes")
    public AjaxResult courseLikeRanking(@RequestParam(required = false, defaultValue = "10") int limit) {
        return success(educationService.courseLikeRanking(limit));
    }

    @GetMapping({"/courses/recommendations", "/courses/recommend", "/courses/recommendations/personalized", "/courses/recommend/personalized"})
    public AjaxResult personalizedRecommendations(@RequestParam(required = false, defaultValue = "6") int limit) {
        return success(educationService.personalizedRecommendations(limit));
    }

    @GetMapping({"/courses/recommendations/orchestrate", "/courses/recommend/orchestrate"})
    public AjaxResult orchestrateAgentRecommend(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String targetRole,
            @RequestParam(required = false, defaultValue = "4") Integer limit,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String query) {
        return success(educationService.orchestrateAgentRecommend(userId, targetRole, limit, difficulty, query));
    }

    /**
     * 获取基于多智能体系统与 RAG 知识图谱生成的学员个性化进阶路线规划图。
     */
    @GetMapping({"/courses/recommendations/learning-path", "/courses/recommend/learning-path"})
    public AjaxResult personalizedLearningPath() {
        return success(educationService.getPersonalizedLearningPath());
    }

    /**
     * 人机协同微调学员个性化成长进阶路线 (Human-in-the-Loop Refinement)。
     */
    @PostMapping({"/courses/recommendations/refine", "/courses/recommend/refine"})
    public AjaxResult refinePersonalizedLearningPath(@RequestBody(required = false) Map<String, Object> overrides) {
        return success(educationService.refinePersonalizedLearningPath(overrides != null ? overrides : Collections.emptyMap()));
    }

    /**
     * 获取冷启动主动探针诊断问卷 (Active Probing Questions)。
     */
    @GetMapping({"/courses/recommendations/probe", "/courses/recommend/probe"})
    public AjaxResult activeProbingQuestions() {
        return success(educationService.getActiveProbingQuestions());
    }

    /**
     * 提交主动探针反馈并即刻自适应生成校准推荐。
     */
    @PostMapping({"/courses/recommendations/probe/submit", "/courses/recommend/probe/submit"})
    public AjaxResult submitActiveProbingAnswers(@RequestBody(required = false) Map<String, String> answers) {
        return success(educationService.submitActiveProbingAnswers(answers != null ? answers : Collections.emptyMap()));
    }

    /**
     * 获取多智能体系统自动化质量评测度量大屏数据 (Agent Evals & Observability Metrics)。
     */
        @Autowired(required = false)
    private com.share.education.ai.evals.EduLearningOutcomeTracker outcomeTracker;

    @Autowired(required = false)
    private com.share.education.ai.tools.sandbox.CodeSandboxTool codeSandboxTool;

    @Autowired(required = false)
    private com.share.education.ai.tools.market.JobMarketRadarTool jobMarketRadarTool;

    @Autowired(required = false)
    private com.share.education.ai.tools.remediation.AdaptiveRemediationTool adaptiveRemediationTool;

    @Autowired(required = false)
    private com.share.education.ai.tools.registry.AgentToolRegistry toolRegistry;

    @Autowired(required = false)
    private com.share.education.ai.rag.HybridGraphRagEngine hybridGraphRagEngine;

    @Autowired(required = false)
    private com.share.education.ai.algorithm.bkt.KnowledgeTracingService knowledgeTracingService;

    @Autowired(required = false)
    private com.share.education.ai.memory.AgentMemoryService agentMemoryService;

    @Autowired(required = false)
    private com.share.education.ai.evals.ragas.RagasEvaluationEngine ragasEvaluationEngine;

    /**
     * 混合检索 (Dense + BM25 RRF 融合) 与 Graph RAG 图拓扑增强端点
     */
    @GetMapping({"/courses/ai/rag/hybrid-search", "/courses/rag/search"})
    public AjaxResult hybridRagSearch(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        if (hybridGraphRagEngine != null) {
            Long targetUid = userId != null && userId > 0 ? userId : com.share.education.service.support.EduUtils.currentUserId();
            return success(hybridGraphRagEngine.retrieve(query, targetUid, limit));
        }
        return error("混合图检索引擎未就绪");
    }

    /**
     * Graph RAG 多跳先修依赖图拓扑路径分析
     */
    @GetMapping({"/courses/ai/rag/graph-path", "/courses/rag/path"})
    public AjaxResult graphPrerequisitePath(@RequestParam(required = false) String concept) {
        if (hybridGraphRagEngine != null) {
            return success(hybridGraphRagEngine.retrieve(concept, null, 5));
        }
        return error("知识图谱拓扑服务未就绪");
    }

    /**
     * 智能体安全代码沙箱执行端点 (Dynamic Code Sandbox Runner)
     */
    @PostMapping({"/courses/sandbox/run", "/interview/sandbox/run"})
    public AjaxResult runCodeSandbox(@RequestBody com.share.education.ai.tools.sandbox.CodeExecutionRequest request) {
        if (codeSandboxTool != null) {
            long t0 = System.currentTimeMillis();
            var result = codeSandboxTool.apply(request);
            if (toolRegistry != null) {
                toolRegistry.recordInvocation("codeSandboxTool", !"SECURITY_VIOLATION".equals(result.getStatus()) && !"COMPILE_ERROR".equals(result.getStatus()), System.currentTimeMillis() - t0);
            }
            return success(result);
        }
        return error("沙箱执行引擎未就绪");
    }

    /**
     * 产业前沿招聘行情与技能需求雷达 (Job Market Radar)
     */
    @GetMapping({"/courses/ai/market/trends", "/courses/market/trends"})
    public AjaxResult getJobMarketTrends(@RequestParam(required = false) String role) {
        if (jobMarketRadarTool != null) {
            if (StringUtils.hasText(role)) {
                return success(jobMarketRadarTool.apply(new com.share.education.ai.tools.market.JobMarketQueryRequest(role)));
            }
            return success(jobMarketRadarTool.getAllTracks());
        }
        return success(Collections.emptyList());
    }

    /**
     * 知识图谱最小前置闭包自适应诊断 (Adaptive Remediation Diagnostic)
     */
    @PostMapping({"/courses/ai/remediation/diagnose", "/courses/remediation/diagnose"})
    public AjaxResult diagnoseRemediation(@RequestBody com.share.education.ai.tools.remediation.AdaptiveRemediationRequest request) {
        if (adaptiveRemediationTool != null) {
            long t0 = System.currentTimeMillis();
            var plan = adaptiveRemediationTool.apply(request);
            if (toolRegistry != null) {
                toolRegistry.recordInvocation("adaptiveRemediationTool", true, System.currentTimeMillis() - t0);
            }
            return success(plan);
        }
        return error("自适应诊断引擎未就绪");
    }

    /**
     * 智能体标准工具目录与 JSON Schema (Agent Tool Catalog)
     */
    @GetMapping({"/courses/ai/tools/catalog", "/courses/tools/catalog"})
    public AjaxResult getToolCatalog() {
        if (toolRegistry != null) {
            return success(toolRegistry.listTools());
        }
        return success(Collections.emptyList());
    }

    /**
     * 智能体工具在线调用度量大屏 (Agent Tool Telemetry)
     */
    @GetMapping({"/courses/ai/tools/telemetry", "/courses/tools/telemetry"})
    public AjaxResult getToolTelemetry() {
        if (toolRegistry != null) {
            return success(toolRegistry.getTelemetry());
        }
        return success(Collections.emptyMap());
    }

    /**
     * BKT 贝叶斯微知识点掌握度画像 (Bayesian Knowledge Tracing Mastery Profile)
     */
    @GetMapping({"/courses/ai/bkt/mastery", "/courses/bkt/mastery"})
    public AjaxResult getBktMasteryProfile(@RequestParam(required = false) Long userId) {
        if (knowledgeTracingService != null) {
            Long uid = userId != null ? userId : 1L;
            return success(knowledgeTracingService.getLearnerMasteryProfile(uid));
        }
        return error("知识追踪服务未就绪");
    }

    /**
     * BKT 知识点答题反馈与后验概率迭代更新
     */
    @PostMapping({"/courses/ai/bkt/update", "/courses/bkt/update"})
    public AjaxResult updateBktSkill(
            @RequestParam(required = false) Long userId,
            @RequestParam String skillId,
            @RequestParam(defaultValue = "true") boolean correct) {
        if (knowledgeTracingService != null) {
            Long uid = userId != null ? userId : 1L;
            return success(knowledgeTracingService.updateSkillObservation(uid, skillId, correct));
        }
        return error("知识追踪服务未就绪");
    }

    /**
     * 记录/模拟一条学员艾宾浩斯记忆事件
     */
    @PostMapping({"/courses/ai/memory/record", "/courses/memory/record"})
    public AjaxResult recordMemoryEpisode(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "Java高并发架构师") String role,
            @RequestParam(defaultValue = "Seata 2.0 分布式事务与二阶段提交") String hurdle,
            @RequestParam(defaultValue = "加强分布式事务底层原理学习") String directive,
            @RequestParam(defaultValue = "72") int score) {
        if (agentMemoryService != null) {
            Long uid = userId != null ? userId : 1L;
            agentMemoryService.recordEpisode(uid, role, hurdle, directive, score);
            return success(agentMemoryService.getMemoryProfile(uid));
        }
        return error("智能体记忆中枢未就绪");
    }

    /**
     * 艾宾浩斯遗忘衰减记忆中枢与复习临界告警画像
     */
    @GetMapping({"/courses/ai/memory/profile", "/courses/memory/profile"})
    public AjaxResult getEbbinghausMemoryProfile(@RequestParam(required = false) Long userId) {
        if (agentMemoryService != null) {
            Long uid = userId != null ? userId : 1L;
            return success(agentMemoryService.getMemoryProfile(uid));
        }
        return error("智能体记忆中枢未就绪");
    }

    /**
     * RAGAS 自动化质量评估大盘跑批
     */
    @GetMapping({"/courses/ai/ragas/benchmark", "/courses/ragas/benchmark"})
    public AjaxResult runRagasBenchmark() {
        if (ragasEvaluationEngine != null) {
            return success(ragasEvaluationEngine.evaluateBenchmark());
        }
        return error("RAGAS 评测引擎未就绪");
    }

    /**
     * 获取多智能体路线真实业务履约率与下游学习成效 (Downstream Outcome Telemetry)
     */
    @GetMapping({"/courses/recommendations/evals/outcome", "/courses/recommend/evals/outcome"})
    public AjaxResult agentOutcomeFulfillment(@RequestParam(required = false) Long userId,
                                             @RequestParam(required = false) List<Long> courseIds) {
        Long targetUid = userId != null && userId > 0 ? userId : com.share.education.service.support.EduUtils.currentUserId();
        if (outcomeTracker != null && targetUid != null) {
            return success(outcomeTracker.calculateFulfillment(targetUid, courseIds != null ? courseIds : Collections.emptyList()));
        }
        return success(Map.of("pathFulfillmentRate", 0.0, "outcomeGrade", "待学习观察"));
    }

    @GetMapping({"/courses/recommendations/evals/metrics", "/courses/recommend/evals/metrics"})
    public AjaxResult agentEvaluationMetrics() {
        return success(educationService.getAgentEvaluationMetrics());
    }

    /**
     * 下一代 L5 智能体流式思考与异步并发推演端点 (Server-Sent Events)
     */
    @GetMapping(value = {"/courses/recommendations/stream/reasoning", "/courses/recommend/stream/reasoning"},
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPersonalizedReasoning(@RequestParam(required = false) String targetRole,
                                                 @RequestParam(required = false) String query) {
        return educationService.streamPersonalizedReasoning(targetRole, query);
    }

    @GetMapping("/user/portrait")
    public AjaxResult userPortrait() {
        return success(educationService.getUserPortrait(null));
    }

    @PutMapping("/user/portrait/preferences")
    public AjaxResult updatePortraitPreferences(@RequestBody Map<String, Object> body) {
        return success(educationService.updateUserPortraitPreferences(null, body));
    }

    @GetMapping("/courses/baseInfo/{id}")
    public AjaxResult course(@PathVariable Long id) {
        return success(educationService.legacyCourse(id));
    }

    @PostMapping("/courses/baseInfo/save")
    @RequiresPermissions("education:course:edit")
    public AjaxResult saveCourse(@RequestBody Map<String, Object> body) {
        return success(educationService.saveLegacyCourse(body));
    }

    /** 旧用户端课程学习页使用的目录入口。 */
    @GetMapping("/courses/{id}/catalogs")
    public AjaxResult courseCatalogs(@PathVariable Long id) {
        return success(educationService.legacyCatalogs(id));
    }

    /** 旧管理端课程删除入口，保留单个课程编号的调用契约。 */
    @DeleteMapping("/courses/delete/{id}")
    @RequiresPermissions("education:course:remove")
    public AjaxResult deleteCourse(@PathVariable Long id) {
        educationService.removeCourses(List.of(id));
        return success();
    }

    /** 旧管理端添加阶段测试前先申请一个目录编号。 */
    @GetMapping("/courses/generator")
    @RequiresPermissions("education:catalog:add")
    public AjaxResult generateStageExam() {
        return success(Map.of("id", educationService.generateStageExamId()));
    }

    @PostMapping("/courses/catas/save/{id}/{step}")
    @RequiresPermissions("education:catalog:edit")
    public AjaxResult saveCatalog(@PathVariable Long id, @PathVariable int step,
            @RequestBody(required = false) Object body) {
        return success(educationService.saveLegacyCatalog(id, step, body));
    }

    @PostMapping("/courses/media/save/{id}")
    @RequiresPermissions("education:catalog:edit")
    public AjaxResult saveMedia(@PathVariable Long id, @RequestBody(required = false) Object body) {
        return success(educationService.saveLegacyMedia(id, body));
    }

    @PostMapping("/courses/media/bind")
    @RequiresPermissions("education:catalog:edit")
    public AjaxResult bindMedia(@RequestBody Map<String, Object> body) {
        Long courseId = educationServiceLong(body, "courseId", "id");
        Long sectionId = educationServiceLong(body, "sectionId", "cataId", "catalogId");
        Long mediaId = educationServiceLong(body, "mediaId");
        String mediaName = educationServiceText(body, "mediaName", "videoName");
        Integer duration = educationServiceInt(body, "durationSeconds", "duration", "mediaDuration");
        return success(educationService.bindCatalogMedia(courseId, sectionId, mediaId, mediaName, duration));
    }

    @PostMapping("/courses/media/unbind")
    @RequiresPermissions("education:catalog:edit")
    public AjaxResult unbindMedia(@RequestBody Map<String, Object> body) {
        Long courseId = educationServiceLong(body, "courseId", "id");
        Long sectionId = educationServiceLong(body, "sectionId", "cataId", "catalogId");
        Long mediaId = educationServiceLong(body, "mediaId");
        educationService.unbindCatalogMedia(courseId, sectionId, mediaId);
        return success();
    }

    @GetMapping("/courses/subjects/get/{id}")
    public AjaxResult subjects(@PathVariable Long id) {
        return success(educationService.legacySubjectGroups(id));
    }

    @PostMapping("/courses/subjects/save/{id}")
    @RequiresPermissions("education:catalog:edit")
    public AjaxResult saveSubjects(@PathVariable Long id, @RequestBody(required = false) Object body) {
        // 旧管理端实际发送的是题目关系数组；同时兼容 {datas: [...]} 这类包装请求。
        return success(educationService.saveLegacySubjects(id, body));
    }

    @PostMapping("/courses/teachers/save")
    @RequiresPermissions("education:teacher:edit")
    public AjaxResult saveTeachers(@RequestBody Map<String, Object> body) {
        Long courseId = educationServiceLong(body, "id");
        return success(educationService.saveLegacyTeachers(courseId, body));
    }

    @PostMapping("/courses/upShelf")
    @RequiresPermissions("education:course:edit")
    public AjaxResult upShelf(@RequestBody Map<String, Object> body) {
        Long id = educationServiceLong(body, "id");
        educationService.updateCourseStatus(id, 1);
        return success();
    }

    @PostMapping("/courses/downShelf")
    @RequiresPermissions("education:course:edit")
    public AjaxResult downShelf(@RequestBody Map<String, Object> body) {
        Long id = educationServiceLong(body, "id");
        educationService.updateCourseStatus(id, 2);
        return success();
    }

    @GetMapping("/courses/checkBeforeUpShelf/{id}")
    @RequiresPermissions("education:course:edit")
    public AjaxResult checkBeforeUpShelf(@PathVariable Long id) {
        return success(educationService.checkBeforeUpShelf(id));
    }

    @GetMapping("/courses/checkName")
    @RequiresPermissions("education:course:add")
    public AjaxResult checkCourseName(@RequestParam Map<String, Object> params) {
        return success(educationService.checkCourseName(params));
    }

    @GetMapping("/courses/teachers")
    public AjaxResult allTeachers() {
        return success(educationService.teachers(null));
    }

    @GetMapping("/courses/teachers/{id}")
    public AjaxResult courseTeachers(@PathVariable Long id) {
        return success(educationService.teachers(id));
    }

    @GetMapping({"/courses/catas/{id}", "/courses/catalogs/{id}"})
    public AjaxResult catalogs(@PathVariable Long id) {
        return success(educationService.legacyCatalogs(id));
    }

    @GetMapping("/courses/catas/index/list/{id}")
    public AjaxResult lessons(@PathVariable Long id) {
        return success(educationService.catalogs(id, true));
    }

    @GetMapping("/recommend/{type}")
    public AjaxResult recommendations(@PathVariable String type) {
        return success(educationService.recommendations(type));
    }

    @GetMapping("/banners")
    public AjaxResult banners() {
        return success(educationService.banners());
    }

    @RequiresLogin
    @GetMapping("/interests")
    public AjaxResult interests() {
        return success(educationService.interests());
    }

    @RequiresLogin
    @PostMapping("/interests")
    public AjaxResult saveInterest(@RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) Long categoryId) {
        Long id = categoryId == null ? educationServiceLong(body, "categoryId") : categoryId;
        return success(educationService.saveInterest(id));
    }

    @GetMapping("/interests/{id}/courses")
    public AjaxResult interestCourses(@PathVariable Long id) {
        return success(educationService.interestCourses(id));
    }

    @RequiresLogin
    @GetMapping("/lessons/{courseId}")
    public AjaxResult learningCourse(@PathVariable Long courseId) {
        return success(educationService.learningCourse(courseId));
    }

    // /internal/enrollments/** 已迁移至 EducationInternalController 统一处理 (支持显式 userId 及当前登录用户兜底)

    /**
     * 重置当前用户指定课程的学习进度，保留报名关系和课程本身。
     */
    @RequiresLogin
    @PutMapping("/lessons/{courseId}/restart")
    public AjaxResult restartLearning(@PathVariable Long courseId) {
        return success(educationService.restartLearning(courseId));
    }

    @RequiresLogin
    @GetMapping("/lessons/page")
    public AjaxResult learningPage(@RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize) {
        return success(educationService.learningPage(pageNo, pageSize, false));
    }

    @RequiresLogin
    @GetMapping("/lessons/now")
    public AjaxResult learningNow(@RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize) {
        return success(educationService.learningPage(pageNo, pageSize, true));
    }

    @RequiresLogin
    @GetMapping({"/lessons/plans", "/plans"})
    public AjaxResult plans() {
        return success(educationService.plans());
    }

    @RequiresLogin
    @PostMapping({"/lessons/plans", "/plans"})
    public AjaxResult savePlan(@RequestBody EduLearningPlan plan) {
        return success(educationService.savePlan(plan));
    }

    @RequiresLogin
    @DeleteMapping("/lessons/{courseId}")
    public AjaxResult removeLearning(@PathVariable Long courseId) {
        educationService.removeLearning(courseId);
        return success();
    }

    @RequiresLogin
    @GetMapping("/learning-records/lessons/{lessonId}")
    public AjaxResult learningLog(@PathVariable Long lessonId) {
        return success(educationService.learningRecord(lessonId));
    }

    @RequiresLogin
    @PostMapping("/learning-records")
    public AjaxResult saveLearning(@RequestBody EduLearningRecord record) {
        return success(educationService.saveLearning(record));
    }

    @GetMapping("/questions/page")
    public AjaxResult questionPage(@RequestParam Map<String, Object> params) {
        return success(educationService.questionPage(params));
    }

    /**
     * 兼容旧前端 Mock 适配器使用的列表别名。
     *
     * <p>如果没有这个固定路径，/questions/{id} 会把 list 当成 Long
     * 解析并返回参数类型错误；真实前端和历史调用方都应得到同一分页结构。</p>
     */
    @GetMapping("/questions/list")
    public AjaxResult questionList(@RequestParam Map<String, Object> params) {
        return success(educationService.questionPage(params));
    }

    @GetMapping("/questions/checkName")
    public AjaxResult checkQuestionName(@RequestParam Map<String, Object> params) {
        return success(educationService.checkQuestionName(params));
    }

    @GetMapping("/questions/listOfBiz")
    public AjaxResult questionsOfBiz(@RequestParam(required = false) Long bizId) {
        return success(educationService.legacyBizQuestions(bizId));
    }

    @PostMapping({"/questions", "/questions/add"})
    @RequiresLogin
    public AjaxResult saveQuestion(@RequestBody Map<String, Object> body) {
        return success(educationService.saveQuestionPayload(body));
    }

    @GetMapping("/questions/{id}")
    public AjaxResult question(@PathVariable Long id) {
        return success(educationService.questionOrQuestionBank(id));
    }

    @RequiresLogin
    @PutMapping("/questions/{id}")
    public AjaxResult updateQuestion(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> value = new LinkedHashMap<>(body == null ? Map.of() : body);
        value.put("id", id);
        return success(educationService.saveQuestionPayload(value));
    }

    @RequiresLogin
    @DeleteMapping("/questions/{id}")
    public AjaxResult removeQuestion(@PathVariable Long id) {
        educationService.removeQuestionPayload(id);
        return success();
    }

    @GetMapping("/replies/page")
    public AjaxResult replyPage(@RequestParam Map<String, Object> params) {
        return success(educationService.replyPage(params));
    }

    @RequiresLogin
    @PostMapping("/replies")
    public AjaxResult saveReply(@RequestBody EduReply reply) {
        return success(educationService.saveReply(reply));
    }

    @GetMapping("/notes/page")
    public AjaxResult notePage(@RequestParam Map<String, Object> params) {
        return success(educationService.notePage(params));
    }

    @RequiresLogin
    @PostMapping({"/notes", "/notes/add"})
    public AjaxResult saveNote(@RequestBody EduNote note) {
        return success(educationService.saveNote(note));
    }

    /** 用户端笔记详情，兼容原智问前端的 GET /ls/notes/{id} 调用。 */
    @RequiresLogin
    @GetMapping("/notes/{id}")
    public AjaxResult note(@PathVariable Long id) {
        return success(educationService.note(id));
    }

    @RequiresLogin
    @PutMapping("/notes/{id}")
    public AjaxResult updateNote(@PathVariable Long id, @RequestBody EduNote note) {
        note.setId(id);
        return success(educationService.saveNote(note));
    }

    @RequiresLogin
    @DeleteMapping("/notes/{id}")
    public AjaxResult removeNote(@PathVariable Long id) {
        educationService.removeNote(id);
        return success();
    }

    @RequiresLogin
    @PostMapping({"/notes/{id}", "/notes/gathers/{id}"})
    public AjaxResult collectNote(@PathVariable Long id) {
        return success(educationService.collectNote(id, true));
    }

    @RequiresLogin
    @DeleteMapping("/notes/gathers/{id}")
    public AjaxResult uncollectNote(@PathVariable Long id) {
        return success(educationService.collectNote(id, false));
    }

    @RequiresLogin
    @PostMapping("/note/{id}")
    public AjaxResult likeNote(@PathVariable Long id) {
        return success(educationService.like("NOTE", id, true));
    }

    @RequiresLogin
    @DeleteMapping("/note/{id}")
    public AjaxResult unlikeNote(@PathVariable Long id) {
        return success(educationService.like("NOTE", id, false));
    }

    @PostMapping("/likes")
    @RequiresLogin
    public AjaxResult like(@RequestBody Map<String, Object> body) {
        Long id = educationServiceLong(body, "bizId");
        boolean liked = educationServiceBoolean(body, "liked");
        return success(educationService.like(String.valueOf(body.getOrDefault("bizType", "QA")), id, liked));
    }

    @RequiresLogin
    @GetMapping("/exams/page")
    public AjaxResult examPage(@RequestParam Map<String, Object> params) {
        return success(educationService.examsPage(params));
    }

    @GetMapping("/exams/{id}")
    public AjaxResult exam(@PathVariable Long id) {
        return success(educationService.exam(id));
    }

    @RequiresLogin
    @GetMapping("/exam-records/{id}")
    public AjaxResult examRecordDetails(@PathVariable Long id) {
        return success(educationService.examRecordDetails(id));
    }

    @PostMapping("/exams")
    public AjaxResult examQuestions(@RequestBody(required = false) Map<String, Object> body) {
        return success(educationService.examQuestions(body == null ? Map.of() : body));
    }

    @PostMapping("/exams/details")
    @RequiresLogin
    public AjaxResult submitExam(@RequestBody Map<String, Object> body) {
        return success(educationService.submitExam(body));
    }

    @PostMapping("/exam-records")
    @RequiresLogin
    public AjaxResult startExam(@RequestBody EduExamRecord record) {
        return success(educationService.startExam(record));
    }

    @PostMapping("/exam-records/details")
    @RequiresLogin
    public AjaxResult submitExamRecord(@RequestBody Map<String, Object> body) {
        return success(educationService.submitExam(body));
    }

    @GetMapping("/sign-records")
    @RequiresLogin
    public AjaxResult signInfo() {
        return success(educationService.signInfo());
    }

    @PostMapping("/sign-records")
    @RequiresLogin
    public AjaxResult sign() {
        return success(educationService.sign());
    }

    @GetMapping("/points/today")
    @RequiresLogin
    public AjaxResult pointsToday() {
        return success(educationService.pointsToday());
    }

    @GetMapping("/boards")
    public AjaxResult pointsBoard(@RequestParam Map<String, Object> params) {
        return success(educationService.pointsBoard(params));
    }

    @GetMapping("/boards/seasons/list")
    public AjaxResult seasons() {
        return success(List.of(Map.of("id", 0, "name", "2026 学习赛季", "value", 0)));
    }

    private Long educationServiceLong(Map<String, Object> body, String... keys) {
        if (body == null) return null;
        for (String key : keys) {
            Object value = body.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                try { return Long.valueOf(String.valueOf(value)); } catch (NumberFormatException ignored) { }
            }
        }
        return null;
    }

    private String educationServiceText(Map<String, Object> body, String... keys) {
        if (body == null) return null;
        for (String key : keys) {
            Object value = body.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }

    private Integer educationServiceInt(Map<String, Object> body, String... keys) {
        if (body == null) return null;
        for (String key : keys) {
            Object value = body.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                try { return Integer.valueOf(String.valueOf(value)); } catch (NumberFormatException ignored) { }
            }
        }
        return null;
    }

    private boolean educationServiceBoolean(Map<String, Object> body, String key) {
        Object value = body == null ? null : body.get(key);
        return value instanceof Boolean b ? b : "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value));
    }
}


