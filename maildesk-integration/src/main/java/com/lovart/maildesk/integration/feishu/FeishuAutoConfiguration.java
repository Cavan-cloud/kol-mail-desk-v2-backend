package com.lovart.maildesk.integration.feishu;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FeishuProperties.class)
public class FeishuAutoConfiguration {
}
