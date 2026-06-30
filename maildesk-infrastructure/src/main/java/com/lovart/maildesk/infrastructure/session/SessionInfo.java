package com.lovart.maildesk.infrastructure.session;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Server-side session record. Fields are kept minimal so the JSON value stays
 * cheap to (de)serialize; richer profile data is fetched from {@code profiles}
 * on demand.
 */
public record SessionInfo(
        String token,
        UUID userId,
        UUID tenantId,
        String email,
        String displayName,
        String role,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt
) {
}
