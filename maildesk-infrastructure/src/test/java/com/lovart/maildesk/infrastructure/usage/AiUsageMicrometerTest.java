package com.lovart.maildesk.infrastructure.usage;

import com.lovart.maildesk.domain.usage.AiUsageEntry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiUsageMicrometerTest {

    @Test
    void record_emitsInvocationAndClassifyTokenCounters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AiUsageMicrometer.record(
                registry,
                new AiUsageEntry(
                        UUID.randomUUID(),
                        "classify",
                        "moonshot",
                        "moonshot-v1-8k",
                        100,
                        20,
                        250,
                        true,
                        null));

        assertThat(registry.get("ai.invocation")
                        .tag("capability", "classify")
                        .tag("outcome", "success")
                        .counter()
                        .count())
                .isEqualTo(1.0);
        assertThat(registry.get("ai.classify.tokens").tag("kind", "prompt").counter().count())
                .isEqualTo(100.0);
        assertThat(registry.get("ai.classify.tokens").tag("kind", "completion").counter().count())
                .isEqualTo(20.0);
    }
}
