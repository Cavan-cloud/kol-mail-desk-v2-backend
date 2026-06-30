package com.lovart.maildesk.infrastructure.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.lovart.maildesk.common.context.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Feeds the active tenant UUID into MyBatis-Plus's
 * {@code TenantLineInnerInterceptor}, which then injects {@code tenant_id} into
 * every SELECT / UPDATE / DELETE / INSERT statement.
 * <p>
 * Resolution order:
 * <ol>
 *   <li>{@link TenantContext#getTenantId()} (set by the inbound filter / worker
 *       job entry point)</li>
 *   <li>{@code DEFAULT_TENANT_ID} fallback (matches the V3 seeded dev tenant
 *       {@code 00000000-0000-0000-0000-000000000001}) — Phase 1 is single-tenant
 *       so this fallback covers cron jobs, smoke tests, and any request that
 *       legitimately runs without a tenant header.</li>
 * </ol>
 *
 * UUID values are returned as a {@link StringValue} (a JSQLParser string
 * literal): PostgreSQL implicitly coerces {@code 'xxxxxxxx-...'} to
 * {@code uuid}, so we sidestep the JDBC parameter typing dance.
 */
public class MaildeskTenantLineHandler implements TenantLineHandler {

    private final UUID defaultTenantId;
    private final Set<String> ignoredTables;

    public MaildeskTenantLineHandler(UUID defaultTenantId, Set<String> ignoredTables) {
        this.defaultTenantId = defaultTenantId;
        Set<String> lower = new HashSet<>();
        for (String t : ignoredTables) {
            lower.add(t.toLowerCase(Locale.ROOT));
        }
        this.ignoredTables = Collections.unmodifiableSet(lower);
    }

    @Override
    public Expression getTenantId() {
        UUID id = TenantContext.getTenantId();
        if (id == null) {
            id = defaultTenantId;
        }
        return new StringValue(id.toString());
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if (tableName == null) {
            return true;
        }
        return ignoredTables.contains(tableName.toLowerCase(Locale.ROOT));
    }
}
