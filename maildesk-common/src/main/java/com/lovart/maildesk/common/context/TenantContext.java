package com.lovart.maildesk.common.context;

import java.util.Optional;
import java.util.UUID;

/**
 * Request-scoped tenant binding. Populated by an inbound filter (api) or by the
 * worker job entry point from the message payload; consumed by MyBatis-Plus's
 * {@code TenantLineInnerInterceptor} to inject {@code tenant_id} into every
 * statement.
 * <p>
 * Callers MUST always pair {@link #setTenantId(UUID)} with {@link #clear()} in a
 * try / finally — leaked thread-locals on pooled threads will bleed across requests.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(UUID tenantId) {
        CURRENT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT.get();
    }

    public static Optional<UUID> tryGetTenantId() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
