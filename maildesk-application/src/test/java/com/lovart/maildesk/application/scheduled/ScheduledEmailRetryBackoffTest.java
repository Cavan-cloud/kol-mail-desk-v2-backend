package com.lovart.maildesk.application.scheduled;

import com.lovart.maildesk.domain.scheduled.ScheduledEmailRetryBackoff;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduledEmailRetryBackoffTest {

    @Test
    void delayAfterFailure_usesExponentialMinutes() {
        assertThat(ScheduledEmailRetryBackoff.delayAfterFailure(0)).isEqualTo(Duration.ZERO);
        assertThat(ScheduledEmailRetryBackoff.delayAfterFailure(1)).isEqualTo(Duration.ofMinutes(1));
        assertThat(ScheduledEmailRetryBackoff.delayAfterFailure(2)).isEqualTo(Duration.ofMinutes(2));
        assertThat(ScheduledEmailRetryBackoff.delayAfterFailure(3)).isEqualTo(Duration.ofMinutes(4));
    }

    @Test
    void isReadyForRetry_respectsBackoffWindow() {
        OffsetDateTime lastAttempt = OffsetDateTime.parse("2026-07-03T10:00:00Z");
        assertThat(ScheduledEmailRetryBackoff.isReadyForRetry(lastAttempt, 1, lastAttempt.plusMinutes(1)))
                .isTrue();
        assertThat(ScheduledEmailRetryBackoff.isReadyForRetry(lastAttempt, 1, lastAttempt.plusSeconds(30)))
                .isFalse();
    }

    @Test
    void isTerminal_whenAttemptCountReachedCap() {
        assertThat(ScheduledEmailRetryBackoff.isTerminal(3)).isTrue();
        assertThat(ScheduledEmailRetryBackoff.isTerminal(2)).isFalse();
    }
}
