package com.share.mq.trade.feign;

import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.web.domain.AjaxResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 交易服务内部 Feign 接口调用客户端
 */
@FeignClient(contextId = "remoteTradeInternalService", value = "share-trade")
public interface RemoteTradeInternalService {

    @PutMapping("/internal/orders/{id}/cancel")
    AjaxResult cancelOrder(@PathVariable("id") Long id, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/internal/orders/{id}")
    AjaxResult getOrder(@PathVariable("id") Long id, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
