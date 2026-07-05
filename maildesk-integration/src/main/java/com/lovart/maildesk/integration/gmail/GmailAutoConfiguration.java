package com.lovart.maildesk.integration.gmail;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GmailProperties.class)
public class GmailAutoConfiguration {
}
