package com.lovart.maildesk.common.enums;

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
    DECLINED
}
