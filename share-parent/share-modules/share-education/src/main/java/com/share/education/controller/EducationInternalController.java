package com.share.education.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.security.annotation.InnerAuth;
import com.share.education.service.EducationService;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 教育服务内部接口，只供微服务间 Feign 调用。 */
@RestController
@RequestMapping("/internal")
public class EducationInternalController extends BaseController {
    private final EducationService educationService;

    public EducationInternalController(EducationService educationService) {
        this.educationService = educationService;
    }

    @InnerAuth
    @GetMapping("/teachers/by-user/{userId}")
    public AjaxResult teacherByUser(@PathVariable Long userId) {
        return success(educationService.teacherProfile(userId));
    }

    @InnerAuth
    @PostMapping("/teachers/profile")
    public AjaxResult saveTeacher(@RequestBody Map<String, Object> profile) {
        return success(educationService.saveTeacherProfile(profile));
    }

    @InnerAuth
    @DeleteMapping("/teachers/by-user/{userId}")
    public AjaxResult deleteTeacher(@PathVariable Long userId) {
        educationService.deleteTeacherProfile(userId);
        return success();
    }

    /** 触发多智能体协同推荐与路径规划 (供客服/其他微服务 Feign 调用) */
    @InnerAuth
    @PostMapping("/ai/agent/orchestrate")
    public AjaxResult orchestrateAgentRecommend(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "targetRole", required = false) String targetRole,
            @RequestParam(value = "limit", required = false, defaultValue = "4") Integer limit) {
        return success(educationService.orchestrateAgentRecommend(userId, targetRole, limit));
    }
}
