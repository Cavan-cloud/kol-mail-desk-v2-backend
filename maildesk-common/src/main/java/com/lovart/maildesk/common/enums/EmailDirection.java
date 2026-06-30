package com.lovart.maildesk.common.enums;

/**
 * Mail flow direction relative to the platform user.
 * <p>
 * Mirrors the {@code email_direction} PostgreSQL ENUM declared in
 * {@code V2__init_enums.sql}.
 */
public enum EmailDirection {
    INBOUND,
    OUTBOUND
}
