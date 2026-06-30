package com.lovart.maildesk.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * REST + OAuth2 login entry point.
 * <p>
 * P1-T04 wired {@code maildesk-infrastructure} onto the runtime classpath, so
 * {@code DataSourceAutoConfiguration} is now allowed to fire. MyBatis-Plus
 * mapper interfaces live in {@code com.lovart.maildesk.domain.**.mapper};
 * {@link MapperScan} forces Spring to instantiate proxies for them at boot.
 */
@SpringBootApplication(scanBasePackages = "com.lovart.maildesk")
@MapperScan("com.lovart.maildesk.domain.**.mapper")
public class MaildeskApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaildeskApiApplication.class, args);
    }
}
