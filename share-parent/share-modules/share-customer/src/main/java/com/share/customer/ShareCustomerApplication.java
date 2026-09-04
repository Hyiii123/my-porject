package com.share.customer;

import com.share.common.security.annotation.EnableCustomConfig;
import com.share.common.security.annotation.EnableRyFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.share.customer.config.CustomerAiProperties;

/**
 * 客服服务启动类。
 *
 * <p>客服服务是增量业务模块，独立使用 tj_customer 数据库，
 * 不会改写底座已有业务表。</p>
 */
@EnableCustomConfig
@EnableRyFeignClients
@EnableConfigurationProperties(CustomerAiProperties.class)
@SpringBootApplication
public class ShareCustomerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShareCustomerApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  客服服务启动成功  ლ(´ڡ`ლ)ﾞ");
    }
}
