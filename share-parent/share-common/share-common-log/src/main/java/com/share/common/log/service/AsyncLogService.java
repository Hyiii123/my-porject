package com.share.common.log.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import com.share.common.core.constant.SecurityConstants;
import com.share.system.api.RemoteLogService;
import com.share.system.api.domain.SysOperLog;

/**
 * 异步调用日志服务
 *
 * @author share
 */
@Service
public class AsyncLogService
{
    private static final Logger log = LoggerFactory.getLogger(AsyncLogService.class);

    @Autowired
    private RemoteLogService remoteLogService;

    /**
     * 保存系统日志记录
     */
    @Async
    public void saveSysLog(SysOperLog sysOperLog)
    {
        try
        {
            // 异步线程不需要绑定主线程可能已被 Tomcat 回收的 Request 上下文
            RequestContextHolder.resetRequestAttributes();
            remoteLogService.saveLog(sysOperLog, SecurityConstants.INNER);
        }
        catch (Exception e)
        {
            log.warn("【异步日志】保存操作日志至 share-system 异常: {}", e.getMessage());
        }
    }
}
