package com.lovart.maildesk.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Worker-specific scheduling knobs (cron, batch sizes, lock TTLs).
 */
@ConfigurationProperties(prefix = "maildesk.worker")
public record WorkerProperties(
        FeishuDeltaSync feishuDeltaSync,
        GmailIncrementalSync gmailIncrementalSync,
        ScheduledEmailDispatch scheduledEmailDispatch) {

    public WorkerProperties {
        if (feishuDeltaSync == null) {
            feishuDeltaSync = FeishuDeltaSync.defaults();
        }
        if (gmailIncrementalSync == null) {
            gmailIncrementalSync = GmailIncrementalSync.defaults();
        }
        if (scheduledEmailDispatch == null) {
            scheduledEmailDispatch = ScheduledEmailDispatch.defaults();
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

    public record GmailIncrementalSync(
            @DefaultValue("true") boolean enabled,
            /** Every 5 minutes — product requirement for Phase 3. */
            @DefaultValue("0 */5 * * * *") String cron,
            @DefaultValue("20") int maxUsersPerRun,
            @DefaultValue("4m") Duration lockTtl) {

        public GmailIncrementalSync {
            if (maxUsersPerRun <= 0) {
                maxUsersPerRun = 20;
            }
            if (lockTtl == null || lockTtl.isZero() || lockTtl.isNegative()) {
                lockTtl = Duration.ofMinutes(4);
            }
        }

        static GmailIncrementalSync defaults() {
            return new GmailIncrementalSync(true, "0 */5 * * * *", 20, Duration.ofMinutes(4));
        }
    }

    public record ScheduledEmailDispatch(
            @DefaultValue("true") boolean enabled,
            /** Every minute — product requirement for Phase 6. */
            @DefaultValue("0 * * * * *") String cron,
            @DefaultValue("10") int batchSize,
            @DefaultValue("55s") Duration lockTtl) {

        public ScheduledEmailDispatch {
            if (batchSize <= 0) {
                batchSize = 10;
            }
            if (lockTtl == null || lockTtl.isZero() || lockTtl.isNegative()) {
                lockTtl = Duration.ofSeconds(55);
            }
        }

        static ScheduledEmailDispatch defaults() {
            return new ScheduledEmailDispatch(true, "0 * * * * *", 10, Duration.ofSeconds(55));
        }
    }
}
