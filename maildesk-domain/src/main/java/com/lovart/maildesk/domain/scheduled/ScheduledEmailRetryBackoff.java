package com.lovart.maildesk.domain.scheduled;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Exponential backoff before re-claiming {@code status=failed} scheduled emails.
 * <p>
 * After attempt {@code n} fails, the next claim is allowed when
 * {@code now >= last_attempt_at + 2^(n-1) * baseDelay} (capped at {@link #MAX_DISPATCH_ATTEMPTS}).
 */
public final class ScheduledEmailRetryBackoff {

    public static final int MAX_DISPATCH_ATTEMPTS = ScheduledEmailStateMachine.MAX_DISPATCH_ATTEMPTS;
    public static final Duration DEFAULT_BASE_DELAY = Duration.ofMinutes(1);

    private ScheduledEmailRetryBackoff() {}

    /**
     * Delay before a failed row (with the given {@code attemptCount}) may be claimed again.
     * {@code attemptCount} is the value stored on the row after the last failed claim.
     */
    public static Duration delayAfterFailure(int attemptCount) {
        if (attemptCount <= 0) {
            return Duration.ZERO;
        }
        long multiplier = 1L << Math.min(attemptCount - 1, 10);
        return DEFAULT_BASE_DELAY.multipliedBy(multiplier);
    }

    public static boolean isReadyForRetry(
            OffsetDateTime lastAttemptAt, int attemptCount, OffsetDateTime now) {
        if (attemptCount <= 0 || lastAttemptAt == null) {
            return true;
        }
        if (isTerminal(attemptCount)) {
            return false;
        }
        return !lastAttemptAt.plus(delayAfterFailure(attemptCount)).isAfter(now);
    }

    public static boolean isTerminal(int attemptCount) {
        return attemptCount >= MAX_DISPATCH_ATTEMPTS;
    }
}
