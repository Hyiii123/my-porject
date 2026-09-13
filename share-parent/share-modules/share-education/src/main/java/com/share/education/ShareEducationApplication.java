package com.share.education;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.share.common.security.annotation.EnableCustomConfig;
import com.share.common.security.annotation.EnableRyFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.ai.model.openai.autoconfigure.*;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;

/** 智问学伴课程、学习、互动、考试和积分服务。 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication(exclude = {
    OpenAiChatAutoConfiguration.class,
    OpenAiImageAutoConfiguration.class,
    OpenAiAudioSpeechAutoConfiguration.class,
    OpenAiAudioTranscriptionAutoConfiguration.class,
    OpenAiEmbeddingAutoConfiguration.class,
    OpenAiModerationAutoConfiguration.class
})
public class ShareEducationApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShareEducationApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  教育业务服务启动成功  ლ(´ڡ`ლ)ﾞ");
    }

    /**
     * 规避 MyBatis-Plus 3.5.6 在 Spring Boot 3.2 下因 auto-ddl 未启用返回 NullBean 触发的 Runner 类型异常
     */
    @Bean
    public ApplicationRunner ddlApplicationRunner() {
        return args -> {};
    }
}
