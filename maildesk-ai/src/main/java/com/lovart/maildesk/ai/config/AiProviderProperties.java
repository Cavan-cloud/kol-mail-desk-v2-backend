package com.lovart.maildesk.ai.config;

import com.lovart.maildesk.ai.AiCapability;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "maildesk.ai")
public class AiProviderProperties {

    private String defaultProvider = "moonshot";

    private Map<String, Provider> providers = new LinkedHashMap<>();

    private Map<String, CapabilityBinding> capabilities = new LinkedHashMap<>();

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public Map<String, Provider> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, Provider> providers) {
        this.providers = providers;
    }

    public Map<String, CapabilityBinding> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Map<String, CapabilityBinding> capabilities) {
        this.capabilities = capabilities;
    }

    public Provider provider(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return null;
        }
        return providers.get(providerId);
    }

    public boolean isProviderConfigured(String providerId) {
        Provider provider = provider(providerId);
        return provider != null && provider.isConfigured();
    }

    public CapabilityBinding capability(AiCapability capability) {
        return capabilities.get(capability.name().toLowerCase());
    }

    public static final class Provider {

        private String baseUrl = "";

        private String apiKey = "";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank()
                    && baseUrl != null && !baseUrl.isBlank();
        }
    }

    public static final class CapabilityBinding {

        private String provider;

        private String model;

        private String fallbackProvider;

        private String fallbackModel;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getFallbackProvider() {
            return fallbackProvider;
        }

        public void setFallbackProvider(String fallbackProvider) {
            this.fallbackProvider = fallbackProvider;
        }

        public String getFallbackModel() {
            return fallbackModel;
        }

        public void setFallbackModel(String fallbackModel) {
            this.fallbackModel = fallbackModel;
        }
    }
}
