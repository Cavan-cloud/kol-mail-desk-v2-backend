package com.lovart.maildesk.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Legacy Supabase read-only JDBC for one-shot credential migration (P6-T10).
 */
@ConfigurationProperties(prefix = "maildesk.migration")
public record MigrationProperties(
        @DefaultValue("") String sourceJdbcUrl,
        @DefaultValue("") String sourceUsername,
        @DefaultValue("") String sourcePassword) {

    public boolean isConfigured() {
        return sourceJdbcUrl != null && !sourceJdbcUrl.isBlank();
    }
}
