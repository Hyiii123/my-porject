package com.share.customer.domain.interview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class ResumeSaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fileName;

    @NotBlank(message = "简历内容不能为空")
    private String rawContent;

    private String targetJob;

    private String targetCompany;
}
