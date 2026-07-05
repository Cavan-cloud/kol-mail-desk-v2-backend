package com.lovart.maildesk.domain.usage;

/**
 * Persists AI provider call metrics for cost / reliability tracking (P4-T10).
 */
public interface AiUsageLogPort {

    AiUsageLogPort NOOP = entry -> {};

    void record(AiUsageEntry entry);
}
