package com.lovart.maildesk.ai;

import com.lovart.maildesk.ai.config.AiProviderProperties;
import com.lovart.maildesk.ai.config.AiProviderProperties.CapabilityBinding;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Map;
import java.util.Optional;

/**
 * Resolves which LLM provider + model to use per {@link AiCapability}, with optional fallback
 * routing (ADR-007). Retry-on-failure is implemented in {@link com.lovart.maildesk.ai.fallback.AiInvocationPipeline} (P4-T07).
 */
public class AiModelRouter {

    private final AiProviderProperties properties;
    private final Map<String, ChatModel> chatModelsByProvider;

    public AiModelRouter(AiProviderProperties properties, Map<String, ChatModel> chatModelsByProvider) {
        this.properties = properties;
        this.chatModelsByProvider = Map.copyOf(chatModelsByProvider);
    }

    public Optional<AiResolvedTarget> resolvePrimary(AiCapability capability) {
        return resolve(capability, false);
    }

    public Optional<AiResolvedTarget> resolveFallback(AiCapability capability) {
        return resolve(capability, true);
    }

    public boolean isProviderConfigured(String providerId) {
        return properties.isProviderConfigured(providerId);
    }

    public String defaultProviderId() {
        return properties.getDefaultProvider();
    }

    private Optional<AiResolvedTarget> resolve(AiCapability capability, boolean fallback) {
        CapabilityBinding binding = properties.capability(capability);
        if (binding == null) {
            return Optional.empty();
        }

        String providerId;
        String model;
        if (fallback) {
            providerId = binding.getFallbackProvider();
            model = binding.getFallbackModel();
            if (providerId == null || providerId.isBlank() || model == null || model.isBlank()) {
                return Optional.empty();
            }
        } else {
            providerId = firstNonBlank(binding.getProvider(), properties.getDefaultProvider());
            model = binding.getModel();
            if (model == null || model.isBlank()) {
                return Optional.empty();
            }
        }

        if (!properties.isProviderConfigured(providerId)) {
            return Optional.empty();
        }

        ChatModel chatModel = chatModelsByProvider.get(providerId);
        if (chatModel == null) {
            return Optional.empty();
        }

        return Optional.of(new AiResolvedTarget(providerId, model, chatModel, fallback));
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }
}
