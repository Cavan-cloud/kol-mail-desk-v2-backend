package com.lovart.maildesk.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Worker-specific scheduling knobs (cron, batch sizes, lock TTLs).
 */
@ConfigurationProperties(prefix = "maildesk.worker")
public record WorkerProperties(FeishuDeltaSync feishuDeltaSync) {

    public WorkerProperties {
        if (feishuDeltaSync == null) {
            feishuDeltaSync = FeishuDeltaSync.defaults();
        }
    }

    public record FeishuDeltaSync(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("0 */30 * * * *") String cron,
            @DefaultValue("50") int maxRecords,
            @DefaultValue("25m") Duration lockTtl) {

        public FeishuDeltaSync {
            if (maxRecords <= 0) {
                maxRecords = 50;
            }
            if (lockTtl == null || lockTtl.isZero() || lockTtl.isNegative()) {
                lockTtl = Duration.ofMinutes(25);
            }
        }

        static FeishuDeltaSync defaults() {
            return new FeishuDeltaSync(true, "0 */30 * * * *", 50, Duration.ofMinutes(25));
        }
    }
}
