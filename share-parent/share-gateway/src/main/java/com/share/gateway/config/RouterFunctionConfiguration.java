package com.share.gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import com.share.gateway.handler.ValidateCodeHandler;

/**
 * 路由配置信息
 *
 * @author share
 */
@Configuration
public class RouterFunctionConfiguration
{
    @Autowired
    private ValidateCodeHandler validateCodeHandler;

    @SuppressWarnings("rawtypes")
    @Bean
    public RouterFunction routerFunction()
    {
        return RouterFunctions.route(
                RequestPredicates.GET("/code").and(RequestPredicates.accept(MediaType.TEXT_PLAIN)),
                validateCodeHandler)
                .andRoute(RequestPredicates.GET("/").or(RequestPredicates.GET("/health")),
                        request -> org.springframework.web.reactive.function.server.ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(com.share.common.core.web.domain.AjaxResult.success("Zhiwen Gateway is running")));
    }
}
