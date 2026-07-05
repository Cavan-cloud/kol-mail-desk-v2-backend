package com.lovart.maildesk.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * {@code scheduled_emails.status} lifecycle (mirrors V9 CHECK constraint).
 */
public enum ScheduledEmailStatus {
    SCHEDULED,
    PROCESSING,
    SENT,
    CANCELLED,
    FAILED;

    @JsonValue
    public String dbValue() {
        return switch (this) {
            case SCHEDULED -> "scheduled";
            case PROCESSING -> "processing";
            case SENT -> "sent";
            case CANCELLED -> "cancelled";
            case FAILED -> "failed";
        };
    }

    @JsonCreator
    public static ScheduledEmailStatus fromDbValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return SCHEDULED;
        }
        return switch (raw) {
            case "scheduled" -> SCHEDULED;
            case "processing" -> PROCESSING;
            case "sent" -> SENT;
            case "cancelled" -> CANCELLED;
            case "failed" -> FAILED;
            default -> throw new IllegalArgumentException("Unknown scheduled_emails.status: " + raw);
        };
    }
}
