package com.share.education.ai.tools.remediation;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 自适应知识闭包补救请求 DTO。
 */
@Data
public class AdaptiveRemediationRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 学员 ID */
    private Long userId;

    /** 薄弱概念或错题涉及的核心考点，例如 "分布式事务Seata", "SpringCloud微服务" */
    private String weakConcept;

    /** 历史错题标签或题目 ID (可选) */
    private List<Long> failedQuestionIds;

    public AdaptiveRemediationRequest() {}

    public AdaptiveRemediationRequest(Long userId, String weakConcept) {
        this.userId = userId;
        this.weakConcept = weakConcept;
    }
}
