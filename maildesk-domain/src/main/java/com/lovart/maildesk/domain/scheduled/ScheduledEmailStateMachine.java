package com.lovart.maildesk.domain.scheduled;

import com.lovart.maildesk.common.enums.ScheduledEmailStatus;

/**
 * Legal {@code scheduled_emails.status} transitions for API guards and worker dispatch.
 */
public final class ScheduledEmailStateMachine {

    /** Worker may re-claim rows until this many attempts have been recorded. */
    public static final int MAX_DISPATCH_ATTEMPTS = 3;

    private ScheduledEmailStateMachine() {}

    public static boolean canCancel(ScheduledEmailStatus status) {
        return status == ScheduledEmailStatus.SCHEDULED;
    }

    public static boolean canClaim(ScheduledEmailStatus status, int attemptCount) {
        if (attemptCount >= MAX_DISPATCH_ATTEMPTS) {
            return false;
        }
        return status == ScheduledEmailStatus.SCHEDULED || status == ScheduledEmailStatus.FAILED;
    }
}
