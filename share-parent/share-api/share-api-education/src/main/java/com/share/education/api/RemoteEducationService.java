package com.share.education.api;

import com.share.common.core.constant.ServiceNameConstants;
import com.share.common.core.web.domain.AjaxResult;
import com.share.education.api.factory.RemoteEducationFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

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

    /** 为当前登录用户创建或补齐课程学习记录。 */
    @PostMapping("/internal/enrollments/{courseId}")
    AjaxResult enroll(@PathVariable("courseId") Long courseId);
}
