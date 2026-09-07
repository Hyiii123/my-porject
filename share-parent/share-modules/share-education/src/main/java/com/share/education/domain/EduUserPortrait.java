package com.share.education.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 学员多维动态学习画像实体类。
 * 包含技术技能向量、自适应难度偏好、完课投入度指标及综合标签。
 */
@Data
@TableName("edu_user_portrait")
public class EduUserPortrait implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 关联系统用户编号 */
    private Long userId;

    /** 目标岗位角色（如：Java后端工程师、前端全栈、AI算法工程师） */
    private String intendedRole;

    /** 技能画像掌握度向量 JSON: {"Java":85,"SpringBoot":75,...} */
    private String skillWeights;

    /** 自适应偏好难度：1初级入门，2中级进阶，3高级架构 */
    private Integer preferredDifficulty;

    /** 学习风格：systematic系统型 / practical实战型 / fast_paced速成型 */
    private String learningStyle;

    /** 历史完课率百分比 (0.00 ~ 100.00) */
    private BigDecimal completionRate;

    /** 活跃学习时段：night夜猫子 / morning早起型 / weekend周末突击 */
    private String studyFrequency;

    /** 累计有效学习小时数 */
    private BigDecimal totalStudyHours;

    /** 价格敏感度：low / medium / high */
    private String priceSensitivity;

    /** 综合个性化标签列表 JSON 格式 */
    private String tags;

    /** 最后计算/刷新时间 */
    private LocalDateTime lastCalculatedTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;

    @Version
    private Integer version;
}
