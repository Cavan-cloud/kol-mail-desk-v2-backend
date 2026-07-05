package com.lovart.maildesk.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Append-only audit event type. Mirrors PostgreSQL {@code action_type} (V2).
 */
public enum ActionType {
    STAGE_CHANGE,
    OWNER_CHANGE,
    EMAIL_SENT,
    EMAIL_READ,
    KOL_CLAIMED,
    USER_APPROVED,
    USER_DEPARTED,
    TEMPLATE_USED,
    SYNC_STARTED,
    SYNC_FAILED;

    @JsonValue
    public String dbValue() {
        return switch (this) {
            case STAGE_CHANGE -> "stage_change";
            case OWNER_CHANGE -> "owner_change";
            case EMAIL_SENT -> "email_sent";
            case EMAIL_READ -> "email_read";
            case KOL_CLAIMED -> "kol_claimed";
            case USER_APPROVED -> "user_approved";
            case USER_DEPARTED -> "user_departed";
            case TEMPLATE_USED -> "template_used";
            case SYNC_STARTED -> "sync_started";
            case SYNC_FAILED -> "sync_failed";
        };
    }

    @JsonCreator
    public static ActionType fromDbValue(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw) {
            case "stage_change" -> STAGE_CHANGE;
            case "owner_change" -> OWNER_CHANGE;
            case "email_sent" -> EMAIL_SENT;
            case "email_read" -> EMAIL_READ;
            case "kol_claimed" -> KOL_CLAIMED;
            case "user_approved" -> USER_APPROVED;
            case "user_departed" -> USER_DEPARTED;
            case "template_used" -> TEMPLATE_USED;
            case "sync_started" -> SYNC_STARTED;
            case "sync_failed" -> SYNC_FAILED;
            default -> throw new IllegalArgumentException("Unknown action_type: " + raw);
        };
    }
}
