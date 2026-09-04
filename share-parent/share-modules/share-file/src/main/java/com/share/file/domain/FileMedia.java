package com.share.file.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 媒资元数据。
 *
 * <p>二进制文件由本文件服务负责保存，课程目录只保存 mediaId 和名称快照，
 * 避免教育服务直接依赖文件存储实现。</p>
 */
@Data
@TableName("file_media")
public class FileMedia implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    private String mediaName;
    private String fileName;
    private String fileId;
    private String fileUrl;
    private String format;
    private Long sizeBytes;
    private Integer durationSeconds;
    private String resolution;
    private String mediaType;
    private String status;
    private String description;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;
    @Version
    private Integer version;
}
