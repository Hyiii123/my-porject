package com.share.mq.education.feign;

import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.web.domain.AjaxResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 教育服务内部 Feign 接口调用客户端
 */
@FeignClient(contextId = "remoteEducationInternalService", value = "share-education")
public interface RemoteEducationInternalService {

    @PostMapping("/internal/enrollments/{courseId}")
    AjaxResult enrollCourseForUser(
            @PathVariable("courseId") Long courseId,
            @RequestParam("userId") Long userId,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @PostMapping("/internal/enrollments/{courseId}/revoke")
    AjaxResult revokeCourseForUser(
            @PathVariable("courseId") Long courseId,
            @RequestParam("userId") Long userId,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
