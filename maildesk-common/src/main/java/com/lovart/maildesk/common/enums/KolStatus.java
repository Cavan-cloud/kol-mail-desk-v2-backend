package com.lovart.maildesk.common.enums;

/**
 * KOL ownership lifecycle status.
 * <p>
 * Values mirror the {@code kol_status} PostgreSQL ENUM declared in
 * {@code V2__init_enums.sql}. The DB stores lower-case labels; conversion is
 * handled by {@code KolStatusTypeHandler}.
 */
public enum KolStatus {
    ACTIVE,
    UNASSIGNED,
    ORPHANED,
    CLOSED;

    public String dbValue() {
        return name().toLowerCase();
    }

    public static KolStatus fromDbValue(String raw) {
        return valueOf(raw.toUpperCase());
    }
}
