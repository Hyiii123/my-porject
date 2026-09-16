package com.share.mq;

import com.share.common.security.annotation.EnableCustomConfig;
import com.share.common.security.annotation.EnableRyFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 智问学伴全站消息中枢与事件调度中心服务
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class ShareMqApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShareMqApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  智问学伴消息中枢 (Share-MQ) 启动成功  ლ(´ڡ`ლ)ﾞ");
    }
}
