package com.lovart.maildesk.ai.config;

import com.lovart.maildesk.ai.AiModelRouter;
import com.lovart.maildesk.ai.check.CheckDraftParser;
import com.lovart.maildesk.ai.classify.EmailClassificationParser;
import com.lovart.maildesk.ai.draft.ReplyDraftParser;
import com.lovart.maildesk.ai.fallback.AiInvocationPipeline;
import com.lovart.maildesk.ai.prompt.AiPromptCatalog;
import com.lovart.maildesk.ai.config.DeepSeekChatOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import com.lovart.maildesk.domain.usage.AiUsageLogPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Multi-provider Spring AI wiring (Moonshot + DeepSeek). Disables the single-provider
 * {@code spring.ai.openai.*} auto-config in favour of {@code maildesk.ai.*} (ADR-007).
 */
@AutoConfiguration
@EnableConfigurationProperties(AiProviderProperties.class)
@ConditionalOnProperty(prefix = "maildesk.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AiAutoConfiguration.class);

    @Bean
    AiModelRouter aiModelRouter(AiProviderProperties properties) {
        Map<String, ChatModel> chatModels = new LinkedHashMap<>();
        for (Map.Entry<String, AiProviderProperties.Provider> entry : properties.getProviders().entrySet()) {
            String providerId = entry.getKey();
            AiProviderProperties.Provider provider = entry.getValue();
            if (!provider.isConfigured()) {
                log.info("[ai] provider '{}' skipped — base-url or api-key not configured", providerId);
                continue;
            }
            OpenAiApi openAiApi = OpenAiApi.builder()
                    .baseUrl(normalizeBaseUrl(provider.getBaseUrl()))
                    .apiKey(provider.getApiKey())
                    .build();
            ChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(openAiApi)
                    .defaultOptions(DeepSeekChatOptions.PROVIDER_ID.equals(providerId)
                            ? DeepSeekChatOptions.defaultProviderOptions()
                            : OpenAiChatOptions.builder().build())
                    .build();
            chatModels.put(providerId, chatModel);
            log.info("[ai] provider '{}' registered (base-url={})", providerId, provider.getBaseUrl());
        }
        return new AiModelRouter(properties, chatModels);
    }

    @Bean
    AiPromptCatalog aiPromptCatalog() {
        return new AiPromptCatalog();
    }

    @Bean
    AiInvocationPipeline aiInvocationPipeline(AiModelRouter aiModelRouter, ObjectProvider<AiUsageLogPort> usageLogPort) {
        return new AiInvocationPipeline(aiModelRouter, usageLogPort.getIfAvailable());
    }

    @Bean
    EmailClassificationParser emailClassificationParser(ObjectMapper objectMapper) {
        return new EmailClassificationParser(objectMapper);
    }

    @Bean
    ReplyDraftParser replyDraftParser(ObjectMapper objectMapper) {
        return new ReplyDraftParser(objectMapper);
    }

    @Bean
    CheckDraftParser checkDraftParser(ObjectMapper objectMapper) {
        return new CheckDraftParser(objectMapper);
    }

    /**
     * Spring AI {@link OpenAiApi} appends {@code /v1/chat/completions} to base-url.
     * Moonshot docs often show {@code https://api.moonshot.cn/v1} — strip trailing {@code /v1}
     * so we do not call {@code /v1/v1/chat/completions}.
     */
    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return baseUrl;
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.endsWith("/v1")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
            while (trimmed.endsWith("/")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
        }
        return trimmed;
    }
}
