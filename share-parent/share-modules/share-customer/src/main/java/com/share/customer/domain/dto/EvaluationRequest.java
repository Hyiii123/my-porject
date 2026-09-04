package com.share.customer.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/** 服务评价请求。 */
@Data
public class EvaluationRequest {
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1分")
    @Max(value = 5, message = "评分最高为5分")
    private Integer score;

    @Size(max = 5, message = "评价标签最多5个")
    private List<@Size(max = 30, message = "评价标签不能超过30个字符") String> tags;

    @Size(max = 1000, message = "评价内容不能超过1000个字符")
    private String comment;
}
