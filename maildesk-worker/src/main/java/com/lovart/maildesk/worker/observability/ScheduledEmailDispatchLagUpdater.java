package com.lovart.maildesk.worker.observability;

import com.lovart.maildesk.application.observability.MaildeskMetrics;
import com.lovart.maildesk.common.context.TenantContext;
import com.lovart.maildesk.domain.scheduled.mapper.ScheduledEmailMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Refreshes {@code scheduled_email.dispatch.lag_seconds} gauge on the worker (P6-T08).
 */
@Component
@Profile("!backfill")
@ConditionalOnProperty(
        prefix = "maildesk.observability.dispatch-lag",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ScheduledEmailDispatchLagUpdater {

    private final ScheduledEmailMapper scheduledEmails;
    private final MaildeskMetrics metrics;
    private final UUID defaultTenantId;

    public ScheduledEmailDispatchLagUpdater(
            ScheduledEmailMapper scheduledEmails,
            MaildeskMetrics metrics,
            @Value("${maildesk.default-tenant-id:00000000-0000-0000-0000-000000000001}") String defaultTenantId) {
        this.scheduledEmails = scheduledEmails;
        this.metrics = metrics;
        this.defaultTenantId = UUID.fromString(defaultTenantId.trim());
    }

    @Scheduled(fixedDelayString = "${maildesk.observability.dispatch-lag.refresh-ms:30000}")
    public void refreshLagGauge() {
        UUID tenantId = TenantContext.tryGetTenantId().orElse(defaultTenantId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Double lag = scheduledEmails.selectMaxDispatchLagSeconds(now, tenantId);
        metrics.updateDispatchLagSeconds(lag == null ? 0.0 : lag);
    }
}
