package com.lovart.maildesk.common.context;

import java.util.Optional;
import java.util.UUID;

/**
 * Request-scoped user binding (the authenticated {@code profiles.id}). Consumed by
 * {@code AuditFieldFiller} to populate {@code created_by} / {@code updated_by}.
 * Returns {@code null} for anonymous flows (sync workers without an actor, system
 * cron jobs) — callers SHOULD NOT NPE on a missing value.
 * <p>
 * Pair {@link #setUserId(UUID)} with {@link #clear()} in a try / finally.
 */
public final class UserContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUserId(UUID userId) {
        CURRENT.set(userId);
    }

    public static UUID getUserId() {
        return CURRENT.get();
    }

    public static Optional<UUID> tryGetUserId() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
