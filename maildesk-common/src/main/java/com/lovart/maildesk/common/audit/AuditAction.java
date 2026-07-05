package com.lovart.maildesk.common.audit;

import com.lovart.maildesk.common.enums.ActionType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative audit hook for application-service write methods. An AOP aspect
 * records a row in {@code actions} after a successful return when
 * {@link #afterSuccessOnly()} is {@code true}.
 * <p>
 * {@link #targetId()} and {@link #metadata()} are Spring EL expressions evaluated
 * against method parameter names (e.g. {@code "#kolId"}, {@code "#request.name"}).
 * For conditional or multi-row audits, inject {@code AuditLogService} directly.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditAction {

    ActionType type();

    String targetType();

    /** SpEL for {@code actions.target_id}; omit when not applicable. */
    String targetId() default "";

    /** SpEL returning a {@code Map} for {@code actions.metadata}; omit when empty. */
    String metadata() default "";

    boolean afterSuccessOnly() default true;
}
