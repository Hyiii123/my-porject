package com.share.file.controller;

import com.share.common.core.domain.R;
import com.share.file.service.FileMediaService;
import java.util.Arrays;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 媒资元数据兼容接口，保持原项目 /ms/medias 调用方式。 */
@RestController
@RequestMapping("/medias")
public class MediaController {
    private final FileMediaService mediaService;

    public MediaController(FileMediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping
    public R<Map<String, Object>> page(@RequestParam Map<String, Object> params) {
        return R.ok(mediaService.pageView(params));
    }

    @GetMapping("/statistics")
    public R<Map<String, Object>> statistics() {
        return R.ok(mediaService.statistics());
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> get(@PathVariable Long id) {
        return R.ok(mediaService.preview(id));
    }

    @PostMapping
    public R<Map<String, Object>> add(@RequestBody Map<String, Object> payload) {
        return R.ok(mediaService.save(payload));
    }

    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        payload.put("id", id);
        return R.ok(mediaService.save(payload));
    }

    @DeleteMapping("/{ids}")
    public R<Void> delete(@PathVariable Long[] ids) {
        mediaService.remove(Arrays.asList(ids));
        return R.ok();
    }

    @DeleteMapping
    public R<Void> deleteBatch(@RequestParam("ids") Long[] ids) {
        mediaService.remove(Arrays.asList(ids));
        return R.ok();
    }

    @GetMapping("/signature/upload")
    public R<Map<String, Object>> uploadCapability() {
        return R.ok(mediaService.uploadCapability());
    }

    @GetMapping("/signature/preview")
    public R<Map<String, Object>> preview(@RequestParam(required = false) Long id,
            @RequestParam(required = false) Long mediaId) {
        return R.ok(mediaService.preview(firstId(id, mediaId)));
    }

    @GetMapping("/signature/play")
    public R<Map<String, Object>> play(@RequestParam(required = false) Long id,
            @RequestParam(required = false) Long mediaId) {
        return R.ok(mediaService.preview(firstId(id, mediaId)));
    }

    private Long firstId(Long id, Long mediaId) {
        return id != null ? id : mediaId;
    }
}
