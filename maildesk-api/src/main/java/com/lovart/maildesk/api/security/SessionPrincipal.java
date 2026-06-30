package com.lovart.maildesk.api.security;

import com.lovart.maildesk.infrastructure.session.SessionInfo;

import java.util.UUID;

/**
 * Lightweight principal exposed to controllers through {@link org.springframework.security.core.Authentication#getPrincipal()}.
 * Carries the same fields as the persisted session record.
 */
public record SessionPrincipal(
        UUID userId,
        UUID tenantId,
        String email,
        String displayName,
        String role,
        String sessionToken
) {

    public static SessionPrincipal fromSession(SessionInfo info) {
        return new SessionPrincipal(
                info.userId(),
                info.tenantId(),
                info.email(),
                info.displayName(),
                info.role(),
                info.token()
        );
    }
}
