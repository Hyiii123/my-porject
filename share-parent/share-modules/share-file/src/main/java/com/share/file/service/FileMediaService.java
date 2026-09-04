package com.share.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.share.common.core.exception.ServiceException;
import com.share.common.security.utils.SecurityUtils;
import com.share.file.domain.FileMedia;
import com.share.file.mapper.FileMediaMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 媒资元数据服务。 */
@Service
public class FileMediaService {
    private static final String DEFAULT_STATUS = "unused";
    private static final String DEFAULT_TYPE = "other";

    private final FileMediaMapper mediaMapper;
    public FileMediaService(FileMediaMapper mediaMapper) {
        this.mediaMapper = mediaMapper;
    }

    public IPage<FileMedia> page(Map<String, ?> params) {
        long pageNo = number(params, "pageNo", number(params, "pageNum", number(params, "page", 1)));
        long pageSize = number(params, "pageSize", 10);
        String keyword = text(params, "keyword", text(params, "name", null));
        String type = text(params, "type", text(params, "mediaType", null));
        String status = text(params, "status", null);
        Page<FileMedia> page = new Page<>(safePage(pageNo), safeSize(pageSize));
        LambdaQueryWrapper<FileMedia> wrapper = new LambdaQueryWrapper<FileMedia>()
                .and(StringUtils.hasText(keyword), item -> item.like(FileMedia::getMediaName, keyword)
                        .or().like(FileMedia::getFileName, keyword))
                .eq(StringUtils.hasText(type), FileMedia::getMediaType, type)
                .eq(StringUtils.hasText(status), FileMedia::getStatus, status)
                .orderByDesc(FileMedia::getCreateTime).orderByDesc(FileMedia::getId);
        return mediaMapper.selectPage(page, wrapper);
    }

    public FileMedia get(Long id) {
        FileMedia value = id == null ? null : mediaMapper.selectById(id);
        if (value == null) {
            throw new ServiceException("媒资不存在");
        }
        return value;
    }

    /** 返回旧天机页面使用的 {total,list} 结构。 */
    public Map<String, Object> pageView(Map<String, ?> params) {
        IPage<FileMedia> page = page(params);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", page.getTotal());
        result.put("list", page.getRecords().stream().map(this::view).toList());
        return result;
    }

    @Transactional
    public Map<String, Object> save(Map<String, ?> payload) {
        Map<String, ?> source = payload == null ? Map.of() : payload;
        Long id = longValue(source.get("id"));
        FileMedia value = id == null ? new FileMedia() : get(id);
        LocalDateTime now = LocalDateTime.now();
        String fileName = firstText(source, "fileName", "filename", "name");
        String mediaName = firstText(source, "mediaName", "name", "filename", "fileName");
        require(StringUtils.hasText(mediaName), "媒资名称不能为空");
        value.setMediaName(mediaName.trim());
        if (StringUtils.hasText(fileName)) value.setFileName(fileName.trim());
        value.setFileId(firstText(source, "fileId", "resourceId"));
        String fileUrl = firstText(source, "fileUrl", "url", "mediaUrl");
        if (StringUtils.hasText(fileUrl)) value.setFileUrl(fileUrl.trim());
        String format = firstText(source, "format", "extension");
        if (!StringUtils.hasText(format)) format = extension(value.getFileName());
        value.setFormat(StringUtils.hasText(format) ? format.toUpperCase() : "MP4");
        Long sizeBytes = longValue(source.get("sizeBytes"));
        if (sizeBytes == null) sizeBytes = parseSize(source.get("size"));
        if (sizeBytes != null) value.setSizeBytes(sizeBytes);
        Integer duration = intValue(source.get("durationSeconds"));
        if (duration == null) duration = intValue(source.get("duration"));
        if (duration == null) duration = durationSeconds(source.get("mediaDuration"));
        if (duration != null) value.setDurationSeconds(Math.max(duration, 0));
        String resolution = firstText(source, "resolution");
        if (StringUtils.hasText(resolution)) value.setResolution(resolution.trim());
        String type = firstText(source, "type", "mediaType");
        if (StringUtils.hasText(type)) value.setMediaType(type.trim());
        if (!StringUtils.hasText(value.getMediaType())) value.setMediaType(DEFAULT_TYPE);
        String status = firstText(source, "status");
        if (StringUtils.hasText(status)) value.setStatus(status.trim());
        if (!StringUtils.hasText(value.getStatus())) value.setStatus(DEFAULT_STATUS);
        String description = firstText(source, "description");
        if (description != null) value.setDescription(description.trim());
        Long userId = currentUserId();
        value.setUpdateBy(userId);
        value.setUpdateTime(now);
        if (value.getId() == null) {
            value.setId(IdWorker.getId());
            value.setCreateBy(userId);
            value.setCreateTime(now);
            value.setDelFlag(0);
            value.setVersion(0);
            mediaMapper.insert(value);
        } else {
            mediaMapper.updateById(value);
        }
        return view(value);
    }

    @Transactional
    public void remove(List<Long> ids) {
        if (ids != null) {
            ids.stream().filter(Objects::nonNull).forEach(mediaMapper::deleteById);
        }
    }

    public Map<String, Object> statistics() {
        List<FileMedia> rows = mediaMapper.selectList(new LambdaQueryWrapper<FileMedia>()
                .select(FileMedia::getSizeBytes, FileMedia::getStatus));
        long totalBytes = rows.stream().map(FileMedia::getSizeBytes).filter(Objects::nonNull)
                .mapToLong(value -> value).sum();
        long usedCount = rows.stream().filter(item -> "used".equalsIgnoreCase(item.getStatus())).count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", rows.size());
        result.put("usedCount", usedCount);
        result.put("unusedCount", rows.size() - usedCount);
        result.put("totalBytes", totalBytes);
        result.put("totalSizeText", formatSize(totalBytes));
        return result;
    }

    private Map<String, Object> view(FileMedia item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("name", item.getMediaName());
        result.put("mediaName", item.getMediaName());
        result.put("filename", item.getFileName());
        result.put("fileName", item.getFileName());
        result.put("fileId", item.getFileId());
        result.put("url", item.getFileUrl());
        result.put("fileUrl", item.getFileUrl());
        result.put("playUrl", item.getFileUrl());
        result.put("format", item.getFormat());
        result.put("sizeBytes", item.getSizeBytes());
        result.put("size", formatSize(item.getSizeBytes()));
        result.put("durationSeconds", item.getDurationSeconds());
        result.put("duration", formatDuration(item.getDurationSeconds()));
        result.put("mediaDuration", item.getDurationSeconds());
        result.put("resolution", item.getResolution());
        result.put("type", item.getMediaType());
        result.put("mediaType", item.getMediaType());
        result.put("status", item.getStatus());
        result.put("description", item.getDescription());
        result.put("uploadTime", item.getCreateTime());
        result.put("createTime", item.getCreateTime());
        result.put("updateTime", item.getUpdateTime());
        return result;
    }

    public Map<String, Object> preview(Long id) {
        return view(get(id));
    }

    public Map<String, Object> uploadCapability() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", "local");
        result.put("uploadUrl", "/file/upload");
        result.put("expiresIn", 3600);
        result.put("message", "当前环境使用文件服务本地存储，无需第三方云媒资签名");
        return result;
    }

    private Long currentUserId() {
        Long userId = SecurityUtils.getUserId();
        return userId != null && userId > 0 ? userId : null;
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new ServiceException(message);
    }

    private long number(Map<String, ?> source, String key, long fallback) {
        Long value = longValue(source == null ? null : source.get(key));
        return value == null ? fallback : value;
    }

    private String text(Map<String, ?> source, String key, String fallback) {
        if (source == null) return fallback;
        String value = source.get(key) == null ? null : String.valueOf(source.get(key));
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String firstText(Map<String, ?> source, String... keys) {
        if (source == null) return null;
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) return String.valueOf(value);
        }
        return null;
    }

    private Long longValue(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) return null;
        try { return Long.valueOf(String.valueOf(value)); } catch (NumberFormatException ignored) { return null; }
    }

    private Integer intValue(Object value) {
        Long parsed = longValue(value);
        return parsed == null ? null : parsed.intValue();
    }

    private Long parseSize(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) return null;
        String text = String.valueOf(value).trim().toUpperCase();
        try {
            if (text.endsWith("GB")) return new BigDecimal(text.substring(0, text.length() - 2).trim())
                    .multiply(BigDecimal.valueOf(1024L * 1024L * 1024L)).longValue();
            if (text.endsWith("MB")) return new BigDecimal(text.substring(0, text.length() - 2).trim())
                    .multiply(BigDecimal.valueOf(1024L * 1024L)).longValue();
            if (text.endsWith("KB")) return new BigDecimal(text.substring(0, text.length() - 2).trim())
                    .multiply(BigDecimal.valueOf(1024L)).longValue();
            return Long.valueOf(text);
        } catch (NumberFormatException ignored) { return null; }
    }

    private Integer durationSeconds(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) return null;
        if (text.contains(":")) {
            String[] parts = text.split(":");
            try {
                int seconds = Integer.parseInt(parts[parts.length - 1]);
                int minutes = parts.length > 1 ? Integer.parseInt(parts[parts.length - 2]) : 0;
                int hours = parts.length > 2 ? Integer.parseInt(parts[parts.length - 3]) : 0;
                return hours * 3600 + minutes * 60 + seconds;
            } catch (NumberFormatException ignored) { return null; }
        }
        return intValue(text);
    }

    private String extension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) return null;
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private String formatSize(Long bytes) {
        if (bytes == null || bytes <= 0) return "0 B";
        double value = bytes;
        if (value >= 1024 * 1024 * 1024) return String.format("%.1f GB", value / (1024 * 1024 * 1024));
        if (value >= 1024 * 1024) return String.format("%.0f MB", value / (1024 * 1024));
        if (value >= 1024) return String.format("%.0f KB", value / 1024);
        return bytes + " B";
    }

    private String formatDuration(Integer seconds) {
        int value = seconds == null ? 0 : Math.max(seconds, 0);
        int hours = value / 3600;
        int minutes = (value % 3600) / 60;
        int remaining = value % 60;
        return hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, remaining)
                : String.format("%02d:%02d", minutes, remaining);
    }

    private long safePage(long value) { return value < 1 ? 1 : Math.min(value, 100000); }
    private long safeSize(long value) { return value < 1 ? 10 : Math.min(value, 200); }
}
