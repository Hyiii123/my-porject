package com.share.education;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.share.common.security.annotation.EnableCustomConfig;
import com.share.common.security.annotation.EnableRyFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 天机学堂课程、学习、互动、考试和积分服务。 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class ShareEducationApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShareEducationApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  教育业务服务启动成功  ლ(´ڡ`ლ)ﾞ");
    }
}
