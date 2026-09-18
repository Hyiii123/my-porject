package com.share.education.api.factory;

import com.share.common.core.web.domain.AjaxResult;
import com.share.education.api.RemoteEducationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.cloud.openfeign.FallbackFactory;

/** 教育服务不可用时的降级实现，交易服务可以依靠本地课程快照继续完成演示流程。 */
@Component
public class RemoteEducationFallbackFactory implements FallbackFactory<RemoteEducationService> {
    private static final Logger log = LoggerFactory.getLogger(RemoteEducationFallbackFactory.class);

    @Override
    public RemoteEducationService create(Throwable cause) {
        log.warn("教育服务调用失败，使用本地快照降级：{}", cause == null ? "unknown" : cause.getMessage());
        return new RemoteEducationService() {
            @Override
            public AjaxResult getCourse(Long id) {
                return AjaxResult.error("教育服务暂不可用");
            }

            @Override
            public AjaxResult enroll(Long courseId, Long userId, String source) {
                return AjaxResult.error("教育服务暂不可用");
            }

            @Override
            public AjaxResult revokeEnrollment(Long courseId, Long userId, String source) {
                return AjaxResult.error("教育服务暂不可用");
            }

            @Override
            public AjaxResult orchestrateAgentRecommend(Long userId, String targetRole, Integer limit, Integer difficulty, String source) {
                return AjaxResult.error("教育多智能体推荐规划服务暂不可用");
            }

            @Override
            public AjaxResult searchCourses(String keyword, Integer pageSize) {
                return AjaxResult.error("课程查询服务暂不可用");
            }
        };
    }
}
