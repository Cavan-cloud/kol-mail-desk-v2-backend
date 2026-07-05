package com.lovart.maildesk.application.audit;

import com.lovart.maildesk.common.audit.AuditAction;
import com.lovart.maildesk.common.enums.ActionType;
import com.lovart.maildesk.domain.audit.entity.ActionDO;
import com.lovart.maildesk.domain.audit.mapper.ActionMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditActionAspectTest {

    @Mock
    private ActionMapper actions;

    private AuditLogService auditLog;
    private AuditActionAspect aspect;

    @BeforeEach
    void setUp() {
        auditLog = new AuditLogService(actions);
        aspect = new AuditActionAspect(auditLog);
    }

    @Test
    void afterSuccess_evaluatesSpelAndDelegatesToAuditLogService() throws Exception {
        UUID kolId = UUID.randomUUID();
        Method method = SampleService.class.getDeclaredMethod("rename", UUID.class, String.class);
        AuditAction annotation = method.getAnnotation(AuditAction.class);

        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[] {kolId, "Bob"});

        aspect.afterSuccess(joinPoint, annotation, null);

        verify(actions).insert(any(ActionDO.class));
    }

    static class SampleService {
        @AuditAction(
                type = ActionType.STAGE_CHANGE,
                targetType = "kol",
                targetId = "#kolId",
                metadata = "{'field':'name','to':#newName}")
        void rename(UUID kolId, String newName) {
        }
    }
}
