package com.lovart.maildesk.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = "com.lovart.maildesk",
        exclude = DataSourceAutoConfiguration.class
)
public class MaildeskWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaildeskWorkerApplication.class, args);
    }
}
