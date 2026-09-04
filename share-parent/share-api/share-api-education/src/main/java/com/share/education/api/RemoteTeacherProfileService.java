package com.share.education.api;

import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.constant.ServiceNameConstants;
import com.share.common.core.web.domain.AjaxResult;
import com.share.education.api.factory.RemoteTeacherProfileFallbackFactory;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 教育服务教师资料内部调用契约。
 *
 * <p>系统服务只通过该契约同步教师扩展资料，不直接访问教育服务的数据表，
 * 以保持服务边界和后续拆分数据库的可行性。</p>
 */
@FeignClient(contextId = "remoteTeacherProfileService", value = ServiceNameConstants.EDUCATION_SERVICE,
        fallbackFactory = RemoteTeacherProfileFallbackFactory.class)
public interface RemoteTeacherProfileService {

    /** 按系统用户编号查询教师扩展资料。 */
    @GetMapping("/internal/teachers/by-user/{userId}")
    AjaxResult getByUserId(@PathVariable("userId") Long userId,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /** 新增或更新教师扩展资料。 */
    @PostMapping("/internal/teachers/profile")
    AjaxResult save(@RequestBody Map<String, Object> profile,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /** 删除教师扩展资料（逻辑删除）。 */
    @DeleteMapping("/internal/teachers/by-user/{userId}")
    AjaxResult deleteByUserId(@PathVariable("userId") Long userId,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
