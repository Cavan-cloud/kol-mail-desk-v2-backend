package com.lovart.maildesk.infrastructure.usage;

import com.lovart.maildesk.domain.usage.AiUsageEntry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

/**
 * Micrometer counters/timers for AI usage (P6-T08).
 */
final class AiUsageMicrometer {

    private AiUsageMicrometer() {
    }

    static void record(MeterRegistry registry, AiUsageEntry entry) {
        String capability = entry.capability() == null ? "unknown" : entry.capability();
        String provider = entry.provider() == null ? "unknown" : entry.provider();
        boolean success = entry.success();
        Counter.builder("ai.invocation")
                .description("AI provider invocations")
                .tag("capability", capability)
                .tag("provider", provider)
                .tag("outcome", success ? "success" : "failure")
                .register(registry)
                .increment();
        if ("classify".equals(capability)) {
            int prompt = entry.promptTokens() == null ? 0 : entry.promptTokens();
            int completion = entry.completionTokens() == null ? 0 : entry.completionTokens();
            if (prompt > 0) {
                Counter.builder("ai.classify.tokens")
                        .description("Prompt tokens consumed by email classification")
                        .tag("kind", "prompt")
                        .register(registry)
                        .increment(prompt);
            }
            if (completion > 0) {
                Counter.builder("ai.classify.tokens")
                        .tag("kind", "completion")
                        .register(registry)
                        .increment(completion);
            }
        }
        if (entry.durationMs() > 0) {
            Timer.builder("ai.invocation.duration")
                    .tag("capability", capability)
                    .tag("provider", provider)
                    .register(registry)
                    .record(Duration.ofMillis(entry.durationMs()));
        }
    }
}
