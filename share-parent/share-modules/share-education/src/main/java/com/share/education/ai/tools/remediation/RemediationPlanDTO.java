package com.share.education.ai.tools.remediation;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 错题与薄弱知识点最小前置闭包补救方案 DTO。
 */
@Data
public class RemediationPlanDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 目标薄弱概念或挂科考点 */
    private String targetConcept;

    /** 诊断出的直接与间接先修前驱闭包 (按拓扑序由浅入深排列) */
    private List<String> prerequisiteClosureChain;

    /** 优先急需恶补的基石前置知识点 */
    private List<String> missingFoundationalConcepts;

    /** 智能体自适应生成的补救微任务序列 */
    private List<Map<String, Object>> remedialActionTasks;

    /** 推荐强相关的针对性补漏课程编号 */
    private List<Long> recommendedCourseIds;

    /** 教学法导师审判与诊断说明 */
    private String diagnosticExplanation;

    public RemediationPlanDTO() {}
}
