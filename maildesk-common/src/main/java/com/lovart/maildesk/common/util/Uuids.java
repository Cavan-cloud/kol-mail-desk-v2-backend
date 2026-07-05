package com.lovart.maildesk.common.util;

import java.util.UUID;

/**
 * UUID parsing helpers shared by TypeHandlers and DO setters.
 * <p>
 * MyBatis-Plus {@code IdType.ASSIGN_UUID} generates a 32-char hex string
 * (no hyphens). Entity {@code id} fields are {@link UUID}, so {@code setId}
 * must accept both forms.
 */
public final class Uuids {

    private Uuids() {
    }

    public static UUID parse(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return parseString(value.toString());
    }

    public static UUID parseString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() == 32 && !trimmed.contains("-")) {
            return fromCompactHex(trimmed);
        }
        return UUID.fromString(trimmed);
    }

    private static UUID fromCompactHex(String hex32) {
        return UUID.fromString(
                hex32.substring(0, 8) + '-'
                        + hex32.substring(8, 12) + '-'
                        + hex32.substring(12, 16) + '-'
                        + hex32.substring(16, 20) + '-'
                        + hex32.substring(20, 32));
    }
}
