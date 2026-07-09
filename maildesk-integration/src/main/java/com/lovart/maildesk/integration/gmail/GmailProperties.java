package com.lovart.maildesk.integration.gmail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "maildesk.gmail")
public record GmailProperties(
        @DefaultValue("") String clientId,
        @DefaultValue("") String clientSecret,
        @DefaultValue("10s") Duration connectTimeout,
        @DefaultValue("30s") Duration readTimeout,
        @DefaultValue("3") int maxRetries,
        /**
         * When false, Gmail sync skips LLM classification and uses inbound/outbound direction only
         * (needs-reply is derived from the latest email direction elsewhere).
         */
        @DefaultValue("true") boolean aiClassificationEnabled) {

    public boolean oauthConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }
}
