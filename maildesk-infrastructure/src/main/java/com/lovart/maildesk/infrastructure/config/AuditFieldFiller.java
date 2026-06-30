package com.lovart.maildesk.infrastructure.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.lovart.maildesk.common.context.UserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Auto-fills {@code created_at} / {@code updated_at} / {@code created_by} /
 * {@code updated_by} on INSERT and {@code updated_at} / {@code updated_by} on
 * UPDATE.
 * <p>
 * Audit-LOG writes (the {@code actions} table) are deliberately NOT performed
 * here — that lives in the {@code @AuditAction} AOP aspect introduced in
 * Phase 5. Mixing the two responsibilities would tangle row-level metadata with
 * cross-cutting business event recording (see {@code .cursor/rules/backend-java.mdc}
 * § 审计).
 */
@Component
public class AuditFieldFiller implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        OffsetDateTime now = OffsetDateTime.now();
        strictInsertFill(metaObject, "createdAt", OffsetDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", OffsetDateTime.class, now);
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
