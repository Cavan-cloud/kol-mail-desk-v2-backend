package com.lovart.maildesk.common.enums;

/**
 * Account lifecycle status (mirrors {@code profiles.status} CHECK in V4):
 * {@code pending_approval}, {@code active}, {@code departed}.
 * <p>
 * P1-T04 lands new OAuth users as {@link #PENDING_APPROVAL}; activation happens
 * in P1-T05 when the user completes the profile form.
 */
public enum UserStatus {
    PENDING_APPROVAL,
    ACTIVE,
    DEPARTED;

    public String dbValue() {
        return switch (this) {
            case PENDING_APPROVAL -> "pending_approval";
            case ACTIVE -> "active";
            case DEPARTED -> "departed";
        };
    }

    public static UserStatus fromDbValue(String raw) {
        if (raw == null) {
            return PENDING_APPROVAL;
        }
        return switch (raw) {
            case "active" -> ACTIVE;
            case "departed" -> DEPARTED;
            default -> PENDING_APPROVAL;
        };
    }
}
