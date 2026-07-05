package com.lovart.maildesk.application.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Domain metrics aligned with {@code specs/02-backend-design.md § 八、可观测性}.
 * AI counters are emitted from {@code AiUsageLogService} in infrastructure.
 */
@Component
public class MaildeskMetrics {

    private final MeterRegistry registry;
    private final AtomicReference<Double> dispatchLagSeconds = new AtomicReference<>(0.0);

    public MaildeskMetrics(ObjectProvider<MeterRegistry> registryProvider) {
        this.registry = registryProvider.getIfAvailable();
        if (this.registry != null) {
            this.registry.gauge("scheduled_email.dispatch.lag_seconds", dispatchLagSeconds, AtomicReference::get);
        }
    }

    public void recordGmailSync(Duration duration, String mode, String outcome) {
        if (registry == null) {
            return;
        }
        String safeMode = mode == null ? "unknown" : mode;
        String safeOutcome = outcome == null ? "unknown" : outcome;
        Timer.builder("gmail.sync.duration")
                .description("Gmail sync wall time per user invocation")
                .tag("mode", safeMode)
                .tag("outcome", safeOutcome)
                .register(registry)
                .record(duration);
        if ("error".equals(safeOutcome)) {
            Counter.builder("gmail.sync.failed")
                    .description("Gmail sync failures")
                    .tag("mode", safeMode)
                    .register(registry)
                    .increment();
        }
    }

    public void updateDispatchLagSeconds(double lagSeconds) {
        dispatchLagSeconds.set(Math.max(0.0, lagSeconds));
    }
}
