package com.lovart.maildesk.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = "com.lovart.maildesk",
        exclude = DataSourceAutoConfiguration.class
)
public class MaildeskApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaildeskApiApplication.class, args);
    }
}
