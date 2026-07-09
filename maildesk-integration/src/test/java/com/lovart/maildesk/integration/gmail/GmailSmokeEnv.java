package com.lovart.maildesk.integration.gmail;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;

/**
 * Environment gate for {@link GmailSendSmokeTest}. Values are read from process env only
 * (never from {@code .env} files — export manually before running the smoke test).
 */
final class GmailSmokeEnv {

    private GmailSmokeEnv() {}

    static boolean isConfigured() {
        return !accessToken().isBlank() && !toAddress().isBlank();
    }

    static String accessToken() {
        return env("GMAIL_SMOKE_ACCESS_TOKEN");
    }

    static String toAddress() {
        return env("GMAIL_SMOKE_TO");
    }

    /** Falls back to {@code GMAIL_SMOKE_TO} when unset (self-CC). */
    static String ccAddress() {
        String cc = env("GMAIL_SMOKE_CC");
        return cc.isBlank() ? toAddress() : cc;
    }

    static GmailClientImpl buildClient() {
        GmailProperties properties = new GmailProperties(
                "",
                "",
                java.time.Duration.ofSeconds(10),
                java.time.Duration.ofSeconds(60),
                3,
                true);
        return new GmailClientImpl(properties, new ObjectMapper(), new RestTemplate(), new GmailMessageParser());
    }

    private static String env(String key) {
        String value = System.getenv(key);
        return value == null ? "" : value.trim();
    }
}
