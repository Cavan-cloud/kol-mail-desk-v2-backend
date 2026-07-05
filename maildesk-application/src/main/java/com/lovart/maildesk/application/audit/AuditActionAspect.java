package com.lovart.maildesk.application.audit;

import com.lovart.maildesk.common.audit.AuditAction;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

/**
 * Records {@link AuditAction}-annotated application-service calls into {@code actions}.
 */
@Aspect
@Component
public class AuditActionAspect {

    private final AuditLogService auditLog;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNames = new DefaultParameterNameDiscoverer();

    public AuditActionAspect(AuditLogService auditLog) {
        this.auditLog = auditLog;
    }

    @AfterReturning(pointcut = "@annotation(auditAction)", returning = "result")
    public void afterSuccess(JoinPoint joinPoint, AuditAction auditAction, Object result) {
        if (!auditAction.afterSuccessOnly()) {
            return;
        }
        record(joinPoint, auditAction, result);
    }

    private void record(JoinPoint joinPoint, AuditAction auditAction, Object result) {
        EvaluationContext context = buildContext(joinPoint, result);
        UUID targetId = evaluateUuid(auditAction.targetId(), context);
        Map<String, ?> metadata = evaluateMetadata(auditAction.metadata(), context);
        auditLog.append(auditAction.type(), auditAction.targetType(), targetId, metadata);
    }

    private EvaluationContext buildContext(JoinPoint joinPoint, Object result) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] names = parameterNames.getParameterNames(method);

        StandardEvaluationContext context = new StandardEvaluationContext();
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                context.setVariable(names[i], args[i]);
            }
        }
        context.setVariable("result", result);
        return context;
    }

    private UUID evaluateUuid(String expression, EvaluationContext context) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        Object value = parser.parseExpression(expression).getValue(context);
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> evaluateMetadata(String expression, EvaluationContext context) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        Object value = parser.parseExpression(expression).getValue(context);
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            return (Map<String, ?>) map;
        }
        throw new IllegalStateException("@AuditAction metadata SpEL must evaluate to Map, got: " + value.getClass());
    }
}
