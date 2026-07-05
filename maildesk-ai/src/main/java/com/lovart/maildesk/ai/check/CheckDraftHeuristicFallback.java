package com.lovart.maildesk.ai.check;

import java.util.List;

/**
 * Fallback when no AI provider is configured or all providers fail.
 * Per cost design: returns empty issues so send flow is not blocked.
 */
public final class CheckDraftHeuristicFallback {

    private CheckDraftHeuristicFallback() {}

    public static CheckDraftResult empty() {
        return empty(null);
    }

    public static CheckDraftResult empty(String aiError) {
        return new CheckDraftResult(List.of(), true, aiError);
    }
}
