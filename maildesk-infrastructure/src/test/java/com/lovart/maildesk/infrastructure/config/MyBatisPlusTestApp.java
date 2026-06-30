package com.lovart.maildesk.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;

/**
 * Minimal Spring Boot application used solely to exercise
 * {@code MyBatisPlusConfig} from {@link MyBatisPlusConfigIT}.
 * <p>
 * Redis auto-configuration is excluded so the context can boot without a running
 * Redis instance — the infrastructure module pulls in
 * {@code spring-boot-starter-data-redis} for real use but it is irrelevant for
 * ORM-level integration tests.
 */
@SpringBootApplication(
        scanBasePackages = "com.lovart.maildesk",
        exclude = {
                RedisAutoConfiguration.class,
                RedisRepositoriesAutoConfiguration.class
        }
)
@MapperScan("com.lovart.maildesk.domain")
public class MyBatisPlusTestApp {
}
