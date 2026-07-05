package com.lovart.maildesk.ai.fallback;

import com.lovart.maildesk.ai.AiCapability;
import com.lovart.maildesk.ai.AiModelRouter;
import com.lovart.maildesk.ai.AiResolvedTarget;
import com.lovart.maildesk.ai.usage.AiCallMetrics;
import com.lovart.maildesk.ai.usage.AiCostEstimator;
import com.lovart.maildesk.ai.usage.AiInvocationAttempt;
import com.lovart.maildesk.common.context.UserContext;
import com.lovart.maildesk.domain.usage.AiUsageEntry;
import com.lovart.maildesk.domain.usage.AiUsageLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Function;

/**
 * Three-tier AI invocation: primary provider → fallback provider → local heuristic (ADR-007 / P4-T07).
 * Records each provider attempt to {@code ai_usage_log} (P4-T10).
 */
public class AiInvocationPipeline {

    private static final Logger log = LoggerFactory.getLogger(AiInvocationPipeline.class);

    private final AiModelRouter modelRouter;
    private final AiUsageLogPort usageLog;

    public AiInvocationPipeline(AiModelRouter modelRouter) {
        this(modelRouter, null);
    }

    public AiInvocationPipeline(AiModelRouter modelRouter, AiUsageLogPort usageLog) {
        this.modelRouter = modelRouter;
        this.usageLog = usageLog != null ? usageLog : AiUsageLogPort.NOOP;
    }

    public <T> T run(
            AiCapability capability,
            String operation,
            Function<AiResolvedTarget, AiInvocationAttempt<T>> invoke,
            AiInvocationFallbacks<T> fallbacks) {
        Optional<AiResolvedTarget> primary = modelRouter.resolvePrimary(capability);
        if (primary.isEmpty()) {
            log.warn("[ai] {}: no AI provider configured — using heuristic fallback", operation);
            recordHeuristicSkip(capability, "none", null, false);
            return fallbacks.onNoProvider().get();
        }

        AiInvocationAttempt<T> primaryAttempt = invoke.apply(primary.get());
        recordAttempt(capability, primaryAttempt.metrics());
        if (primaryAttempt.result().isPresent()) {
            return primaryAttempt.result().get();
        }

        Optional<AiResolvedTarget> fallbackTarget = modelRouter.resolveFallback(capability);
        if (fallbackTarget.isPresent()) {
            AiInvocationAttempt<T> fallbackAttempt = invoke.apply(fallbackTarget.get());
            recordAttempt(capability, fallbackAttempt.metrics());
            if (fallbackAttempt.result().isPresent()) {
                return fallbackAttempt.result().get();
            }
        }

        log.warn("[ai] {}: all providers failed — using heuristic fallback", operation);
        recordHeuristicSkip(capability, "heuristic", null, false);
        return fallbacks.onAllProvidersFailed().get();
    }

    private void recordAttempt(AiCapability capability, AiCallMetrics metrics) {
        usageLog.record(new AiUsageEntry(
                UserContext.getUserId(),
                capability.name().toLowerCase(),
                metrics.providerId(),
                metrics.model(),
                metrics.promptTokens(),
                metrics.completionTokens(),
                metrics.durationMs(),
                metrics.success(),
                AiCostEstimator.estimate(
                        metrics.providerId(),
                        metrics.model(),
                        metrics.promptTokens(),
                        metrics.completionTokens())));
    }

    private void recordHeuristicSkip(AiCapability capability, String provider, String model, boolean success) {
        usageLog.record(new AiUsageEntry(
                UserContext.getUserId(),
                capability.name().toLowerCase(),
                provider,
                model,
                null,
                null,
                0,
                success,
                null));
    }
}
