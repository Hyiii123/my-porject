package com.share.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.common.core.exception.ServiceException;
import com.share.common.security.utils.SecurityUtils;
import com.share.education.domain.*;
import com.share.education.mapper.*;
import com.share.education.service.IEduCourseService;
import com.share.education.service.IEduExamService;
import com.share.education.service.IEduInteractionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.share.education.service.support.EduUtils.*;

/**
 * 考试测评、题库管理与答卷批改领域服务实现
 */
@Slf4j
@Service
@Primary
public class EduExamServiceImpl implements IEduExamService {

    private static final int ENABLED = 1;

    private final EduExamMapper examMapper;
    private final EduExamQuestionBankMapper questionBankMapper;
    private final EduExamQuestionMapper examQuestionMapper;
    private final EduExamRecordMapper examRecordMapper;
    private final EduExamAnswerMapper examAnswerMapper;
    private final EduCatalogQuestionMapper catalogQuestionMapper;
    private final ObjectMapper objectMapper;
    private final IEduCourseService courseService;
    private final IEduInteractionService interactionService;
    private final com.share.education.service.support.grading.QuestionGradingFactory gradingFactory;

    public EduExamServiceImpl(EduExamMapper examMapper,
                              EduExamQuestionBankMapper questionBankMapper,
                              EduExamQuestionMapper examQuestionMapper,
                              EduExamRecordMapper examRecordMapper,
                              EduExamAnswerMapper examAnswerMapper,
                              EduCatalogQuestionMapper catalogQuestionMapper,
                              ObjectMapper objectMapper,
                              IEduCourseService courseService,
                              @Lazy IEduInteractionService interactionService,
                              com.share.education.service.support.grading.QuestionGradingFactory gradingFactory) {
        this.examMapper = examMapper;
        this.questionBankMapper = questionBankMapper;
        this.examQuestionMapper = examQuestionMapper;
        this.examRecordMapper = examRecordMapper;
        this.examAnswerMapper = examAnswerMapper;
        this.catalogQuestionMapper = catalogQuestionMapper;
        this.objectMapper = objectMapper;
        this.courseService = courseService;
        this.interactionService = interactionService;
        this.gradingFactory = gradingFactory;
    }

    @Override
    public long generateStageExamId() {
        return newId();
    }

    @Override
    public boolean isQuestionBankPayload(Map<String, ?> payload) {
        if (payload == null) return false;
        return payload.containsKey("questionType") || payload.containsKey("options")
                || payload.containsKey("answer") || payload.containsKey("correctAnswer")
                || payload.containsKey("stem") || payload.containsKey("type") && !payload.containsKey("content");
    }

    @Override
    @Transactional
    public Object saveQuestionPayload(Map<String, ?> payload) {
        if (!isQuestionBankPayload(payload)) {
            EduQuestion value = new EduQuestion();
            Long id = longValue(payload.get("id"));
            value.setId(id);
            value.setCourseId(longValue(payload.get("courseId")));
            value.setTitle((String) payload.get("title"));
            String content = defaultText(payload.get("content"), defaultText(payload.get("description"), null));
            value.setContent(content);
            Object sectionObj = payload.get("sectionId");
            if (sectionObj != null && StringUtils.hasText(String.valueOf(sectionObj))) {
                value.setCategory(String.valueOf(sectionObj));
            } else if (payload.containsKey("category")) {
                value.setCategory((String) payload.get("category"));
            }
            return interactionService.saveQuestion(value);
        }
        Long id = longValue(payload.get("id"));
        EduExamQuestionBank value = id == null ? new EduExamQuestionBank() : questionBankMapper.selectById(id);
        if (value == null) throw new ServiceException("题目不存在");
        String stem = defaultText(payload.get("title"), defaultText(payload.get("stem"), null));
        require(StringUtils.hasText(stem), "题目内容不能为空");
        value.setQuestionType(normalizeQuestionType(defaultText(payload.get("type"),
                defaultText(payload.get("questionType"), value.getQuestionType()))));
        value.setCategoryId(defaultValue(longValue(payload.get("categoryId")), value.getCategoryId()));
        value.setStem(stem);
        Object options = payload.get("options");
        if (options != null) {
            try {
                value.setOptionsJson(objectMapper.writeValueAsString(options));
            } catch (Exception ex) {
                throw new ServiceException("题目选项格式不正确");
            }
        }
        value.setCorrectAnswer(defaultText(payload.get("answer"),
                defaultText(payload.get("correctAnswer"), value.getCorrectAnswer())));
        value.setScore(decimalValue(payload.get("score"), defaultValue(value.getScore(), BigDecimal.TEN)));
        value.setDifficulty(intValue(payload.get("difficulty"), defaultValue(value.getDifficulty(), 2)));
        value.setStatus(defaultValue(value.getStatus(), ENABLED));
        LocalDateTime now = LocalDateTime.now();
        if (value.getId() == null) {
            value.setId(newId());
            value.setCreateTime(now);
            value.setUpdateTime(now);
            value.setDelFlag(0);
            value.setVersion(0);
            questionBankMapper.insert(value);
        } else {
            value.setUpdateTime(now);
            questionBankMapper.updateById(value);
        }
        return legacyQuestionView(value);
    }

    @Override
    public Map<String, Object> checkQuestionName(Map<String, ?> params) {
        String stem = defaultText(params == null ? null : params.get("title"),
                defaultText(params == null ? null : params.get("stem"), null));
        Long id = longValue(params == null ? null : params.get("id"));
        boolean existed = StringUtils.hasText(stem) && questionBankMapper.selectCount(new LambdaQueryWrapper<EduExamQuestionBank>()
                .eq(EduExamQuestionBank::getStem, stem.trim()).ne(id != null, EduExamQuestionBank::getId, id)) > 0;
        return new LinkedHashMap<>(Map.of("existed", existed, "name", defaultText(stem, "")));
    }

    @Override
    public Object questionOrQuestionBank(Long id) {
        EduExamQuestionBank bank = id == null ? null : questionBankMapper.selectById(id);
        return bank == null ? interactionService.question(id) : legacyQuestionView(bank);
    }

    @Override
    @Transactional
    public void removeQuestionPayload(Long id) {
        EduExamQuestionBank bank = id == null ? null : questionBankMapper.selectById(id);
        if (bank != null) questionBankMapper.deleteById(id);
        else interactionService.removeQuestion(id);
    }

    @Override
    public List<Map<String, Object>> legacyBizQuestions(Long bizId) {
        if (bizId == null) return List.of();
        return catalogQuestionMapper.selectList(new LambdaQueryWrapper<EduCatalogQuestion>()
                .eq(EduCatalogQuestion::getCatalogId, bizId).orderByAsc(EduCatalogQuestion::getSortNum))
                .stream().map(item -> questionBankMapper.selectById(item.getQuestionId()))
                .filter(Objects::nonNull).map(this::legacyQuestionView).toList();
    }

    @Override
    public Map<String, Object> examsPage(Map<String, ?> params) {
        long pageNo = number(params, "pageNo", 1);
        long pageSize = number(params, "pageSize", 10);
        Page<EduExamRecord> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        examRecordMapper.selectPage(page, new LambdaQueryWrapper<EduExamRecord>()
                .eq(EduExamRecord::getUserId, currentUserId()).orderByDesc(EduExamRecord::getCreateTime));
        return pageView(page.getTotal(), page.getRecords().stream().map(this::examRecordView).toList());
    }

    @Override
    public Map<String, Object> examRecordDetails(Long recordId) {
        EduExamRecord record = recordId == null ? null : examRecordMapper.selectById(recordId);
        require(record != null, "考试记录不存在");
        Long userId = currentUserId();
        require(Objects.equals(record.getUserId(), userId) || SecurityUtils.isAdmin(userId), "无权查看该考试记录");

        EduExam exam = examMapper.selectById(record.getExamId());
        Map<String, Object> result = new LinkedHashMap<>(examRecordView(record));
        result.put("courseId", exam == null ? null : exam.getCourseId());
        result.put("courseName", exam == null ? "" : courseService.courseName(exam.getCourseId()));
        result.put("sectionName", exam == null ? "" : exam.getExamName());
        result.put("commitTime", record.getSubmittedAt());
        result.put("duration", durationSeconds(record.getStartedAt(), record.getSubmittedAt()));

        List<EduExamAnswer> answers = examAnswerMapper.selectList(new LambdaQueryWrapper<EduExamAnswer>()
                .eq(EduExamAnswer::getRecordId, record.getId()).orderByAsc(EduExamAnswer::getCreateTime));
        Map<Long, EduExamAnswer> answerByQuestion = answers.stream()
                .collect(Collectors.toMap(EduExamAnswer::getQuestionId, Function.identity(), (left, right) -> right,
                        LinkedHashMap::new));
        List<Long> questionIds = exam == null ? List.of() : examQuestionMapper.selectList(new LambdaQueryWrapper<EduExamQuestion>()
                .eq(EduExamQuestion::getExamId, exam.getId()).orderByAsc(EduExamQuestion::getSortNum))
                .stream().map(EduExamQuestion::getQuestionId).toList();
        if (questionIds.isEmpty()) {
            questionIds = answers.stream().map(EduExamAnswer::getQuestionId).filter(Objects::nonNull).toList();
        }
        if (questionIds.isEmpty()) {
            questionIds = questionBankMapper.selectList(new LambdaQueryWrapper<EduExamQuestionBank>()
                    .eq(EduExamQuestionBank::getStatus, ENABLED).orderByAsc(EduExamQuestionBank::getId))
                    .stream().map(EduExamQuestionBank::getId).toList();
        }

        List<Map<String, Object>> details = new ArrayList<>();
        for (Long questionId : questionIds) {
            EduExamQuestionBank question = questionBankMapper.selectById(questionId);
            if (question == null) continue;
            EduExamAnswer answer = answerByQuestion.get(questionId);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("question", legacyQuestionView(question));
            item.put("answer", answer == null ? "" : answer.getUserAnswer());
            item.put("correct", answer != null && Integer.valueOf(1).equals(answer.getIsCorrect()));
            item.put("score", answer == null ? BigDecimal.ZERO : defaultValue(answer.getScore(), BigDecimal.ZERO));
            details.add(item);
        }
        result.put("details", details);
        result.put("list", details);
        result.put("answers", answers);
        return result;
    }

    @Override
    public Map<String, Object> exam(Long id) {
        EduExam exam = examMapper.selectById(id);
        if (exam == null) {
            EduExamRecord record = examRecordMapper.selectById(id);
            if (record != null) exam = examMapper.selectById(record.getExamId());
        }
        if (exam == null) throw new ServiceException("考试不存在");
        Map<String, Object> result = examView(exam);
        List<EduExamQuestion> relations = examQuestionMapper.selectList(new LambdaQueryWrapper<EduExamQuestion>()
                .eq(EduExamQuestion::getExamId, exam.getId()).orderByAsc(EduExamQuestion::getSortNum));
        List<Long> questionIds = relations.stream().map(EduExamQuestion::getQuestionId).toList();
        List<EduExamQuestionBank> questions = questionIds.isEmpty()
                ? questionBankMapper.selectList(new LambdaQueryWrapper<EduExamQuestionBank>()
                .eq(EduExamQuestionBank::getStatus, ENABLED).orderByAsc(EduExamQuestionBank::getId).last("limit 20"))
                : questionIds.stream().map(questionBankMapper::selectById).filter(Objects::nonNull).toList();
        result.put("questions", questions.stream().map(this::legacyQuestionView).toList());
        result.put("questionBankQuestions", questions.stream().map(this::questionBankView).toList());
        result.put("questionCount", questions.size());
        return result;
    }

    @Override
    public Map<String, Object> examQuestions(Map<String, ?> params) {
        Map<String, ?> request = params == null ? Map.of() : params;
        Long examId = longValue(request.get("examId"));
        if (examId == null) examId = longValue(request.get("id"));
        if (examId != null) return exam(examId);
        Long courseId = longValue(request.get("courseId"));
        EduExam value = examMapper.selectOne(new LambdaQueryWrapper<EduExam>()
                .eq(courseId != null, EduExam::getCourseId, courseId)
                .eq(EduExam::getStatus, ENABLED).orderByAsc(EduExam::getId).last("limit 1"));
        if (value != null) return exam(value.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("questions", questionBankMapper.selectList(new LambdaQueryWrapper<EduExamQuestionBank>()
                .eq(EduExamQuestionBank::getStatus, ENABLED).orderByAsc(EduExamQuestionBank::getId).last("limit 20")).stream()
                .map(this::legacyQuestionView).toList());
        return result;
    }

    @Override
    @Transactional
    public EduExamRecord startExam(EduExamRecord value) {
        require(value != null && value.getExamId() != null, "考试编号不能为空");
        EduExam exam = examMapper.selectById(value.getExamId());
        require(exam != null, "考试不存在");
        value.setId(newId());
        value.setUserId(currentUserId());
        value.setTotalScore(defaultValue(exam.getTotalScore(), BigDecimal.valueOf(100)));
        value.setQuestionCount(examQuestionCount(exam.getId()));
        value.setCorrectCount(0);
        value.setStatus(0);
        value.setStartedAt(LocalDateTime.now());
        value.setCreateTime(LocalDateTime.now());
        value.setUpdateTime(LocalDateTime.now());
        value.setDelFlag(0);
        value.setVersion(0);
        examRecordMapper.insert(value);
        return value;
    }

    @Override
    @Transactional
    public Map<String, Object> submitExam(Map<String, ?> payload) {
        Map<String, ?> request = payload == null ? Map.of() : payload;
        Long recordId = longValue(request.get("recordId"));
        EduExamRecord record = recordId == null ? null : examRecordMapper.selectById(recordId);
        if (record != null) {
            Long userId = currentUserId();
            require(Objects.equals(record.getUserId(), userId) || SecurityUtils.isAdmin(userId), "无权提交该考试记录");
        }
        if (record == null) {
            Long examId = longValue(request.get("examId"));
            if (examId == null) examId = longValue(request.get("id"));
            require(examId != null, "考试编号不能为空");
            EduExamRecord newRecord = new EduExamRecord();
            newRecord.setExamId(examId);
            record = startExam(newRecord);
        }
        EduExam exam = examMapper.selectById(record.getExamId());
        require(exam != null, "考试不存在");

        List<EduExamQuestion> relations = examQuestionMapper.selectList(new LambdaQueryWrapper<EduExamQuestion>()
                .eq(EduExamQuestion::getExamId, exam.getId()).orderByAsc(EduExamQuestion::getSortNum));
        Map<Long, EduExamQuestion> relationByQuestion = relations.stream()
                .collect(Collectors.toMap(EduExamQuestion::getQuestionId, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));
        boolean hasExplicitQuestions = !relationByQuestion.isEmpty();
        List<EduExamQuestionBank> examQuestions = hasExplicitQuestions
                ? relationByQuestion.keySet().stream().map(questionBankMapper::selectById).filter(Objects::nonNull).toList()
                : questionBankMapper.selectList(new LambdaQueryWrapper<EduExamQuestionBank>()
                .eq(EduExamQuestionBank::getStatus, ENABLED).orderByAsc(EduExamQuestionBank::getId).last("limit 20"));

        if (!Integer.valueOf(0).equals(record.getStatus())) {
            Map<String, Object> result = examRecordView(record);
            result.put("score", record.getScore());
            result.put("correctCount", record.getCorrectCount());
            result.put("passScore", defaultValue(exam.getPassScore(), BigDecimal.valueOf(60)));
            result.put("questions", examQuestions.stream().map(this::legacyQuestionView).toList());
            result.putAll(examRecordDetails(record.getId()));
            return result;
        }

        Map<Long, EduExamAnswer> existingAnswers = examAnswerMapper.selectList(new LambdaQueryWrapper<EduExamAnswer>()
                .eq(EduExamAnswer::getRecordId, record.getId())).stream()
                .collect(Collectors.toMap(EduExamAnswer::getQuestionId, Function.identity(), (left, right) -> right,
                        LinkedHashMap::new));

        Object answersObject = request.get("answers");
        if (!(answersObject instanceof List<?>)) answersObject = request.get("examDetails");
        int correct = 0;
        BigDecimal score = BigDecimal.ZERO;
        Set<Long> processedQuestions = new HashSet<>();
        if (answersObject instanceof List<?> answers) {
            for (Object item : answers) {
                if (!(item instanceof Map<?, ?> answer)) continue;
                Long questionId = longValue(answer.get("questionId"));
                if (questionId == null) questionId = longValue(answer.get("id"));
                require(questionId != null, "题目编号不能为空");
                require(processedQuestions.add(questionId), "同一题目不能重复提交");
                EduExamQuestionBank question = questionId == null ? null : questionBankMapper.selectById(questionId);
                EduExamQuestion relation = relationByQuestion.get(questionId);
                require(question != null && (!hasExplicitQuestions || relation != null), "提交的题目不属于该考试");
                Object rawAns = answer.get("answer") != null ? answer.get("answer") : answer.get("userAnswer");
                String userAnswer = gradingFactory.normalizeAnswer(text(rawAns), question.getQuestionType());
                boolean right = question != null && gradingFactory.isCorrect(question.getCorrectAnswer(), userAnswer, question.getQuestionType());
                BigDecimal itemScore = relation != null && relation.getScore() != null
                        && relation.getScore().compareTo(BigDecimal.ZERO) > 0
                        ? relation.getScore() : defaultValue(question.getScore(), BigDecimal.ZERO);
                if (right) {
                    correct++;
                    score = score.add(itemScore);
                }
                EduExamAnswer entity = existingAnswers.get(questionId);
                if (entity == null) {
                    entity = new EduExamAnswer();
                    entity.setId(newId());
                    entity.setRecordId(record.getId());
                    entity.setQuestionId(questionId);
                }
                entity.setUserAnswer(userAnswer);
                entity.setIsCorrect(right ? 1 : 0);
                entity.setScore(right ? itemScore : BigDecimal.ZERO);
                if (entity.getCreateTime() == null) entity.setCreateTime(LocalDateTime.now());
                if (existingAnswers.containsKey(questionId)) examAnswerMapper.updateById(entity);
                else {
                    examAnswerMapper.insert(entity);
                    existingAnswers.put(questionId, entity);
                }
            }
        }

        for (EduExamQuestionBank question : examQuestions) {
            if (question != null && question.getId() != null && !processedQuestions.contains(question.getId())) {
                EduExamAnswer unattempted = existingAnswers.get(question.getId());
                if (unattempted == null) {
                    unattempted = new EduExamAnswer();
                    unattempted.setId(newId());
                    unattempted.setRecordId(record.getId());
                    unattempted.setQuestionId(question.getId());
                    unattempted.setUserAnswer("");
                    unattempted.setIsCorrect(0);
                    unattempted.setScore(BigDecimal.ZERO);
                    unattempted.setCreateTime(LocalDateTime.now());
                    examAnswerMapper.insert(unattempted);
                    existingAnswers.put(question.getId(), unattempted);
                }
            }
        }
        record.setScore(score);
        record.setCorrectCount(correct);
        BigDecimal passScore = defaultValue(exam.getPassScore(), BigDecimal.valueOf(60));
        record.setStatus(score.compareTo(passScore) >= 0 ? 1 : 2);
        record.setSubmittedAt(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        examRecordMapper.updateById(record);
        Map<String, Object> result = examRecordView(record);
        result.put("score", score);
        result.put("correctCount", correct);
        result.put("passScore", passScore);
        result.put("questions", examQuestions.stream().map(this::legacyQuestionView).toList());
        return result;
    }

    @Override
    public IPage<EduExam> pageExams(String keyword, Integer status, long pageNo, long pageSize) {
        Page<EduExam> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        return examMapper.selectPage(page, new LambdaQueryWrapper<EduExam>()
                .like(StringUtils.hasText(keyword), EduExam::getExamName, keyword)
                .eq(status != null, EduExam::getStatus, status).orderByDesc(EduExam::getCreateTime));
    }

    @Override
    public IPage<EduExamQuestionBank> pageQuestionBank(String keyword, String questionType, Integer status,
                                                      long pageNo, long pageSize) {
        return pageQuestionBank(keyword, questionType, status, null, pageNo, pageSize);
    }

    @Override
    public IPage<EduExamQuestionBank> pageQuestionBank(String keyword, String questionType, Integer status,
                                                      Long categoryId, long pageNo, long pageSize) {
        Page<EduExamQuestionBank> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        return questionBankMapper.selectPage(page, new LambdaQueryWrapper<EduExamQuestionBank>()
                .like(StringUtils.hasText(keyword), EduExamQuestionBank::getStem, keyword)
                .eq(StringUtils.hasText(questionType), EduExamQuestionBank::getQuestionType, questionType)
                .eq(categoryId != null, EduExamQuestionBank::getCategoryId, categoryId)
                .eq(status != null, EduExamQuestionBank::getStatus, status)
                .orderByDesc(EduExamQuestionBank::getCreateTime));
    }

    @Override
    public EduExam saveExam(EduExam value) {
        require(value != null && StringUtils.hasText(value.getExamName()), "考试名称不能为空");
        LocalDateTime now = LocalDateTime.now();
        if (value.getId() == null) {
            value.setId(newId());
            value.setTotalScore(defaultValue(value.getTotalScore(), BigDecimal.valueOf(100)));
            value.setPassScore(defaultValue(value.getPassScore(), BigDecimal.valueOf(60)));
            value.setDurationMinutes(defaultValue(value.getDurationMinutes(), 60));
            value.setStatus(defaultValue(value.getStatus(), ENABLED));
            value.setCreateTime(now);
            value.setUpdateTime(now);
            value.setDelFlag(0);
            value.setVersion(0);
            examMapper.insert(value);
        } else {
            value.setUpdateTime(now);
            examMapper.updateById(value);
        }
        return value;
    }

    @Override
    public EduExamQuestionBank saveQuestionBank(EduExamQuestionBank value) {
        require(value != null && StringUtils.hasText(value.getStem()), "题干不能为空");
        LocalDateTime now = LocalDateTime.now();
        if (value.getId() == null) {
            value.setId(newId());
            value.setQuestionType(defaultText(value.getQuestionType(), "single"));
            value.setScore(defaultValue(value.getScore(), BigDecimal.ONE));
            value.setDifficulty(defaultValue(value.getDifficulty(), 2));
            value.setStatus(defaultValue(value.getStatus(), ENABLED));
            value.setCreateTime(now);
            value.setUpdateTime(now);
            value.setDelFlag(0);
            value.setVersion(0);
            questionBankMapper.insert(value);
        } else {
            value.setUpdateTime(now);
            questionBankMapper.updateById(value);
        }
        return value;
    }

    @Override
    public Map<String, Object> examView(EduExam item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("courseId", item.getCourseId());
        result.put("courseName", courseService.courseName(item.getCourseId()));
        result.put("examName", item.getExamName());
        result.put("name", item.getExamName());
        result.put("sectionName", item.getExamName());
        result.put("description", item.getDescription());
        result.put("totalScore", item.getTotalScore());
        result.put("passScore", item.getPassScore());
        result.put("durationMinutes", item.getDurationMinutes());
        result.put("duration", item.getDurationMinutes() == null ? 0 : item.getDurationMinutes() * 60);
        result.put("status", item.getStatus());
        return result;
    }

    @Override
    public Map<String, Object> examRecordView(EduExamRecord item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("examId", item.getExamId());
        result.put("score", item.getScore());
        result.put("totalScore", item.getTotalScore());
        result.put("correctCount", item.getCorrectCount());
        result.put("questionCount", item.getQuestionCount());
        result.put("status", item.getStatus());
        result.put("statusName", Integer.valueOf(1).equals(item.getStatus()) ? "通过" : Integer.valueOf(2).equals(item.getStatus()) ? "未通过" : "进行中");
        result.put("startedAt", item.getStartedAt());
        result.put("startTime", item.getStartedAt());
        result.put("submittedAt", item.getSubmittedAt());
        result.put("endTime", item.getSubmittedAt());
        result.put("commitTime", item.getSubmittedAt());
        result.put("duration", durationSeconds(item.getStartedAt(), item.getSubmittedAt()));
        EduExam exam = examMapper.selectById(item.getExamId());
        if (exam != null) {
            result.put("examName", exam.getExamName());
            result.put("sectionName", exam.getExamName());
            result.put("courseId", exam.getCourseId());
            result.put("courseName", courseService.courseName(exam.getCourseId()));
        }
        return result;
    }

    @Override
    public Map<String, Object> questionBankView(EduExamQuestionBank item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("questionType", item.getQuestionType());
        result.put("type", item.getQuestionType());
        result.put("stem", item.getStem());
        result.put("title", item.getStem());
        result.put("options", item.getOptionsJson());
        result.put("optionsJson", item.getOptionsJson());
        result.put("correctAnswer", item.getCorrectAnswer());
        result.put("analysis", item.getAnalysis());
        result.put("score", item.getScore());
        result.put("difficulty", item.getDifficulty());
        result.put("categoryId", item.getCategoryId());
        return result;
    }

    @Override
    public Map<String, Object> legacyQuestionView(EduExamQuestionBank item) {
        Map<String, Object> result = questionBankView(item);
        result.put("questionType", item.getQuestionType());
        result.put("type", legacyQuestionType(item.getQuestionType()));
        result.put("title", item.getStem());
        result.put("name", item.getStem());
        result.put("answer", item.getCorrectAnswer());
        result.put("categoryName", "题库题目");
        result.put("createTime", item.getCreateTime());
        List<?> options = List.of();
        if (StringUtils.hasText(item.getOptionsJson())) {
            try {
                options = objectMapper.readValue(item.getOptionsJson(), List.class);
            } catch (Exception ignored) {
                options = List.of(item.getOptionsJson());
            }
        }
        result.put("options", options);
        return result;
    }

    @Override
    public int examQuestionCount(Long examId) {
        int count = examQuestionMapper.selectCount(new LambdaQueryWrapper<EduExamQuestion>().eq(EduExamQuestion::getExamId, examId)).intValue();
        return count > 0 ? count : Math.min(20, questionBankMapper.selectCount(new LambdaQueryWrapper<EduExamQuestionBank>().eq(EduExamQuestionBank::getStatus, ENABLED)).intValue());
    }
}
