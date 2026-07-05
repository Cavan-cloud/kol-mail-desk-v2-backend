package com.lovart.maildesk.ai.usage;

import com.lovart.maildesk.ai.AiResolvedTarget;

/**
 * Per-call metrics captured around a single provider invocation (P4-T10).
 */
public record AiCallMetrics(
        String providerId,
        String model,
        boolean success,
        int durationMs,
        Integer promptTokens,
        Integer completionTokens,
        boolean fallbackProvider) {

    public static AiCallMetrics of(AiResolvedTarget target, boolean success, int durationMs, Integer prompt, Integer completion) {
        return new AiCallMetrics(
                target.providerId(),
                target.model(),
                success,
                durationMs,
                prompt,
                completion,
                target.fallback());
    }

    public static AiCallMetrics of(
            String providerId,
            String model,
            boolean success,
            int durationMs,
            Integer prompt,
            Integer completion,
            boolean fallbackProvider) {
        return new AiCallMetrics(providerId, model, success, durationMs, prompt, completion, fallbackProvider);
    }
}
