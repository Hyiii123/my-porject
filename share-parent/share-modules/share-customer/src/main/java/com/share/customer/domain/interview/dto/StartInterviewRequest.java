package com.share.customer.domain.interview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class StartInterviewRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "目标岗位不能为空")
    private String targetJob = "Java高级开发工程师";

    private String companyTarget = "大厂通用";

    private String interviewerStyle = "p7_architect"; // p7_architect / bytedance_tech / gentle_hr / standard

    private Integer totalTurns = 6;

    private Long resumeId;

    private Boolean enableResumeCustomization = true;
}
