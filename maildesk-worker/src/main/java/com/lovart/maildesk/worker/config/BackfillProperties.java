package com.lovart.maildesk.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * CLI flags for {@code spring.profiles.active=backfill} (Feishu full roster import).
 */
@ConfigurationProperties(prefix = "maildesk.backfill")
public record BackfillProperties(
        @DefaultValue("false") boolean dryRun,
        /** {@code 0} scans every sheet tab; {@code 2} matches the default HTTP sync window. */
        @DefaultValue("0") int recentMonths,
        @DefaultValue("true") boolean exitAfter,
        @DefaultValue("60m") Duration lockTtl) {

    public BackfillProperties {
        if (recentMonths < 0) {
            recentMonths = 0;
        }
        if (lockTtl == null || lockTtl.isZero() || lockTtl.isNegative()) {
            lockTtl = Duration.ofMinutes(60);
        }
    }
}
