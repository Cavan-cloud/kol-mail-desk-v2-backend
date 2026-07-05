package com.lovart.maildesk.domain.credential;

import java.util.Optional;
import java.util.UUID;

/**
 * Loads and refreshes per-user Google OAuth credentials stored in
 * {@code integration_credentials}.
 */
public interface GoogleCredentialPort {

    Optional<GoogleAccessToken> resolveAccessToken(UUID userId);

    boolean hasCredential(UUID userId);

    /** Whether stored OAuth scopes include {@code gmail.send} (required for outbound mail). */
    boolean hasGmailSendScope(UUID userId);
}
