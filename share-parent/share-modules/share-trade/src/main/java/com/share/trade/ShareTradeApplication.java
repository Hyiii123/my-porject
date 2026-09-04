package com.share.trade;

import com.share.common.security.annotation.EnableCustomConfig;
import com.share.common.security.annotation.EnableRyFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 天机学堂交易、优惠券和订单服务。 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class ShareTradeApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShareTradeApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  交易业务服务启动成功  ლ(´ڡ`ლ)ﾞ");
    }
}
