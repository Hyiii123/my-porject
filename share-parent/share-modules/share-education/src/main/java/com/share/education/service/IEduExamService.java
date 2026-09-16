package com.share.education.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.share.education.domain.EduExam;
import com.share.education.domain.EduExamQuestionBank;
import com.share.education.domain.EduExamRecord;

import java.util.List;
import java.util.Map;

/**
 * 考试测评、题库管理与答卷批改领域接口
 */
public interface IEduExamService {

    long generateStageExamId();

    boolean isQuestionBankPayload(Map<String, ?> payload);

    Object saveQuestionPayload(Map<String, ?> payload);

    Map<String, Object> checkQuestionName(Map<String, ?> params);

    Object questionOrQuestionBank(Long id);

    void removeQuestionPayload(Long id);

    List<Map<String, Object>> legacyBizQuestions(Long bizId);

    Map<String, Object> examsPage(Map<String, ?> params);

    Map<String, Object> examRecordDetails(Long recordId);

    Map<String, Object> exam(Long id);

    Map<String, Object> examQuestions(Map<String, ?> params);

    EduExamRecord startExam(EduExamRecord value);

    Map<String, Object> submitExam(Map<String, ?> payload);

    IPage<EduExam> pageExams(String keyword, Integer status, long pageNo, long pageSize);

    IPage<EduExamQuestionBank> pageQuestionBank(String keyword, String questionType, Integer status,
                                               long pageNo, long pageSize);

    IPage<EduExamQuestionBank> pageQuestionBank(String keyword, String questionType, Integer status,
                                               Long categoryId, long pageNo, long pageSize);

    EduExam saveExam(EduExam value);

    EduExamQuestionBank saveQuestionBank(EduExamQuestionBank value);

    Map<String, Object> examView(EduExam item);

    Map<String, Object> examRecordView(EduExamRecord item);

    Map<String, Object> questionBankView(EduExamQuestionBank item);

    Map<String, Object> legacyQuestionView(EduExamQuestionBank item);

    int examQuestionCount(Long examId);
}
