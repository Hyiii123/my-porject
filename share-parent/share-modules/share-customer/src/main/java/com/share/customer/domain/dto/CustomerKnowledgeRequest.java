package com.share.customer.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 知识库新增/修改请求。 */
@Data
public class CustomerKnowledgeRequest {
    private Long id;

    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题不能超过500个字符")
    private String question;

    @NotBlank(message = "答案不能为空")
    private String answer;

    @Size(max = 1000, message = "关键词不能超过1000个字符")
    private String keywords;

    @NotBlank(message = "分类不能为空")
    @Size(max = 64, message = "分类不能超过64个字符")
    private String category;

    private Integer status;
}
