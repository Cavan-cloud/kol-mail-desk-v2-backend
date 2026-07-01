package com.lovart.maildesk.worker.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("backfill")
@EnableConfigurationProperties(BackfillProperties.class)
public class BackfillConfig {
}
