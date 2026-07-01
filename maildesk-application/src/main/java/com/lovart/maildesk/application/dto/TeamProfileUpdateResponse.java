package com.lovart.maildesk.application.dto;

/**
 * Result of {@code PATCH /api/v1/team/profile} including KOL auto-assignment count.
 */
public record TeamProfileUpdateResponse(
        ProfileDto profile,
        int kolsAssigned) {
}
