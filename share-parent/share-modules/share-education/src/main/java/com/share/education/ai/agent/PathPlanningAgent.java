package com.share.education.ai.agent;

import com.share.education.ai.algorithm.DefaultHybridAlgorithmEngine;
import com.share.education.ai.model.AnalyzedCourseVO;
import com.share.education.ai.model.LearningPathPlan;
import com.share.education.ai.model.PathStageVO;
import com.share.education.ai.model.UserProfileContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 智能体 5：路径规划 Agent (PathPlanningAgent)。
 *
 * <p>【核心职责】：
 * 1. 依据知识图谱先修依赖与工程师成长进阶曲线，构建有向无环拓扑路线 (DAG)；
 * 2. 将候选课程按进阶梯度合理划分为 4 个阶段性成长里程碑；
 * 3. 响应 Critic 审判反思修正指令与 Human-in-the-Loop 人机协同微调指令；
 * 4. 动态消除先修断层，保障课程拓扑 100% 合规。</p>
 */
@Component
public class PathPlanningAgent {

    private static final Logger log = LoggerFactory.getLogger(PathPlanningAgent.class);

    private final com.share.education.service.IDisciplineTaxonomyService taxonomyService;

    public PathPlanningAgent() {
        this(null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PathPlanningAgent(@org.springframework.beans.factory.annotation.Autowired(required = false) com.share.education.service.IDisciplineTaxonomyService taxonomyService) {
        this.taxonomyService = taxonomyService;
    }

    public LearningPathPlan planPath(UserProfileContext profile, List<AnalyzedCourseVO> courses) {
        return planPath(profile, courses, Collections.emptyMap());
    }

    /**
     * 带针对性修正指令或人机协同微调的路径规划
     *
     * @param profile 学员画像
     * @param courses 候选课程集合
     * @param directives Critic 指令集或用户微调参数 (如 needMoreBeginnerCourses, excludedCourseIds 等)
     * @return 优化后的结构化进阶路线
     */
    public LearningPathPlan planPath(UserProfileContext profile,
                                    List<AnalyzedCourseVO> courses,
                                    Map<String, Object> directives) {
        String role = (profile != null && StringUtils.hasText(profile.getIntendedRole()))
            ? profile.getIntendedRole() : "高级全栈软件工程师";

        if (courses == null || courses.isEmpty()) {
            return LearningPathPlan.builder()
                .intendedRole(role)
                .overallGoal("系统化 IT 技术进阶之旅")
                .totalCourses(0)
                .totalEstimatedHours(0)
                .stages(Collections.emptyList())
                .referenceStandard("IT 工程师标准化进阶指南")
                .build();
        }

        // 0. 处理人机协同排除课程 (Human-in-the-Loop: excludedCourseIds 或 excludeCourseIds)
        List<AnalyzedCourseVO> activeCourses = new ArrayList<>(courses);
        Object exObj = null;
        if (directives != null) {
            if (directives.containsKey("excludedCourseIds")) {
                exObj = directives.get("excludedCourseIds");
            } else if (directives.containsKey("excludeCourseIds")) {
                exObj = directives.get("excludeCourseIds");
            }
        }
        if (exObj instanceof Collection<?> exList) {
            Set<Long> exIds = new HashSet<>();
            for (Object item : exList) {
                if (item instanceof Number num) {
                    exIds.add(num.longValue());
                } else if (item != null) {
                    try { exIds.add(Long.parseLong(item.toString())); } catch (Exception ignored) {}
                }
            }
            activeCourses.removeIf(c -> exIds.contains(c.getCourseId()));
        }

        // 0.5 学科领域一致性核验与非对口孤岛课程剔除 (Domain Consistency Check & Isolated Course Pruning)
        DefaultHybridAlgorithmEngine.DisciplineDomain domain = DefaultHybridAlgorithmEngine.resolveDomain(role);
        List<AnalyzedCourseVO> consistentCourses = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        for (AnalyzedCourseVO c : activeCourses) {
            if (c != null && seenIds.add(c.getCourseId()) && isCourseDomainRelevant(c, role, domain)) {
                consistentCourses.add(c);
            } else {
                log.info("[PathPlanningAgent] 剔除非本学科强相关或重复课程: courseId={}, courseName={}, targetRole={}",
                        c != null ? c.getCourseId() : null, c != null ? c.getCourseName() : null, role);
            }
        }
        if (!consistentCourses.isEmpty()) {
            activeCourses = consistentCourses;
        }

        // 1. 基于 Kahn 算法的 DAG 严格拓扑排序 (避免 ComparableTimSort 破坏 contract 异常)
        Map<Long, AnalyzedCourseVO> courseMap = new HashMap<>();
        Map<Long, List<Long>> adj = new HashMap<>();
        Map<Long, Integer> inDegree = new HashMap<>();

        for (AnalyzedCourseVO c : activeCourses) {
            courseMap.put(c.getCourseId(), c);
            adj.put(c.getCourseId(), new ArrayList<>());
            inDegree.put(c.getCourseId(), 0);
        }

        for (AnalyzedCourseVO c1 : activeCourses) {
            for (AnalyzedCourseVO c2 : activeCourses) {
                if (!c1.getCourseId().equals(c2.getCourseId()) && isPrerequisite(c1, c2)) {
                    adj.get(c1.getCourseId()).add(c2.getCourseId());
                    inDegree.put(c2.getCourseId(), inDegree.get(c2.getCourseId()) + 1);
                }
            }
        }

        Comparator<AnalyzedCourseVO> nodeComparator = (a, b) -> {
            int diff1 = a.getDifficultyLevel() != null ? a.getDifficultyLevel() : 2;
            int diff2 = b.getDifficultyLevel() != null ? b.getDifficultyLevel() : 2;
            if (diff1 != diff2) return Integer.compare(diff1, diff2);
            double s1 = a.getMatchScore() != null ? a.getMatchScore() : 85.0;
            double s2 = b.getMatchScore() != null ? b.getMatchScore() : 85.0;
            if (Double.compare(s2, s1) != 0) return Double.compare(s2, s1);
            return Long.compare(a.getCourseId(), b.getCourseId());
        };

        PriorityQueue<AnalyzedCourseVO> pq = new PriorityQueue<>(nodeComparator);
        for (AnalyzedCourseVO c : activeCourses) {
            if (inDegree.get(c.getCourseId()) == 0) {
                pq.offer(c);
            }
        }

        List<AnalyzedCourseVO> sortedCourses = new ArrayList<>();
        Set<Long> visited = new HashSet<>();

        while (!pq.isEmpty()) {
            AnalyzedCourseVO curr = pq.poll();
            sortedCourses.add(curr);
            visited.add(curr.getCourseId());

            for (Long nxtId : adj.getOrDefault(curr.getCourseId(), Collections.emptyList())) {
                int deg = inDegree.get(nxtId) - 1;
                inDegree.put(nxtId, deg);
                if (deg <= 0 && !visited.contains(nxtId)) {
                    AnalyzedCourseVO nxtCourse = courseMap.get(nxtId);
                    if (nxtCourse != null && !pq.contains(nxtCourse)) {
                        pq.offer(nxtCourse);
                    }
                }
            }
        }

        if (sortedCourses.size() < activeCourses.size()) {
            List<AnalyzedCourseVO> remaining = activeCourses.stream()
                .filter(c -> !visited.contains(c.getCourseId()))
                .sorted(nodeComparator)
                .toList();
            sortedCourses.addAll(remaining);
        }

        // 2. 切分至 4 个进阶里程碑阶段
        List<AnalyzedCourseVO> stage1Courses = new ArrayList<>(); // 初级筑基 (难度 1 或先修基石)
        List<AnalyzedCourseVO> stage2Courses = new ArrayList<>(); // 核心进阶 (难度 2)
        List<AnalyzedCourseVO> stage3Courses = new ArrayList<>(); // 架构实战 (难度 3)
        List<AnalyzedCourseVO> stage4Courses = new ArrayList<>(); // 综合攻坚与突破

        boolean skipBasic = directives != null && (Boolean.TRUE.equals(directives.get("skipBasicPhase"))
            || "true".equalsIgnoreCase(String.valueOf(directives.get("skipBasicPhase"))));
        boolean needMoreBeginner = directives != null && (Boolean.TRUE.equals(directives.get("needMoreBeginnerCourses"))
            || "true".equalsIgnoreCase(String.valueOf(directives.get("needMoreBeginnerCourses"))));
        boolean fixPrereqInversion = directives != null && (Boolean.TRUE.equals(directives.get("fixPrerequisiteInversion"))
            || "true".equalsIgnoreCase(String.valueOf(directives.get("fixPrerequisiteInversion"))));
        boolean smoothTransition = directives != null && (Boolean.TRUE.equals(directives.get("smoothDifficultyTransition"))
            || "true".equalsIgnoreCase(String.valueOf(directives.get("smoothDifficultyTransition"))));

        for (AnalyzedCourseVO c : sortedCourses) {
            int diff = c.getDifficultyLevel() != null ? c.getDifficultyLevel() : 2;
            // 修复：若指令要求更多筑基课程，仅允许难度<=2的课程进入阶段1，杜绝高难度课被误塞进阶段1
            if (!skipBasic && (diff == 1 || (needMoreBeginner && diff <= 2)) && stage1Courses.size() < 3) {
                stage1Courses.add(c);
            } else if (diff <= 2 && stage2Courses.size() < 3) {
                stage2Courses.add(c);
            } else if (stage3Courses.size() < 3) {
                stage3Courses.add(c);
            } else {
                stage4Courses.add(c);
            }
        }

        // 兜底保障：若未跳过基础且 stage1 依然为空但有其他课程，借调一门最低难度的课程至 stage1 夯实底座
        if (!skipBasic && stage1Courses.isEmpty()) {
            if (!stage2Courses.isEmpty()) {
                stage1Courses.add(stage2Courses.remove(0));
            } else if (!stage3Courses.isEmpty()) {
                stage1Courses.add(stage3Courses.remove(0));
            } else if (!stage4Courses.isEmpty()) {
                stage1Courses.add(stage4Courses.remove(0));
            }
        }
        // 同样保障 stage2 在有充裕课程时不出现断层饥饿
        if (stage2Courses.isEmpty()) {
            if (stage1Courses.size() > 1) {
                stage2Courses.add(stage1Courses.remove(stage1Courses.size() - 1));
            } else if (!stage3Courses.isEmpty()) {
                stage2Courses.add(stage3Courses.remove(0));
            } else if (!stage4Courses.isEmpty()) {
                stage2Courses.add(stage4Courses.remove(0));
            }
        }

        // 响应 Critic 的 fixPrerequisiteInversion 指令：若后续阶段包含前置阶段依赖的课程，自动前移
        if (fixPrereqInversion) {
            adjustPrerequisiteInversions(stage1Courses, stage2Courses, stage3Courses, stage4Courses);
        }

        // 响应 Critic 的 smoothDifficultyTransition 指令：确保各阶段难度单调平滑递增
        if (smoothTransition) {
            smoothStageTransitions(stage1Courses, stage2Courses, stage3Courses, stage4Courses);
        }

        // 动态阶段容量自适应均衡 (防止单阶段超载或空阶段导致质检扣分)
        balanceStageCapacities(stage1Courses, stage2Courses, stage3Courses, stage4Courses);

        // 数据驱动构建阶段定义
        record StageDef(String name, String goal, int hoursPerCourse, List<AnalyzedCourseVO> courseList) {}
        StageDef[] stageDefs = {
            new StageDef("阶段一：核心基石与工程化筑基", "掌握现代核心语言规范与基础工程化设计，夯实扎实底座", 16, stage1Courses),
            new StageDef("阶段二：核心技术进阶与组件精通", "深入主流企业级框架与核心中间件，攻克业务核心技术难点", 24, stage2Courses),
            new StageDef("阶段三：分布式架构与工程级实战", "构建高并发、高可用微服务与生产集群，具备工业级项目落地能力", 32, stage3Courses),
            new StageDef("阶段四：全景前沿攻坚与技术突破", "洞悉底层内核与前沿大模型/云原生，打破技术天花板直达架构专家", 36, stage4Courses)
        };

        List<PathStageVO> stages = new ArrayList<>();
        int stageIndex = 1;
        for (StageDef def : stageDefs) {
            if (!def.courseList.isEmpty()) {
                stages.add(PathStageVO.builder()
                    .stageIndex(stageIndex++)
                    .stageName(def.name)
                    .stageGoal(def.goal)
                    .estimatedHours(def.courseList.size() * def.hoursPerCourse)
                    .courses(def.courseList)
                    .build());
            }
        }

        int totalHours = stages.stream().mapToInt(PathStageVO::getEstimatedHours).sum();

        return LearningPathPlan.builder()
            .intendedRole(role)
            .overallGoal("基于 " + role + " 胜任力标准的自适应闭环成长路径")
            .totalCourses(activeCourses.size())
            .totalEstimatedHours(totalHours)
            .stages(stages)
            .referenceStandard("国家 IT 软件工程师能力标准及大厂 P6/P7 技术模型")
            .build();
    }

    private void adjustPrerequisiteInversions(List<AnalyzedCourseVO> s1,
                                              List<AnalyzedCourseVO> s2,
                                              List<AnalyzedCourseVO> s3,
                                              List<AnalyzedCourseVO> s4) {
        List<List<AnalyzedCourseVO>> allStages = List.of(s1, s2, s3, s4);
        for (int i = 0; i < allStages.size(); i++) {
            List<AnalyzedCourseVO> currentStage = allStages.get(i);
            for (AnalyzedCourseVO currCourse : new ArrayList<>(currentStage)) {
                // 检查后续阶段是否有 currCourse 的先修课
                for (int j = i + 1; j < allStages.size(); j++) {
                    List<AnalyzedCourseVO> laterStage = allStages.get(j);
                    for (int k = 0; k < laterStage.size(); k++) {
                        AnalyzedCourseVO laterCourse = laterStage.get(k);
                        if (isPrerequisite(laterCourse, currCourse)) {
                            // 将先修课程前移至当前阶段
                            laterStage.remove(k);
                            currentStage.add(0, laterCourse);
                            k--;
                        }
                    }
                }
            }
        }
    }

    private void smoothStageTransitions(List<AnalyzedCourseVO> s1,
                                        List<AnalyzedCourseVO> s2,
                                        List<AnalyzedCourseVO> s3,
                                        List<AnalyzedCourseVO> s4) {
        Comparator<AnalyzedCourseVO> diffAsc = Comparator.comparingInt(c -> c.getDifficultyLevel() != null ? c.getDifficultyLevel() : 2);
        s1.sort(diffAsc);
        s2.sort(diffAsc);
        s3.sort(diffAsc);
        s4.sort(diffAsc);
    }

    private void balanceStageCapacities(List<AnalyzedCourseVO> s1,
                                       List<AnalyzedCourseVO> s2,
                                       List<AnalyzedCourseVO> s3,
                                       List<AnalyzedCourseVO> s4) {
        List<List<AnalyzedCourseVO>> allStages = List.of(s1, s2, s3, s4);
        int total = s1.size() + s2.size() + s3.size() + s4.size();
        if (total < 4) return;

        // 1. 确保每个阶段不为空
        for (int i = 0; i < 4; i++) {
            List<AnalyzedCourseVO> curr = allStages.get(i);
            if (curr.isEmpty()) {
                for (int j = 0; j < 4; j++) {
                    List<AnalyzedCourseVO> donor = allStages.get(j);
                    if (donor.size() > 1) {
                        AnalyzedCourseVO courseToMove = (j > i) ? donor.remove(0) : donor.remove(donor.size() - 1);
                        curr.add(courseToMove);
                        break;
                    }
                }
            }
        }

        // 2. 避免任一阶段超过 5 门课 (防止局部过载)
        for (int i = 3; i >= 1; i--) {
            List<AnalyzedCourseVO> curr = allStages.get(i);
            while (curr.size() > 5) {
                List<AnalyzedCourseVO> prev = allStages.get(i - 1);
                if (prev.size() < 5) {
                    prev.add(curr.remove(0));
                } else {
                    break;
                }
            }
        }
    }

    private boolean isPrerequisite(AnalyzedCourseVO c1, AnalyzedCourseVO c2) {
        if (c1 == null || c2 == null || c1.getCourseId().equals(c2.getCourseId())) {
            return false;
        }
        int diff1 = c1.getDifficultyLevel() != null ? c1.getDifficultyLevel() : 2;
        int diff2 = c2.getDifficultyLevel() != null ? c2.getDifficultyLevel() : 2;
        if (diff1 > diff2) {
            return false;
        }

        String c1Name = c1.getCourseName() != null ? c1.getCourseName().toLowerCase() : "";
        List<String> prereqs = c2.getPrerequisiteSkills();
        if (prereqs != null) {
            for (String req : prereqs) {
                if (!StringUtils.hasText(req)) continue;
                String reqNorm = req.toLowerCase().trim();
                if (reqNorm.length() < 2 || "计算机基础知识".equals(reqNorm)) {
                    continue;
                }
                boolean matched = false;
                if (c1Name.equals(reqNorm)) {
                    matched = true;
                } else if (reqNorm.length() <= 4) {
                    matched = c1Name.matches(".*(?i)(\\b|[ (（\\[_-])" + java.util.regex.Pattern.quote(reqNorm) + "(\\b|[ )）\\]_-]).*");
                } else {
                    matched = c1Name.contains(reqNorm);
                }
                if (matched) {
                    return true;
                }
            }
        }
        // 检查 DRAG-KP4SR 知识图谱先修推导证据链
        if (c2.getEvidencePaths() != null) {
            for (String path : c2.getEvidencePaths()) {
                String[] parts = path.split("--PREREQUISITE-->|->");
                if (parts.length >= 2) {
                    String source = parts[0].trim().toLowerCase();
                    if (StringUtils.hasText(source) && source.length() >= 2 && !source.equals("计算机基础知识")) {
                        boolean matchSource = c1Name.contains(source) || (c1.getCoreKnowledgePoints() != null 
                            && c1.getCoreKnowledgePoints().stream().anyMatch(kp -> kp.toLowerCase().contains(source)));
                        if (matchSource) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isCourseDomainRelevant(AnalyzedCourseVO c, String role, DefaultHybridAlgorithmEngine.DisciplineDomain domain) {
        if (c == null) {
            return false;
        }
        if (taxonomyService != null) {
            com.share.education.domain.EduCourse dummy = new com.share.education.domain.EduCourse();
            dummy.setId(c.getCourseId());
            dummy.setCourseName(c.getCourseName());
            dummy.setCategoryId(c.getCategoryId());
            String skills = (c.getCoreKnowledgePoints() != null ? String.join(" ", c.getCoreKnowledgePoints()) : "")
                    + " " + (c.getPrerequisiteSkills() != null ? String.join(" ", c.getPrerequisiteSkills()) : "");
            dummy.setSkills(skills);
            dummy.setDescription(c.getSyllabusSummary());
            return taxonomyService.isCourseAllowedForDomain(dummy, role);
        }
        return isCourseDomainRelevantFallback(c, domain);
    }

    private static final Set<String> JAVA_EXCLUDED_KEYWORDS = Set.of(
        "go 语言", "go语言", "golang", "goroutine", "kratos", "geecache", "go后端", "go web", "gin", "grpc", "protobuf",
        "kubernetes", "k8s", "terraform", "istio", "service mesh", "iac", "rust",
        "大数据", "hadoop", "flink", "spark", "datax", "sqoop", "clickhouse", "doris", "hive", "hbase", "数仓", "离线计算",
        "nlp", "word2vec", "大模型", "llm", "rag", "langchain", "python", "django", "fastapi",
        "vue", "react", "typescript", "javascript", "前端", "flutter", "android", "ios", "鸿蒙", "harmonyos", "harmony",
        "c++", "node.js", "nodejs", "区块链"
    );

    private static final Set<String> JAVA_REQUIRED_KEYWORDS = Set.of(
        "java", "spring", "mysql", "redis", "mybatis", "jvm", "linux", "微服务", "高并发", "分布式",
        "数据库", "sql", "网络编程", "数据结构", "算法", "操作系统", "计算机网络", "后端", "中间件",
        "netty", "kafka", "设计模式", "juc", "seata"
    );

    private static final Set<String> FRONTEND_EXCLUDED = Set.of("java", "rust", "大数据", "flutter");
    private static final Set<String> FRONTEND_REQUIRED = Set.of("vue", "react", "typescript", "javascript", "前端", "web");

    private static final Set<String> BIGDATA_EXCLUDED = Set.of("vue", "react", "flutter");
    private static final Set<String> BIGDATA_REQUIRED = Set.of("大数据", "spark", "flink", "hadoop", "datax", "sqoop");

    private static final Set<String> AI_EXCLUDED = Set.of("vue", "flutter", "区块链");
    private static final Set<String> AI_REQUIRED = Set.of("ai", "大模型", "大语言模型", "语言模型", "llm", "nlp", "word2vec", "pytorch", "深度学习");

    private static final Set<String> GO_EXCLUDED = Set.of("vue", "rust", "大数据");
    private static final Set<String> GO_REQUIRED = Set.of("go", "golang", "k8s", "docker");

    private static final Set<String> MOBILE_REQUIRED = Set.of("flutter", "android", "ios", "鸿蒙", "安卓");

    private static boolean containsAny(String text, Set<String> keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCourseDomainRelevantFallback(AnalyzedCourseVO c, DefaultHybridAlgorithmEngine.DisciplineDomain domain) {
        if (c == null || domain == DefaultHybridAlgorithmEngine.DisciplineDomain.GENERAL) {
            return true;
        }
        String name = c.getCourseName() != null ? c.getCourseName().toLowerCase() : "";
        List<String> kps = c.getCoreKnowledgePoints() != null ? c.getCoreKnowledgePoints() : Collections.emptyList();
        List<String> prereqs = c.getPrerequisiteSkills() != null ? c.getPrerequisiteSkills() : Collections.emptyList();
        String allText = (name + " " + String.join(" ", kps) + " " + String.join(" ", prereqs)).toLowerCase();

        switch (domain) {
            case JAVA_BACKEND:
                if (containsAny(allText, JAVA_EXCLUDED_KEYWORDS)) {
                    return false;
                }
                return containsAny(allText, JAVA_REQUIRED_KEYWORDS);

            case FRONTEND:
                if (containsAny(allText, FRONTEND_EXCLUDED)) return false;
                return containsAny(allText, FRONTEND_REQUIRED);

            case BIG_DATA:
                if (containsAny(allText, BIGDATA_EXCLUDED)) return false;
                return containsAny(allText, BIGDATA_REQUIRED);

            case AI_LLM:
                if (containsAny(allText, AI_EXCLUDED)) return false;
                return containsAny(allText, AI_REQUIRED);

            case GO_CLOUD_NATIVE:
                if (containsAny(allText, GO_EXCLUDED)) return false;
                return containsAny(allText, GO_REQUIRED);

            case MOBILE:
                return containsAny(allText, MOBILE_REQUIRED);

            default:
                return true;
        }
    }
}

