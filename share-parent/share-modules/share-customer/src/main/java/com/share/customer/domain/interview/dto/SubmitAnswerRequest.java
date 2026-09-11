package com.share.customer.domain.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class SubmitAnswerRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "面试场次ID不能为空")
    private Long sessionId;

    @NotNull(message = "问答轮次ID不能为空")
    private Long turnId;

    @NotBlank(message = "回答内容不能为空")
    private String userAnswer;
}
