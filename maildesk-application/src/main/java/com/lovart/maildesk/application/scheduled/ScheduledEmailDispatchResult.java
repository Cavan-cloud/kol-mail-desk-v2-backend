package com.lovart.maildesk.application.scheduled;

import java.util.List;

/**
 * Summary of one worker dispatch tick.
 */
public record ScheduledEmailDispatchResult(
        int claimed,
        int sent,
        int failed,
        int skipped,
        List<ItemResult> items
) {
    public record ItemResult(
            java.util.UUID scheduledEmailId,
            String status,
            String message
    ) {}
}
