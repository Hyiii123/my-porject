package com.share.education.api.factory;

import com.share.common.core.web.domain.AjaxResult;
import com.share.education.api.RemoteTeacherProfileService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** 教师资料同步降级实现，不阻断系统用户的主流程。 */
@Component
public class RemoteTeacherProfileFallbackFactory implements FallbackFactory<RemoteTeacherProfileService> {
    private static final Logger log = LoggerFactory.getLogger(RemoteTeacherProfileFallbackFactory.class);

    @Override
    public RemoteTeacherProfileService create(Throwable cause) {
        log.warn("教育服务教师资料调用失败：{}", cause == null ? "unknown" : cause.getMessage());
        return new RemoteTeacherProfileService() {
            @Override
            public AjaxResult getByUserId(Long userId, String source) {
                return AjaxResult.error("教育服务暂不可用");
            }

            @Override
            public AjaxResult save(Map<String, Object> profile, String source) {
                return AjaxResult.error("教师扩展资料同步失败");
            }

            @Override
            public AjaxResult deleteByUserId(Long userId, String source) {
                return AjaxResult.error("教师扩展资料删除失败");
            }
        };
    }
}
