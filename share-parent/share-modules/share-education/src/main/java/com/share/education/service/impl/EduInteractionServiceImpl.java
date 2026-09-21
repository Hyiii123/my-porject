package com.share.education.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.share.common.core.exception.ServiceException;
import com.share.common.redis.service.RedisService;
import com.share.common.security.utils.SecurityUtils;
import com.share.education.domain.*;
import com.share.education.mapper.*;
import com.share.education.service.IEduCourseService;
import com.share.education.service.IEduExamService;
import com.share.education.service.IEduInteractionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

import static com.share.education.service.support.EduUtils.*;

/**
 * 问答社区、笔记与互动点赞领域服务实现
 */
@Slf4j
@Service
@Primary
public class EduInteractionServiceImpl implements IEduInteractionService {

    private static final int ENABLED = 1;

    private final EduQuestionMapper questionMapper;
    private final EduReplyMapper replyMapper;
    private final EduQuestionLikeMapper questionLikeMapper;
    private final EduNoteMapper noteMapper;
    private final EduNoteCollectMapper noteCollectMapper;
    private final EduNoteLikeMapper noteLikeMapper;
    private final EduExamQuestionBankMapper questionBankMapper;
    private final RedisService redisService;
    private final IEduCourseService courseService;
    private final IEduExamService examService;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    public EduInteractionServiceImpl(EduQuestionMapper questionMapper,
                                    EduReplyMapper replyMapper,
                                    EduQuestionLikeMapper questionLikeMapper,
                                    EduNoteMapper noteMapper,
                                    EduNoteCollectMapper noteCollectMapper,
                                    EduNoteLikeMapper noteLikeMapper,
                                    EduExamQuestionBankMapper questionBankMapper,
                                    RedisService redisService,
                                    IEduCourseService courseService,
                                    @Lazy IEduExamService examService) {
        this.questionMapper = questionMapper;
        this.replyMapper = replyMapper;
        this.questionLikeMapper = questionLikeMapper;
        this.noteMapper = noteMapper;
        this.noteCollectMapper = noteCollectMapper;
        this.noteLikeMapper = noteLikeMapper;
        this.questionBankMapper = questionBankMapper;
        this.redisService = redisService;
        this.courseService = courseService;
        this.examService = examService;
    }

    @Override
    public Map<String, Object> questionPage(Map<String, ?> params) {
        if (isQuestionBankQuery(params)) return legacyQuestionPage(params);
        long pageNo = number(params, "pageNo", 1);
        long pageSize = number(params, "pageSize", 10);
        Page<EduQuestion> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        Long courseId = longValue(params.get("courseId"));
        Long sectionId = longValue(params.get("sectionId"));
        boolean onlyMine = bool(params.get("onlyMine"));
        String keyword = defaultText(params.get("keyword"), defaultText(params.get("title"), null));
        String solved = defaultText(params.get("solved"), defaultText(params.get("status"), null));
        LambdaQueryWrapper<EduQuestion> wrapper = new LambdaQueryWrapper<EduQuestion>()
                .eq(EduQuestion::getHidden, 0).eq(EduQuestion::getStatus, ENABLED)
                .eq(courseId != null && courseId > 0, EduQuestion::getCourseId, courseId)
                .eq(sectionId != null && sectionId > 0, EduQuestion::getCategory, String.valueOf(sectionId))
                .eq(onlyMine, EduQuestion::getUserId, currentUserId())
                .and(StringUtils.hasText(keyword), item -> item.like(EduQuestion::getTitle, keyword)
                        .or().like(EduQuestion::getContent, keyword))
                .ge("solved".equalsIgnoreCase(solved), EduQuestion::getReplyCount, 1)
                .eq("unsolved".equalsIgnoreCase(solved), EduQuestion::getReplyCount, 0)
                .orderByDesc(EduQuestion::getCreateTime);
        questionMapper.selectPage(page, wrapper);
        return pageView(page.getTotal(), page.getRecords().stream().map(this::questionView).toList());
    }

    @Override
    public Map<String, Object> legacyQuestionPage(Map<String, ?> params) {
        long pageNo = number(params, "pageNo", number(params, "pageNum", 1));
        long pageSize = number(params, "pageSize", 10);
        String type = defaultText(params == null ? null : params.get("type"),
                defaultText(params == null ? null : params.get("questionType"), null));
        String keyword = defaultText(params == null ? null : params.get("keyword"),
                defaultText(params == null ? null : params.get("title"), null));
        Page<EduExamQuestionBank> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        Long categoryId = longValue(params == null ? null : params.get("categoryId"));
        questionBankMapper.selectPage(page, new LambdaQueryWrapper<EduExamQuestionBank>()
                .like(StringUtils.hasText(keyword), EduExamQuestionBank::getStem, keyword)
                .eq(StringUtils.hasText(type), EduExamQuestionBank::getQuestionType, normalizeQuestionType(type))
                .eq(categoryId != null && categoryId > 0, EduExamQuestionBank::getCategoryId, categoryId)
                .eq(EduExamQuestionBank::getStatus, ENABLED)
                .orderByDesc(EduExamQuestionBank::getCreateTime));
        return pageView(page.getTotal(), page.getRecords().stream().map(examService::legacyQuestionView).toList());
    }

    @Override
    public Map<String, Object> question(Long id) {
        EduQuestion value = requireQuestion(id);
        value.setViewCount(defaultValue(value.getViewCount(), 0) + 1);
        questionMapper.updateById(value);
        return questionView(value);
    }

    @Override
    @Transactional
    public EduQuestion saveQuestion(EduQuestion value) {
        require(value != null && StringUtils.hasText(value.getTitle()), "问题标题不能为空");
        require(StringUtils.hasText(value.getContent()), "问题内容不能为空");
        LocalDateTime now = LocalDateTime.now();
        if (value.getId() == null) {
            value.setId(newId());
            value.setUserId(requireCurrentUserId());
            value.setViewCount(0);
            value.setReplyCount(0);
            value.setLikeCount(0);
            value.setHidden(0);
            value.setStatus(ENABLED);
            value.setCreateTime(now);
            value.setUpdateTime(now);
            value.setDelFlag(0);
            value.setVersion(0);
            questionMapper.insert(value);
        } else {
            EduQuestion old = requireQuestion(value.getId());
            Long uid = requireCurrentUserId();
            if (!Objects.equals(old.getUserId(), uid) && !SecurityUtils.isAdmin(uid)) {
                throw new ServiceException("只能编辑自己的问题");
            }
            value.setUserId(old.getUserId());
            value.setUpdateTime(now);
            questionMapper.updateById(value);
        }
        return value;
    }

    @Override
    @Transactional
    public void removeQuestion(Long id) {
        EduQuestion value = requireQuestion(id);
        Long uid = requireCurrentUserId();
        if (!Objects.equals(value.getUserId(), uid) && !SecurityUtils.isAdmin(uid)) {
            throw new ServiceException("只能删除自己的问题");
        }
        questionMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> reply(Long id) {
        EduReply value = id == null ? null : replyMapper.selectById(id);
        if (value == null) throw new ServiceException("回复不存在");
        return replyView(value);
    }

    @Override
    @Transactional
    public void setQuestionHidden(Long id, boolean hidden) {
        EduQuestion value = requireQuestion(id);
        value.setHidden(hidden ? 1 : 0);
        value.setUpdateTime(LocalDateTime.now());
        questionMapper.updateById(value);
    }

    @Override
    @Transactional
    public void setReplyHidden(Long id, boolean hidden) {
        EduReply value = id == null ? null : replyMapper.selectById(id);
        if (value == null) throw new ServiceException("回复不存在");
        value.setHidden(hidden ? 1 : 0);
        value.setUpdateTime(LocalDateTime.now());
        replyMapper.updateById(value);
    }

    @Override
    public Map<String, Object> replyPage(Map<String, ?> params) {
        long pageNo = number(params, "pageNo", 1);
        long pageSize = number(params, "pageSize", 10);
        Long questionId = longValue(params.get("questionId"));
        Long answerId = longValue(params.get("answerId"));
        Page<EduReply> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        LambdaQueryWrapper<EduReply> wrapper = new LambdaQueryWrapper<EduReply>()
                .eq(EduReply::getHidden, 0).eq(EduReply::getStatus, ENABLED)
                .eq(questionId != null, EduReply::getQuestionId, questionId);
        if (answerId != null && answerId > 0) {
            wrapper.eq(EduReply::getParentId, answerId);
        } else {
            wrapper.and(w -> w.eq(EduReply::getParentId, 0).or().isNull(EduReply::getParentId));
        }
        wrapper.orderByAsc(EduReply::getCreateTime);
        replyMapper.selectPage(page, wrapper);
        return pageView(page.getTotal(), page.getRecords().stream().map(this::replyView).toList());
    }

    @Override
    @Transactional
    public EduReply saveReply(EduReply value) {
        require(value != null && value.getQuestionId() != null, "问题编号不能为空");
        require(StringUtils.hasText(value.getContent()), "回复内容不能为空");
        requireQuestion(value.getQuestionId());
        LocalDateTime now = LocalDateTime.now();
        value.setId(newId());
        value.setUserId(requireCurrentUserId());
        value.setLikeCount(0);
        value.setHidden(0);
        value.setStatus(ENABLED);
        value.setCreateTime(now);
        value.setUpdateTime(now);
        value.setDelFlag(0);
        value.setVersion(0);

        if (value.getParentId() == null || value.getParentId() <= 0) {
            if (value.getAnswerId() != null && value.getAnswerId() > 0) {
                value.setParentId(value.getAnswerId());
            } else if (value.getTargetReplyId() != null && value.getTargetReplyId() > 0) {
                value.setParentId(value.getTargetReplyId());
            } else {
                value.setParentId(0L);
            }
        }

        replyMapper.insert(value);
        EduQuestion question = requireQuestion(value.getQuestionId());
        question.setReplyCount(defaultValue(question.getReplyCount(), 0) + 1);
        question.setUpdateTime(now);
        questionMapper.updateById(question);
        return value;
    }

    @Override
    public Map<String, Object> notePage(Map<String, ?> params) {
        long pageNo = number(params, "pageNo", 1);
        long pageSize = number(params, "pageSize", 10);
        Long courseId = longValue(params.get("courseId"));
        Long catalogId = longValue(params.get("sectionId"));
        if (catalogId == null) catalogId = longValue(params.get("catalogId"));
        boolean onlyMine = bool(params.get("onlyMine"));
        Page<EduNote> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        Long userId = currentUserId();
        boolean admin = SecurityUtils.isAdmin(userId);
        noteMapper.selectPage(page, new LambdaQueryWrapper<EduNote>()
                .eq(courseId != null && courseId > 0, EduNote::getCourseId, courseId)
                .eq(catalogId != null && catalogId > 0, EduNote::getCatalogId, catalogId)
                .eq(onlyMine, EduNote::getUserId, userId)
                .and(!onlyMine && !admin, item -> item.eq(EduNote::getVisibility, 1).or().eq(EduNote::getUserId, userId))
                .eq(EduNote::getHidden, 0).eq(EduNote::getStatus, ENABLED)
                .orderByDesc(EduNote::getCreateTime));
        return pageView(page.getTotal(), page.getRecords().stream().map(this::noteView).toList());
    }

    @Override
    @Transactional
    public EduNote saveNote(EduNote value) {
        require(value != null && StringUtils.hasText(value.getContent()), "笔记内容不能为空");
        LocalDateTime now = LocalDateTime.now();
        if (value.getId() == null) {
            value.setId(newId());
            value.setUserId(requireCurrentUserId());
            value.setTitle(StringUtils.hasText(value.getTitle()) ? value.getTitle() : "学习笔记");
            value.setVisibility(defaultValue(value.getVisibility(), 1));
            value.setLikeCount(0);
            value.setCollectCount(0);
            value.setHidden(0);
            value.setStatus(ENABLED);
            value.setCreateTime(now);
            value.setUpdateTime(now);
            value.setDelFlag(0);
            value.setVersion(0);
            noteMapper.insert(value);
        } else {
            EduNote old = requireNote(value.getId());
            Long uid = requireCurrentUserId();
            if (!Objects.equals(old.getUserId(), uid) && !SecurityUtils.isAdmin(uid)) {
                throw new ServiceException("只能编辑自己的笔记");
            }
            value.setUserId(old.getUserId());
            value.setUpdateTime(now);
            noteMapper.updateById(value);
        }
        return value;
    }

    @Override
    @Transactional
    public void removeNote(Long id) {
        EduNote value = requireNote(id);
        Long uid = requireCurrentUserId();
        if (!Objects.equals(value.getUserId(), uid) && !SecurityUtils.isAdmin(uid)) {
            throw new ServiceException("只能删除自己的笔记");
        }
        noteMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> note(Long id) {
        return noteView(requireNote(id));
    }

    @Override
    @Transactional
    public void setNoteHidden(Long id, boolean hidden) {
        EduNote value = requireNote(id);
        value.setHidden(hidden ? 1 : 0);
        value.setUpdateTime(LocalDateTime.now());
        noteMapper.updateById(value);
    }

    @Override
    @Transactional
    public void setNoteVisibility(Long id, boolean visible) {
        EduNote value = requireNote(id);
        value.setVisibility(visible ? 1 : 0);
        value.setUpdateTime(LocalDateTime.now());
        noteMapper.updateById(value);
    }

    @Override
    @Transactional
    public boolean collectNote(Long noteId, boolean collect) {
        EduNote note = requireNote(noteId);
        Long userId = requireCurrentUserId();
        EduNoteCollect old = noteCollectMapper.selectOne(new LambdaQueryWrapper<EduNoteCollect>()
                .eq(EduNoteCollect::getNoteId, noteId).eq(EduNoteCollect::getUserId, userId));
        if (collect && old == null) {
            old = new EduNoteCollect();
            old.setId(newId());
            old.setNoteId(noteId);
            old.setUserId(userId);
            old.setCreateTime(LocalDateTime.now());
            noteCollectMapper.insert(old);
            note.setCollectCount(defaultValue(note.getCollectCount(), 0) + 1);
        } else if (!collect && old != null) {
            noteCollectMapper.deleteById(old.getId());
            note.setCollectCount(Math.max(defaultValue(note.getCollectCount(), 0) - 1, 0));
        }
        noteMapper.updateById(note);
        return collect;
    }

    @Override
    @Transactional
    public boolean like(String bizType, Long bizId, boolean liked) {
        if ("COURSE".equalsIgnoreCase(bizType)) {
            courseService.requireCourse(bizId);
            Long userId = requireCurrentUserId();
            String userSetKey = "edu:course:likes:users:" + bizId;
            String rankingZSetKey = "edu:course:likes:zset";

            if (liked) {
                if (Boolean.TRUE.equals(redisService.sIsMember(userSetKey, String.valueOf(userId)))) {
                    return true;
                }
                redisService.sAdd(userSetKey, String.valueOf(userId));
                redisService.zIncrementScore(rankingZSetKey, String.valueOf(bizId), 1.0);
            } else {
                if (Boolean.FALSE.equals(redisService.sIsMember(userSetKey, String.valueOf(userId)))) {
                    return false;
                }
                redisService.sRemove(userSetKey, String.valueOf(userId));
                Double score = redisService.zIncrementScore(rankingZSetKey, String.valueOf(bizId), -1.0);
                if (score != null && score < 0) {
                    redisService.zAdd(rankingZSetKey, String.valueOf(bizId), 0.0);
                }
            }
            return liked;
        }
        if ("NOTE".equalsIgnoreCase(bizType)) {
            EduNote note = requireNote(bizId);
            EduNoteLike old = noteLikeMapper.selectOne(new LambdaQueryWrapper<EduNoteLike>()
                    .eq(EduNoteLike::getNoteId, bizId).eq(EduNoteLike::getUserId, currentUserId()));
            if (liked && old == null) {
                old = new EduNoteLike();
                old.setId(newId());
                old.setNoteId(bizId);
                old.setUserId(currentUserId());
                old.setCreateTime(LocalDateTime.now());
                noteLikeMapper.insert(old);
                note.setLikeCount(defaultValue(note.getLikeCount(), 0) + 1);
            } else if (!liked && old != null) {
                noteLikeMapper.deleteById(old.getId());
                note.setLikeCount(Math.max(defaultValue(note.getLikeCount(), 0) - 1, 0));
            }
            noteMapper.updateById(note);
            return liked;
        }
        EduReply reply = replyMapper.selectById(bizId);
        if (reply != null) {
            Long userId = currentUserId();
            String likeKey = "edu:reply:likes:" + bizId;
            if (liked) {
                if (Boolean.TRUE.equals(redisService.sIsMember(likeKey, String.valueOf(userId)))) {
                    return true;
                }
                redisService.sAdd(likeKey, String.valueOf(userId));
                reply.setLikeCount(defaultValue(reply.getLikeCount(), 0) + 1);
            } else {
                if (Boolean.FALSE.equals(redisService.sIsMember(likeKey, String.valueOf(userId)))) {
                    return false;
                }
                redisService.sRemove(likeKey, String.valueOf(userId));
                reply.setLikeCount(Math.max(defaultValue(reply.getLikeCount(), 0) - 1, 0));
            }
            reply.setUpdateTime(LocalDateTime.now());
            replyMapper.updateById(reply);
            return liked;
        }
        EduQuestion question = requireQuestion(bizId);
        EduQuestionLike old = questionLikeMapper.selectOne(new LambdaQueryWrapper<EduQuestionLike>()
                .eq(EduQuestionLike::getQuestionId, bizId).eq(EduQuestionLike::getUserId, currentUserId()));
        if (liked && old == null) {
            old = new EduQuestionLike();
            old.setId(newId());
            old.setQuestionId(bizId);
            old.setUserId(currentUserId());
            old.setCreateTime(LocalDateTime.now());
            questionLikeMapper.insert(old);
            question.setLikeCount(defaultValue(question.getLikeCount(), 0) + 1);
        } else if (!liked && old != null) {
            questionLikeMapper.deleteById(old.getId());
            question.setLikeCount(Math.max(defaultValue(question.getLikeCount(), 0) - 1, 0));
        }
        questionMapper.updateById(question);
        return liked;
    }

    @Override
    public Map<String, Object> questionView(EduQuestion item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("userId", item.getUserId());
        result.put("courseId", item.getCourseId());
        result.put("title", item.getTitle());
        result.put("content", item.getContent());
        result.put("description", item.getContent());
        result.put("category", item.getCategory());
        result.put("sectionId", item.getCategory());
        result.put("viewCount", item.getViewCount());
        result.put("replyCount", item.getReplyCount());
        result.put("replyTimes", item.getReplyCount());
        result.put("answerTimes", item.getReplyCount());
        result.put("likeCount", item.getLikeCount());
        result.put("likedTimes", item.getLikeCount());

        Long currentUid = currentUserId();
        boolean isLiked = false;
        if (currentUid != null) {
            isLiked = questionLikeMapper.selectCount(new LambdaQueryWrapper<EduQuestionLike>()
                    .eq(EduQuestionLike::getQuestionId, item.getId())
                    .eq(EduQuestionLike::getUserId, currentUid)) > 0;
        }
        result.put("liked", isLiked);

        Map<String, String> userInfo = resolveUserBasic(item.getUserId());
        result.put("userName", userInfo.getOrDefault("name", "学习者"));
        result.put("userIcon", userInfo.getOrDefault("avatar", ""));

        EduReply latest = replyMapper.selectOne(new LambdaQueryWrapper<EduReply>()
                .eq(EduReply::getQuestionId, item.getId())
                .eq(EduReply::getHidden, 0)
                .eq(EduReply::getStatus, ENABLED)
                .orderByDesc(EduReply::getCreateTime)
                .last("limit 1"));
        if (latest != null) {
            Map<String, String> latestUser = resolveUserBasic(latest.getUserId());
            result.put("latestReplyContent", latest.getContent());
            result.put("latestReplyUser", latestUser.getOrDefault("name", "热心学伴"));
            result.put("latestReplyTime", latest.getCreateTime());
        } else {
            result.put("latestReplyContent", null);
            result.put("latestReplyUser", null);
            result.put("latestReplyTime", null);
        }

        result.put("createTime", item.getCreateTime());
        return result;
    }

    @Override
    public Map<String, Object> replyView(EduReply item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("questionId", item.getQuestionId());
        result.put("answerId", item.getParentId());
        result.put("parentId", item.getParentId());
        result.put("userId", item.getUserId());
        result.put("content", item.getContent());

        Long currentUid = currentUserId();
        boolean isLiked = false;
        String likeKey = "edu:reply:likes:" + item.getId();
        if (currentUid != null) {
            isLiked = Boolean.TRUE.equals(redisService.sIsMember(likeKey, String.valueOf(currentUid)));
        }
        result.put("liked", isLiked);
        result.put("likedTimes", defaultValue(item.getLikeCount(), 0));

        long subReplyCount = replyMapper.selectCount(new LambdaQueryWrapper<EduReply>()
                .eq(EduReply::getHidden, 0)
                .eq(EduReply::getStatus, ENABLED)
                .eq(EduReply::getParentId, item.getId()));
        result.put("replyTimes", subReplyCount);

        Map<String, String> userInfo = resolveUserBasic(item.getUserId());
        result.put("userName", userInfo.getOrDefault("name", "学习者"));
        result.put("userIcon", userInfo.getOrDefault("avatar", ""));

        String targetName = "提问者";
        if (item.getParentId() != null && item.getParentId() > 0) {
            EduReply parent = replyMapper.selectById(item.getParentId());
            if (parent != null && parent.getUserId() != null) {
                Map<String, String> parentUser = resolveUserBasic(parent.getUserId());
                targetName = parentUser.getOrDefault("name", "同学");
            }
        }
        result.put("targetUserName", targetName);
        result.put("createTime", item.getCreateTime());
        return result;
    }

    @Override
    public Map<String, Object> noteView(EduNote item) {
        Long userId = currentUserId();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("userId", item.getUserId());
        result.put("authorId", item.getUserId());
        result.put("authorName", item.getUserId() != null && item.getUserId().equals(userId) ? currentUserName() : "学习者");
        result.put("title", item.getTitle());
        result.put("content", item.getContent());
        result.put("courseId", item.getCourseId());
        result.put("catalogId", item.getCatalogId());
        result.put("visibility", item.getVisibility());
        result.put("likedTimes", item.getLikeCount());
        result.put("isGathered", noteCollectMapper.selectCount(new LambdaQueryWrapper<EduNoteCollect>()
                .eq(EduNoteCollect::getNoteId, item.getId()).eq(EduNoteCollect::getUserId, userId)) > 0);
        result.put("liked", noteLikeMapper.selectCount(new LambdaQueryWrapper<EduNoteLike>()
                .eq(EduNoteLike::getNoteId, item.getId()).eq(EduNoteLike::getUserId, userId)) > 0);
        result.put("createTime", item.getCreateTime());
        return result;
    }

    @Override
    public EduQuestion requireQuestion(Long id) {
        EduQuestion value = questionMapper.selectById(id);
        if (value == null) throw new ServiceException("问题不存在");
        return value;
    }

    @Override
    public EduNote requireNote(Long id) {
        EduNote value = noteMapper.selectById(id);
        if (value == null) throw new ServiceException("笔记不存在");
        return value;
    }

    private Map<String, String> resolveUserBasic(Long userId) {
        if (userId == null) return Map.of("name", "匿名用户", "avatar", "");
        if (jdbcTemplate != null) {
            try {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT user_name, nick_name, avatar FROM share.sys_user WHERE user_id = ? LIMIT 1", userId);
                if (!rows.isEmpty()) {
                    Map<String, Object> r = rows.get(0);
                    String nick = (String) r.get("nick_name");
                    String user = (String) r.get("user_name");
                    String avatar = (String) r.get("avatar");
                    String displayName = StringUtils.hasText(nick) ? nick : (StringUtils.hasText(user) ? user : "学习者");
                    return Map.of("name", displayName, "avatar", avatar != null ? avatar : "");
                }
            } catch (Exception ignored) {}
        }
        return Map.of("name", Objects.equals(userId, currentUserId()) ? currentUserName() : "学习者" + userId, "avatar", "");
    }
}

