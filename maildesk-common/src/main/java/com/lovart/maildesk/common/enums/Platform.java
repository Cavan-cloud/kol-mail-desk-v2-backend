package com.lovart.maildesk.common.enums;

/**
 * Content platform slug stored in the PostgreSQL {@code platform} ENUM.
 */
public enum Platform {
    TIKTOK,
    INSTAGRAM,
    YOUTUBE,
    X,
    OTHER;

    public String dbValue() {
        return name().toLowerCase();
    }

    public static Platform fromDbValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return valueOf(raw.trim().toUpperCase());
    }
}
