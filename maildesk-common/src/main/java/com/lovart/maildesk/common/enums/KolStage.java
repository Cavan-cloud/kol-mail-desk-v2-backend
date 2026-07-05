package com.lovart.maildesk.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * KOL outreach / negotiation funnel stage.
 * <p>
 * Values mirror the {@code kol_stage} PostgreSQL ENUM declared in
 * {@code V2__init_enums.sql}. The DB stores lower-case labels; conversion is
 * handled by {@code KolStageTypeHandler}.
 */
public enum KolStage {
    OUTREACH,
    REPLIED,
    NEGOTIATING,
    CONFIRMED,
    PRODUCING,
    REVIEWING,
    PUBLISHED,
    PAYING,
    REINVEST,
    DECLINED;

    @JsonValue
    public String apiValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static KolStage fromApiValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
