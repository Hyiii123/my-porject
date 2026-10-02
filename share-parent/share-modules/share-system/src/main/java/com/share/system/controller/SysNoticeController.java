package com.share.system.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.log.annotation.Log;
import com.share.common.log.enums.BusinessType;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.common.security.utils.SecurityUtils;
import com.share.system.domain.SysNotice;
import com.share.system.service.ISysNoticeService;

/**
 * 公告 信息操作处理
 *
 * @author share
 */
@RestController
@RequestMapping("/notice")
public class SysNoticeController extends BaseController
{
    @Autowired
    private ISysNoticeService noticeService;

    @Autowired(required = false)
    private com.share.common.redis.service.RedisService redisService;

    /**
     * 获取通知公告列表
     */
    @RequiresPermissions("system:notice:list")
    @GetMapping({"", "/", "/list"})
    public TableDataInfo list(SysNotice notice)
    {
        String cacheKey = null;
        if (redisService != null && !com.share.common.core.utils.StringUtils.isNotEmpty(notice.getNoticeTitle())) {
            cacheKey = "sys:notice:list:" + notice.getNoticeType() + ":" + notice.getStatus();
            try {
                Object raw = redisService.getCacheObject(cacheKey);
                TableDataInfo cached = parseCachedTable(raw);
                if (cached != null && cached.getRows() != null && !cached.getRows().isEmpty()) {
                    return cached;
                }
            } catch (Exception ignored) {}
        }
        startPage();
        List<SysNotice> list = noticeService.selectNoticeList(notice);
        TableDataInfo result = getDataTable(list);
        if (cacheKey != null && redisService != null && result.getRows() != null && !result.getRows().isEmpty()) {
            try {
                redisService.setCacheObject(cacheKey, result, 60L, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private TableDataInfo parseCachedTable(Object raw) {
        if (raw == null) return null;
        if (raw instanceof TableDataInfo t) return t;
        if (raw instanceof String str && !str.isBlank()) {
            try {
                return com.alibaba.fastjson2.JSON.parseObject(str, TableDataInfo.class);
            } catch (Exception ignored) {}
        }
        if (raw instanceof java.util.Map<?, ?> m) {
            TableDataInfo t = new TableDataInfo();
            t.setCode(m.containsKey("code") && m.get("code") instanceof Number n ? n.intValue() : 200);
            t.setMsg(m.containsKey("msg") && m.get("msg") != null ? m.get("msg").toString() : "查询成功");
            t.setTotal(m.containsKey("total") && m.get("total") instanceof Number n ? n.longValue() : 0L);
            if (m.get("rows") instanceof java.util.List<?> r) {
                t.setRows((java.util.List) r);
            }
            return t;
        }
        return null;
    }

    /**
     * 根据通知公告编号获取详细信息
     */
    @RequiresPermissions("system:notice:query")
    @GetMapping(value = "/{noticeId}")
    public AjaxResult getInfo(@PathVariable Long noticeId)
    {
        return success(noticeService.selectNoticeById(noticeId));
    }

    /**
     * 新增通知公告
     */
    @RequiresPermissions("system:notice:add")
    @Log(title = "通知公告", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysNotice notice)
    {
        notice.setCreateBy(SecurityUtils.getUsername());
        return toAjax(noticeService.insertNotice(notice));
    }

    /**
     * 修改通知公告
     */
    @RequiresPermissions("system:notice:edit")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysNotice notice)
    {
        notice.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(noticeService.updateNotice(notice));
    }

    /**
     * 删除通知公告
     */
    @RequiresPermissions("system:notice:remove")
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{noticeIds}")
    public AjaxResult remove(@PathVariable Long[] noticeIds)
    {
        return toAjax(noticeService.deleteNoticeByIds(noticeIds));
    }
}
