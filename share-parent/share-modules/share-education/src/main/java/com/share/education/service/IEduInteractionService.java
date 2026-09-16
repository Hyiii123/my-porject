package com.share.education.service;

import com.share.education.domain.EduNote;
import com.share.education.domain.EduQuestion;
import com.share.education.domain.EduReply;

import java.util.Map;

/**
 * 问答社区、笔记与互动点赞领域接口
 */
public interface IEduInteractionService {

    Map<String, Object> questionPage(Map<String, ?> params);

    Map<String, Object> legacyQuestionPage(Map<String, ?> params);

    Map<String, Object> question(Long id);

    EduQuestion saveQuestion(EduQuestion value);

    void removeQuestion(Long id);

    Map<String, Object> reply(Long id);

    void setQuestionHidden(Long id, boolean hidden);

    void setReplyHidden(Long id, boolean hidden);

    Map<String, Object> replyPage(Map<String, ?> params);

    EduReply saveReply(EduReply value);

    Map<String, Object> notePage(Map<String, ?> params);

    EduNote saveNote(EduNote value);

    void removeNote(Long id);

    Map<String, Object> note(Long id);

    void setNoteHidden(Long id, boolean hidden);

    void setNoteVisibility(Long id, boolean visible);

    boolean collectNote(Long noteId, boolean collect);

    boolean like(String bizType, Long bizId, boolean liked);

    Map<String, Object> questionView(EduQuestion item);

    Map<String, Object> replyView(EduReply item);

    Map<String, Object> noteView(EduNote item);

    EduQuestion requireQuestion(Long id);

    EduNote requireNote(Long id);
}
