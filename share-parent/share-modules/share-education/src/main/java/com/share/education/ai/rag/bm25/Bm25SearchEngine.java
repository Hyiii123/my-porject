package com.share.education.ai.rag.bm25;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.education.domain.EduCourse;
import com.share.education.mapper.EduCourseMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 原生 Okapi BM25 稀疏倒排检索引擎。
 *
 * <p>专为技术术语、版本号、框架名词（如 "Seata 2.0", "RocketMQ Dledger", "Vue3 Composition API", "SpringCloud Alibaba"）
 * 提供极速、精准的稀疏关键词召回，与 Dense 密集向量形成强互补。</p>
 */
@Component
public class Bm25SearchEngine {

    private static final Logger log = LoggerFactory.getLogger(Bm25SearchEngine.class);

    private static final double K1 = 1.5;
    private static final double B = 0.75;
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[\\s,，、/|+-_]+");

    private final EduCourseMapper courseMapper;

    /** 倒排索引表: Term -> Map<DocId, TermFrequency> */
    private final Map<String, Map<Long, Integer>> invertedIndex = new ConcurrentHashMap<>();

    /** 文档长度表: DocId -> DocumentLength */
    private final Map<Long, Integer> docLengths = new ConcurrentHashMap<>();

    /** 文档实体元数据缓存: DocId -> Course */
    private final Map<Long, EduCourse> docCache = new ConcurrentHashMap<>();

    private double avgDocLength = 0.0;
    private int totalDocs = 0;

    public Bm25SearchEngine(@org.springframework.beans.factory.annotation.Autowired(required = false) EduCourseMapper courseMapper) {
        this.courseMapper = courseMapper;
    }

    @PostConstruct
    public void initIndex() {
        if (courseMapper == null) {
            log.info("[Bm25Engine] courseMapper 未注入，跳过数据库预热");
            return;
        }
        try {
            List<EduCourse> courses = courseMapper.selectList(
                    new LambdaQueryWrapper<EduCourse>().eq(EduCourse::getStatus, 1));
            buildIndex(courses);
            log.info("[Bm25Engine] 成功为 {} 门生产课程构建 Okapi BM25 倒排索引表，平均文档词长: {:.1f}",
                    courses.size(), avgDocLength);
        } catch (Exception ex) {
            log.warn("[Bm25Engine] 初始化索引构建异常: {}", ex.getMessage());
        }
    }

    public synchronized void buildIndex(List<EduCourse> courses) {
        invertedIndex.clear();
        docLengths.clear();
        docCache.clear();

        if (courses == null || courses.isEmpty()) {
            totalDocs = 0;
            avgDocLength = 0.0;
            return;
        }

        long totalLenSum = 0;
        for (EduCourse c : courses) {
            if (c == null || c.getId() == null) continue;
            docCache.put(c.getId(), c);

            String text = String.join(" ",
                    Optional.ofNullable(c.getCourseName()).orElse(""),
                    Optional.ofNullable(c.getDescription()).orElse(""),
                    Optional.ofNullable(c.getShortDescription()).orElse(""),
                    Optional.ofNullable(c.getSkills()).orElse(""),
                    Optional.ofNullable(c.getTargetRole()).orElse(""),
                    Optional.ofNullable(c.getPrerequisites()).orElse("")
            );

            List<String> tokens = tokenize(text);
            docLengths.put(c.getId(), tokens.size());
            totalLenSum += tokens.size();

            Map<String, Integer> tf = new HashMap<>();
            for (String t : tokens) {
                tf.put(t, tf.getOrDefault(t, 0) + 1);
            }

            for (Map.Entry<String, Integer> entry : tf.entrySet()) {
                invertedIndex.computeIfAbsent(entry.getKey(), k -> new ConcurrentHashMap<>())
                        .put(c.getId(), entry.getValue());
            }
        }

        totalDocs = docLengths.size();
        avgDocLength = totalDocs > 0 ? (double) totalLenSum / totalDocs : 0.0;
    }

    /**
     * 执行 Okapi BM25 关键词精确匹配打分检索
     *
     * @param query 查询关键词
     * @param limit 返回条数上限
     * @return 命中课程列表 (按 BM25 得分倒序排列)
     */
    public List<Map<String, Object>> search(String query, int limit) {
        if (!StringUtils.hasText(query) || totalDocs == 0) {
            return Collections.emptyList();
        }

        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Double> scores = new HashMap<>();

        for (String qToken : queryTokens) {
            Map<Long, Integer> postings = invertedIndex.get(qToken);
            if (postings == null || postings.isEmpty()) {
                continue;
            }

            int n_q = postings.size();
            // Okapi BM25 IDF: ln(1 + (N - n(q) + 0.5) / (n(q) + 0.5))
            double idf = Math.log(1.0 + (totalDocs - n_q + 0.5) / (n_q + 0.5));
            if (idf <= 0.0) idf = 0.1; // 最小平滑

            for (Map.Entry<Long, Integer> p : postings.entrySet()) {
                Long docId = p.getKey();
                int f_q = p.getValue();
                int docLen = docLengths.getOrDefault(docId, (int) avgDocLength);

                // BM25 TF: (f(q,D) * (k1 + 1)) / (f(q,D) + k1 * (1 - b + b * (|D| / avgdl)))
                double tfPart = (f_q * (K1 + 1.0)) / (f_q + K1 * (1.0 - B + B * (docLen / Math.max(1.0, avgDocLength))));
                double termScore = idf * tfPart;

                scores.put(docId, scores.getOrDefault(docId, 0.0) + termScore);
            }
        }

        if (scores.isEmpty()) {
            return Collections.emptyList();
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit > 0 ? limit : 10)
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    EduCourse c = docCache.get(e.getKey());
                    item.put("id", e.getKey());
                    item.put("bm25Score", Math.round(e.getValue() * 100.0) / 100.0);
                    item.put("title", c != null ? c.getCourseName() : "课程 #" + e.getKey());
                    item.put("courseName", c != null ? c.getCourseName() : "课程 #" + e.getKey());
                    item.put("skills", c != null ? c.getSkills() : "");
                    item.put("price", c != null ? c.getPrice() : 0);
                    item.put("cover", c != null ? c.getCoverUrl() : "");
                    return item;
                })
                .collect(Collectors.toList());
    }

    public static List<String> tokenize(String text) {
        if (!StringUtils.hasText(text)) return Collections.emptyList();
        List<String> tokens = new ArrayList<>();
        String[] words = TOKEN_SPLIT.split(text.trim().toLowerCase());
        for (String w : words) {
            w = w.trim();
            if (w.length() >= 2) {
                tokens.add(w);
            }
        }
        return tokens;
    }
}
