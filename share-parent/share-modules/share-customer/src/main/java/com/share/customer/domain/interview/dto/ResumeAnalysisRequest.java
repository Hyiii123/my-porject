package com.share.customer.domain.interview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class ResumeAnalysisRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "目标岗位不能为空")
    private String targetJob = "Java高级开发工程师";

    private String companyTarget = "阿里巴巴";

    @NotBlank(message = "简历内容不能为空")
    private String resumeContent;

    private String fileName;
}
