package com.lovart.maildesk.infrastructure.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.lovart.maildesk.common.context.TenantContext;
import com.lovart.maildesk.common.context.UserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Auto-fills {@code created_at} / {@code updated_at} / {@code created_by} /
 * {@code updated_by} / {@code tenant_id} on INSERT and {@code updated_at} /
 * {@code updated_by} on UPDATE.
 * <p>
 * {@code tenant_id} fill deserves a note: every business DO declares
 * {@code tenant_id} as a mapped {@code @TableField(fill = FieldFill.INSERT)}
 * column, so it is ALWAYS present in the generated INSERT statement's column
 * list. That means {@code TenantLineInnerInterceptor}'s own column-injection
 * is a no-op here (its default {@code ignoreInsert} skips columns already
 * present) — filling {@code tenant_id} onto the entity object, here, before
 * parameter binding is what actually supplies the value. Falls back to
 * {@code maildesk.default-tenant-id} (Phase 1 single-tenant) when no
 * {@link TenantContext} is bound, mirroring {@link MaildeskTenantLineHandler}.
 * {@link org.apache.ibatis.reflection.MetaObject#hasSetter(String)} guards
 * entities without a {@code tenantId} property (e.g. {@code tenants} itself).
 * <p>
 * Audit-LOG writes (the {@code actions} table) are deliberately NOT performed
 * here — that lives in the {@code @AuditAction} AOP aspect introduced in
 * Phase 5. Mixing the two responsibilities would tangle row-level metadata with
 * cross-cutting business event recording (see {@code .cursor/rules/backend-java.mdc}
 * § 审计).
 */
@Component
public class AuditFieldFiller implements MetaObjectHandler {

    private static final UUID DEFAULT_TENANT_FALLBACK =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final UUID defaultTenantId;

    public AuditFieldFiller(
            @Value("${maildesk.default-tenant-id:00000000-0000-0000-0000-000000000001}") String defaultTenantId
    ) {
        this.defaultTenantId = parseTenant(defaultTenantId);
    }

    private static UUID parseTenant(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_TENANT_FALLBACK;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            return DEFAULT_TENANT_FALLBACK;
        }
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        OffsetDateTime now = OffsetDateTime.now();
        strictInsertFill(metaObject, "createdAt", OffsetDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", OffsetDateTime.class, now);
        if (metaObject.hasSetter("tenantId")) {
            UUID tenantId = TenantContext.getTenantId();
            strictInsertFill(metaObject, "tenantId", UUID.class,
                    tenantId != null ? tenantId : defaultTenantId);
        }
        UUID actor = UserContext.getUserId();
        if (actor != null) {
            strictInsertFill(metaObject, "createdBy", UUID.class, actor);
            strictInsertFill(metaObject, "updatedBy", UUID.class, actor);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", OffsetDateTime.class, OffsetDateTime.now());
        UUID actor = UserContext.getUserId();
        if (actor != null) {
            strictUpdateFill(metaObject, "updatedBy", UUID.class, actor);
        }
    }
}
