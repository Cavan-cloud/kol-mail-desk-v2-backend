package com.lovart.maildesk.application.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaildeskMetricsTest {

    private SimpleMeterRegistry registry;
    private MaildeskMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        @SuppressWarnings("unchecked")
        ObjectProvider<io.micrometer.core.instrument.MeterRegistry> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(registry);
        metrics = new MaildeskMetrics(provider);
    }

    @Test
    void recordGmailSync_incrementsTimerAndFailureCounter() {
        metrics.recordGmailSync(Duration.ofMillis(120), "history", "error");

        assertThat(registry.get("gmail.sync.duration").tag("mode", "history").tag("outcome", "error").timer())
                .isNotNull();
        assertThat(registry.get("gmail.sync.failed").tag("mode", "history").counter().count())
                .isEqualTo(1.0);
    }

    @Test
    void updateDispatchLagSeconds_exposesGauge() {
        metrics.updateDispatchLagSeconds(42.5);

        assertThat(registry.get("scheduled_email.dispatch.lag_seconds").gauge().value())
                .isEqualTo(42.5);
    }
}
