package com.lovart.maildesk.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Team role. Mirrors the {@code profiles.role} CHECK constraint declared in
 * {@code V4__init_profiles.sql} ({@code 'member' | 'leader' | 'full_time' | 'intern'}).
 * <p>
 * The OpenAPI contract exposes {@code 'leader' | 'full_time' | 'intern'};
 * {@link #MEMBER} is the legacy default used during the first login flow until
 * the user picks one of the public roles via the profile form (P1-T05).
 */
public enum UserRole {
    MEMBER,
    LEADER,
    FULL_TIME,
    INTERN;

    @JsonValue
    public String dbValue() {
        return switch (this) {
            case MEMBER -> "member";
            case LEADER -> "leader";
            case FULL_TIME -> "full_time";
            case INTERN -> "intern";
        };
    }

    @JsonCreator
    public static UserRole fromApiValue(String raw) {
        return fromDbValue(raw);
    }

    public static UserRole fromDbValue(String raw) {
        if (raw == null) {
            return MEMBER;
        }
        return switch (raw) {
            case "leader" -> LEADER;
            case "full_time" -> FULL_TIME;
            case "intern" -> INTERN;
            default -> MEMBER;
        };
    }
}
