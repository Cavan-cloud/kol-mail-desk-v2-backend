package com.lovart.maildesk.ai;

import org.springframework.ai.chat.model.ChatModel;

/**
 * A concrete provider + model + {@link ChatModel} instance selected for one AI call.
 */
public record AiResolvedTarget(
        String providerId,
        String model,
        ChatModel chatModel,
        boolean fallback) {

    public AiResolvedTarget {
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("providerId must not be blank");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        if (chatModel == null) {
            throw new IllegalArgumentException("chatModel must not be null");
        }
    }
}
