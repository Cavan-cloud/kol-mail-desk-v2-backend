package com.lovart.maildesk.application.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WorkbenchProperties.class)
public class WorkbenchConfig {
}
