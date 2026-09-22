package com.share.education.ai.tools.registry;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 智能体工具元数据描述符 (Tool Descriptor)。
 */
@Data
public class ToolDescriptor implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 工具唯一标识 (与 Spring Bean Name / Function Name 一致) */
    private String name;

    /** 用户可读中文显示名 */
    private String displayName;

    /** 分类: ALGORITHM, EXECUTION, MARKET, REMEDIATION, MEMORY */
    private String category;

    /** 核心功能职责详细描述 */
    private String description;

    /** 输入参数 JSON Schema */
    private Map<String, Object> parametersSchema;

    /** 安全防护级别: SAFE (只读无副作用), SANDBOXED (沙箱隔离执行), APPROVAL_REQUIRED (需人工确认) */
    private String securityLevel;

    /** 是否启用 */
    private Boolean enabled = true;

    public ToolDescriptor() {}

    public ToolDescriptor(String name, String displayName, String category, String description,
                          Map<String, Object> parametersSchema, String securityLevel) {
        this.name = name;
        this.displayName = displayName;
        this.category = category;
        this.description = description;
        this.parametersSchema = parametersSchema;
        this.securityLevel = securityLevel;
        this.enabled = true;
    }
}
