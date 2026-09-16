package com.share.education.api;

import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.constant.ServiceNameConstants;
import com.share.common.core.web.domain.AjaxResult;
import com.share.education.api.factory.RemoteEducationFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 教育服务的跨服务调用契约。
 *
 * <p>交易服务只依赖本契约，不直接依赖教育服务的 Entity 或数据库，
 * 课程名称、封面、价格和购买后的学习记录均通过服务接口获取。</p>
 */
@FeignClient(contextId = "remoteEducationService", value = ServiceNameConstants.EDUCATION_SERVICE,
        fallbackFactory = RemoteEducationFallbackFactory.class)
public interface RemoteEducationService {

    /** 查询课程展示快照，返回 data 中的课程视图。 */
    @GetMapping("/courses/baseInfo/{id}")
    AjaxResult getCourse(@PathVariable("id") Long id);

    /** 为指定用户创建或补齐课程学习记录（支持显式指定 userId 与内部调用来源）。 */
    @PostMapping("/internal/enrollments/{courseId}")
    AjaxResult enroll(
            @PathVariable("courseId") Long courseId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestHeader(value = SecurityConstants.FROM_SOURCE, required = false) String source);

    /** 撤销指定用户的课程学习权限（退款时调用，支持显式指定 userId 与内部调用来源）。 */
    @PostMapping("/internal/enrollments/{courseId}/revoke")
    AjaxResult revokeEnrollment(
            @PathVariable("courseId") Long courseId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestHeader(value = SecurityConstants.FROM_SOURCE, required = false) String source);

    /** 为当前登录用户创建或补齐课程学习记录 (兼容旧方法)。 */
    default AjaxResult enroll(Long courseId) {
        return enroll(courseId, null, SecurityConstants.INNER);
    }

    /** 撤销当前登录用户的课程学习权限 (兼容旧方法)。 */
    default AjaxResult revokeEnrollment(Long courseId) {
        return revokeEnrollment(courseId, null, SecurityConstants.INNER);
    }

    /** 触发多智能体协同推荐与路径规划 (内部调用)。 */
    @PostMapping("/internal/ai/agent/orchestrate")
    AjaxResult orchestrateAgentRecommend(
        @RequestParam(value = "userId", required = false) Long userId,
        @RequestParam(value = "targetRole", required = false) String targetRole,
        @RequestParam(value = "limit", required = false, defaultValue = "4") Integer limit,
        @RequestHeader(SecurityConstants.FROM_SOURCE) String source
    );

    /** 搜索公开课程列表。 */
    @GetMapping("/courses/page")
    AjaxResult searchCourses(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pageSize", required = false, defaultValue = "5") Integer pageSize
    );
}
