package com.lovart.maildesk.infrastructure.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.lovart.maildesk.common.typehandler.EmailDirectionTypeHandler;
import com.lovart.maildesk.common.typehandler.JsonbTypeHandler;
import com.lovart.maildesk.common.typehandler.KolStageTypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Central MyBatis-Plus wiring for the platform.
 * <p>
 * Interceptor ORDER MATTERS (MP applies them outside-in, i.e. last-added runs
 * first against the parsed SQL). We follow the framework-recommended chain:
 * <ol>
 *   <li>{@link TenantLineInnerInterceptor} — must wrap the SQL before pagination
 *       so the {@code COUNT(*)} statement also carries the tenant filter.</li>
 *   <li>{@link PaginationInnerInterceptor} — PG dialect for {@code LIMIT/OFFSET}
 *       (and a real {@code COUNT(*)} fallback for boards / workbench).</li>
 *   <li>{@link OptimisticLockerInnerInterceptor} — appends
 *       {@code AND version = ?} and sets {@code version = version + 1}.</li>
 *   <li>{@link BlockAttackInnerInterceptor} — refuses {@code UPDATE} /
 *       {@code DELETE} without a {@code WHERE} clause; the very last layer so
 *       it blocks the final rewritten statement.</li>
 * </ol>
 *
 * TypeHandlers are registered via {@link ConfigurationCustomizer} into the
 * shared {@link TypeHandlerRegistry}. This avoids forcing every DO field to
 * carry an explicit {@code @TableField(typeHandler = ...)}. Generic-collection
 * handlers ({@code StringArrayTypeHandler}) still need a field-level hint plus
 * {@code @TableName(autoResultMap = true)}, because Java erasure hides the
 * element type.
 */
@Configuration
public class MyBatisPlusConfig {

    private static final UUID DEFAULT_TENANT_FALLBACK =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    /**
     * Tables that have no {@code tenant_id} column or are operational metadata.
     * The handler short-circuits on these so MP does not try to inject
     * {@code WHERE tenant_id = ?} against them.
     */
    private static final List<String> TENANT_EXEMPT_TABLES = List.of(
            "tenants",
            "flyway_schema_history"
    );

    private final UUID defaultTenantId;

    public MyBatisPlusConfig(
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

    @Bean
    public MaildeskTenantLineHandler tenantLineHandler() {
        Set<String> exempt = new LinkedHashSet<>(TENANT_EXEMPT_TABLES);
        return new MaildeskTenantLineHandler(defaultTenantId, exempt);
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(MaildeskTenantLineHandler tenantLineHandler) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    @Bean
    public ConfigurationCustomizer maildeskTypeHandlerCustomizer() {
        return configuration -> {
            TypeHandlerRegistry registry = configuration.getTypeHandlerRegistry();
            registry.register(new JsonbTypeHandler());
            registry.register(new KolStageTypeHandler());
            registry.register(new EmailDirectionTypeHandler());
            // StringArrayTypeHandler is intentionally NOT registered globally:
            // Java erases List<String> to raw List, which collides with MyBatis's
            // built-in collection mappers. Attach it per-field via
            // @TableField(typeHandler = StringArrayTypeHandler.class) on DOs that
            // own a text[] column (see EmailDO).
        };
    }
}
