package com.lovart.maildesk.application.scheduled;

import com.lovart.maildesk.common.enums.ScheduledEmailStatus;
import com.lovart.maildesk.domain.scheduled.ScheduledEmailStateMachine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduledEmailStateMachineTest {

    @Test
    void canCancel_onlyWhenScheduled() {
        assertTrue(ScheduledEmailStateMachine.canCancel(ScheduledEmailStatus.SCHEDULED));
        assertFalse(ScheduledEmailStateMachine.canCancel(ScheduledEmailStatus.PROCESSING));
        assertFalse(ScheduledEmailStateMachine.canCancel(ScheduledEmailStatus.SENT));
        assertFalse(ScheduledEmailStateMachine.canCancel(ScheduledEmailStatus.CANCELLED));
        assertFalse(ScheduledEmailStateMachine.canCancel(ScheduledEmailStatus.FAILED));
    }

    @Test
    void canClaim_scheduledOrFailedUnderAttemptCap() {
        assertTrue(ScheduledEmailStateMachine.canClaim(ScheduledEmailStatus.SCHEDULED, 0));
        assertTrue(ScheduledEmailStateMachine.canClaim(ScheduledEmailStatus.FAILED, 2));
        assertFalse(ScheduledEmailStateMachine.canClaim(ScheduledEmailStatus.FAILED, 3));
        assertFalse(ScheduledEmailStateMachine.canClaim(ScheduledEmailStatus.PROCESSING, 0));
        assertFalse(ScheduledEmailStateMachine.canClaim(ScheduledEmailStatus.SENT, 0));
    }
}
