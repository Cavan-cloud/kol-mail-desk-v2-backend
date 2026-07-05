package com.lovart.maildesk.domain.usage;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Append-only AI invocation audit row (maps to {@code ai_usage_log}).
 */
public record AiUsageEntry(
        UUID userId,
        String capability,
        String provider,
        String model,
        Integer promptTokens,
        Integer completionTokens,
        int durationMs,
        boolean success,
        BigDecimal estimatedCostCny) {
}
