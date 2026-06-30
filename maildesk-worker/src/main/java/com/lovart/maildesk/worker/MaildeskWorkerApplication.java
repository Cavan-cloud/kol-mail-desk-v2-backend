package com.lovart.maildesk.worker;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Worker entry point — schedules sync jobs / dispatches scheduled mail.
 * <p>
 * P1-T04 added the infrastructure dep and removed the DataSource exclusion so
 * the worker boots against the same PG + MyBatis-Plus stack the api uses.
 */
@SpringBootApplication(scanBasePackages = "com.lovart.maildesk")
@MapperScan("com.lovart.maildesk.domain.**.mapper")
public class MaildeskWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaildeskWorkerApplication.class, args);
    }
}
