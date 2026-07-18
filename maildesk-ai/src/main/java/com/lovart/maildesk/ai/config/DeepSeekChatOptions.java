package com.lovart.maildesk.ai.config;

import com.lovart.maildesk.ai.AiResolvedTarget;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.Map;

/**
 * DeepSeek V4 enables thinking mode by default; disable it for low-latency maildesk tasks (ADR-007).
 */
public final class DeepSeekChatOptions {

    public static final String PROVIDER_ID = "deepseek";

    private static final Map<String, Object> THINKING_DISABLED =
            Map.of("thinking", Map.of("type", "disabled"));

    private DeepSeekChatOptions() {}

    public static OpenAiChatOptions defaultProviderOptions() {
        return OpenAiChatOptions.builder().extraBody(THINKING_DISABLED).build();
    }

    public static OpenAiChatOptions.Builder builder(AiResolvedTarget target) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder().model(target.model());
        if (PROVIDER_ID.equals(target.providerId())) {
            builder.extraBody(THINKING_DISABLED);
        }
        return builder;
    }
}
