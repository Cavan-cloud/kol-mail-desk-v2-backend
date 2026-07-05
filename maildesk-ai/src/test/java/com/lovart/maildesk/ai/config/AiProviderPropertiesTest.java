package com.lovart.maildesk.ai.config;

import com.lovart.maildesk.ai.AiCapability;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderPropertiesTest {

    @Test
    void bindsMaildeskAiConfiguration() {
        Map<String, Object> source = Map.of(
                "maildesk.ai.default-provider", "deepseek",
                "maildesk.ai.providers.moonshot.base-url", "https://api.moonshot.cn/v1",
                "maildesk.ai.providers.moonshot.api-key", "moonshot-key",
                "maildesk.ai.providers.deepseek.base-url", "https://api.deepseek.com",
                "maildesk.ai.providers.deepseek.api-key", "deepseek-key",
                "maildesk.ai.capabilities.classify.provider", "moonshot",
                "maildesk.ai.capabilities.classify.model", "moonshot-v1-8k",
                "maildesk.ai.capabilities.classify.fallback-provider", "deepseek",
                "maildesk.ai.capabilities.classify.fallback-model", "deepseek-v4-flash",
                "maildesk.ai.capabilities.translate.model", "moonshot-v1-8k");

        AiProviderProperties properties = new Binder(new MapConfigurationPropertySource(source))
                .bind("maildesk.ai", AiProviderProperties.class)
                .get();

        assertThat(properties.getDefaultProvider()).isEqualTo("deepseek");
        assertThat(properties.isProviderConfigured("moonshot")).isTrue();
        assertThat(properties.isProviderConfigured("deepseek")).isTrue();
        assertThat(properties.capability(AiCapability.CLASSIFY).getModel()).isEqualTo("moonshot-v1-8k");
        assertThat(properties.capability(AiCapability.CLASSIFY).getFallbackProvider()).isEqualTo("deepseek");
        assertThat(properties.capability(AiCapability.TRANSLATE).getModel()).isEqualTo("moonshot-v1-8k");
    }
}
