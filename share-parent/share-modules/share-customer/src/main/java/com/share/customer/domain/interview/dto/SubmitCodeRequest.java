package com.share.customer.domain.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class SubmitCodeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "面试场次ID不能为空")
    private Long sessionId;

    private Long turnId;

    @NotBlank(message = "算法题目名称不能为空")
    private String problemTitle;

    private String language = "java";

    @NotBlank(message = "提交代码不能为空")
    private String userCode;
}
