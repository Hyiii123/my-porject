package com.share.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.common.redis.service.RedisService;
import com.share.education.domain.EduCourse;
import com.share.education.domain.EduDisciplineTaxonomy;
import com.share.education.domain.EduKnowledgePrerequisite;
import com.share.education.mapper.EduDisciplineTaxonomyMapper;
import com.share.education.mapper.EduKnowledgePrerequisiteMapper;
import com.share.education.service.IDisciplineTaxonomyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class DisciplineTaxonomyServiceImpl implements IDisciplineTaxonomyService {

    private static final Logger log = LoggerFactory.getLogger(DisciplineTaxonomyServiceImpl.class);

    private static final String TAXONOMY_CACHE_KEY = "edu:discipline:taxonomy:list";
    private static final String PREREQ_CACHE_KEY = "edu:knowledge:prereq:list";

    private final EduDisciplineTaxonomyMapper taxonomyMapper;
    private final EduKnowledgePrerequisiteMapper prerequisiteMapper;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    // 本地读写缓存，防止高并发下频繁序列化
    private volatile List<TaxonomyCompiledRule> compiledRulesCache = null;
    private volatile List<EduKnowledgePrerequisite> prereqListCache = null;
    private volatile long lastCacheTime = 0L;
    private static final long LOCAL_CACHE_EXPIRE_MS = 60_000L; // 1分钟本地刷新

    public DisciplineTaxonomyServiceImpl(EduDisciplineTaxonomyMapper taxonomyMapper,
                                         EduKnowledgePrerequisiteMapper prerequisiteMapper,
                                         RedisService redisService,
                                         ObjectMapper objectMapper) {
        this.taxonomyMapper = taxonomyMapper;
        this.prerequisiteMapper = prerequisiteMapper;
        this.redisService = redisService;
        this.objectMapper = objectMapper;
    }

    private static class TaxonomyCompiledRule {
        EduDisciplineTaxonomy raw;
        Set<Long> allowedCategoryIds = new HashSet<>();
        List<String> whitelistKeywords = new ArrayList<>();
        List<String> blacklistKeywords = new ArrayList<>();
        List<String> targetRolePatterns = new ArrayList<>();
    }

    private synchronized void ensureCacheLoaded() {
        long now = System.currentTimeMillis();
        if (compiledRulesCache != null && (now - lastCacheTime) < LOCAL_CACHE_EXPIRE_MS) {
            return;
        }

        try {
            List<EduDisciplineTaxonomy> taxonomies = taxonomyMapper.selectList(
                new LambdaQueryWrapper<EduDisciplineTaxonomy>()
                    .eq(EduDisciplineTaxonomy::getDelFlag, 0)
                    .orderByAsc(EduDisciplineTaxonomy::getSortNum)
            );

            List<TaxonomyCompiledRule> compiled = new ArrayList<>();
            for (EduDisciplineTaxonomy t : taxonomies) {
                TaxonomyCompiledRule rule = new TaxonomyCompiledRule();
                rule.raw = t;
                if (StringUtils.hasText(t.getAllowedCategoryIds())) {
                    try {
                        List<Long> cids = objectMapper.readValue(t.getAllowedCategoryIds(), new TypeReference<List<Long>>() {});
                        rule.allowedCategoryIds.addAll(cids);
                    } catch (Exception ignored) {}
                }
                if (StringUtils.hasText(t.getWhitelistKeywords())) {
                    try {
                        List<String> wls = objectMapper.readValue(t.getWhitelistKeywords(), new TypeReference<List<String>>() {});
                        rule.whitelistKeywords.addAll(wls.stream().map(String::toLowerCase).toList());
                    } catch (Exception ignored) {}
                }
                if (StringUtils.hasText(t.getBlacklistKeywords())) {
                    try {
                        List<String> bls = objectMapper.readValue(t.getBlacklistKeywords(), new TypeReference<List<String>>() {});
                        rule.blacklistKeywords.addAll(bls.stream().map(String::toLowerCase).toList());
                    } catch (Exception ignored) {}
                }
                if (StringUtils.hasText(t.getTargetRolePatterns())) {
                    try {
                        List<String> patterns = objectMapper.readValue(t.getTargetRolePatterns(), new TypeReference<List<String>>() {});
                        rule.targetRolePatterns.addAll(patterns.stream().map(String::toLowerCase).toList());
                    } catch (Exception ignored) {}
                }
                compiled.add(rule);
            }
            this.compiledRulesCache = compiled;

            List<EduKnowledgePrerequisite> prereqs = prerequisiteMapper.selectList(
                new LambdaQueryWrapper<EduKnowledgePrerequisite>()
                    .eq(EduKnowledgePrerequisite::getDelFlag, 0)
                    .orderByDesc(EduKnowledgePrerequisite::getStrengthWeight)
            );
            this.prereqListCache = prereqs != null ? prereqs : Collections.emptyList();
            this.lastCacheTime = now;
        } catch (Exception ex) {
            log.error("加载学科领域与先修知识元数据异常", ex);
        }
    }

    @Override
    public EduDisciplineTaxonomy resolveTaxonomy(String intendedRole) {
        ensureCacheLoaded();
        if (compiledRulesCache == null || compiledRulesCache.isEmpty()) {
            return null;
        }

        if (!StringUtils.hasText(intendedRole)) {
            return getDefaultTaxonomy();
        }

        String lowerRole = intendedRole.toLowerCase();

        // 1. 优先根据 targetRolePatterns 匹配特定细分领域
        for (TaxonomyCompiledRule rule : compiledRulesCache) {
            for (String pattern : rule.targetRolePatterns) {
                if (lowerRole.contains(pattern)) {
                    return rule.raw;
                }
            }
        }

        // 2. 兜底返回默认领域
        return getDefaultTaxonomy();
    }

    private EduDisciplineTaxonomy getDefaultTaxonomy() {
        if (compiledRulesCache == null) return null;
        return compiledRulesCache.stream()
            .filter(r -> r.raw != null && Integer.valueOf(1).equals(r.raw.getIsDefault()))
            .map(r -> r.raw)
            .findFirst()
            .orElseGet(() -> compiledRulesCache.isEmpty() ? null : compiledRulesCache.get(0).raw);
    }

    @Override
    public boolean isCourseAllowedForDomain(EduCourse course, String intendedRole) {
        if (course == null) return false;
        ensureCacheLoaded();
        if (compiledRulesCache == null || compiledRulesCache.isEmpty()) {
            return true; // 元数据未就绪时放行
        }

        EduDisciplineTaxonomy taxonomy = resolveTaxonomy(intendedRole);
        if (taxonomy == null) return true;

        TaxonomyCompiledRule matchedRule = compiledRulesCache.stream()
            .filter(r -> r.raw.getDomainCode().equalsIgnoreCase(taxonomy.getDomainCode()))
            .findFirst()
            .orElse(null);

        if (matchedRule == null) return true;

        // 1. 门类白名单校验
        if (!matchedRule.allowedCategoryIds.isEmpty()) {
            if (course.getCategoryId() != null && !matchedRule.allowedCategoryIds.contains(course.getCategoryId())) {
                return false;
            }
        }

        // 组装课程综合文本特征
        String cName = course.getCourseName() != null ? course.getCourseName().toLowerCase() : "";
        String skills = course.getSkills() != null ? course.getSkills().toLowerCase() : "";
        String targetRole = course.getTargetRole() != null ? course.getTargetRole().toLowerCase() : "";
        String desc = course.getDescription() != null ? course.getDescription().toLowerCase() : "";
        String combined = cName + " " + skills + " " + targetRole + " " + desc;

        // 2. 强排他黑名单过滤 (排除非本领域课程)
        for (String blKw : matchedRule.blacklistKeywords) {
            if (combined.contains(blKw)) {
                return false;
            }
        }

        // 3. 白名单核心关键词命中校验 (至少命中一项本领域白名单技能)
        if (!matchedRule.whitelistKeywords.isEmpty()) {
            boolean matchedAny = false;
            for (String wlKw : matchedRule.whitelistKeywords) {
                if (combined.contains(wlKw)) {
                    matchedAny = true;
                    break;
                }
            }
            return matchedAny;
        }

        return true;
    }

    @Override
    public List<String> getPrerequisitesForCourse(String courseName, String skills) {
        ensureCacheLoaded();
        if (prereqListCache == null || prereqListCache.isEmpty()) {
            return List.of("计算机基础知识");
        }

        String text = ((courseName != null ? courseName : "") + " " + (skills != null ? skills : "")).toLowerCase();
        List<String> results = new ArrayList<>();

        for (EduKnowledgePrerequisite p : prereqListCache) {
            if (p.getConceptName() != null && text.contains(p.getConceptName().toLowerCase())) {
                if (StringUtils.hasText(p.getPrerequisiteConcept()) && !results.contains(p.getPrerequisiteConcept())) {
                    results.add(p.getPrerequisiteConcept());
                }
            }
        }

        if (results.isEmpty()) {
            return List.of("计算机基础知识");
        }
        return results.stream().limit(4).toList();
    }

    @Override
    public void refreshCache() {
        this.compiledRulesCache = null;
        this.prereqListCache = null;
        this.lastCacheTime = 0L;
        try {
            redisService.deleteObject(TAXONOMY_CACHE_KEY);
            redisService.deleteObject(PREREQ_CACHE_KEY);
        } catch (Exception ignored) {}
        log.info("[DisciplineTaxonomyService] 学科领域与先修图谱缓存已主动刷新");
    }
}
