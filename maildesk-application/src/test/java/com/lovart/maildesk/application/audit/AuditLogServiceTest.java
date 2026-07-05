package com.lovart.maildesk.application.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.lovart.maildesk.common.context.UserContext;
import com.lovart.maildesk.common.enums.ActionType;
import com.lovart.maildesk.domain.audit.entity.ActionDO;
import com.lovart.maildesk.domain.audit.mapper.ActionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private ActionMapper actions;

    private AuditLogService service;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        service = new AuditLogService(actions);
        actorId = UUID.randomUUID();
        UserContext.setUserId(actorId);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void append_persistsActorTypeTargetAndMetadata() {
        UUID targetId = UUID.randomUUID();

        service.append(
                ActionType.STAGE_CHANGE,
                "kol",
                targetId,
                Map.of("field", "name", "from", "Alice", "to", "Bob"));

        ArgumentCaptor<ActionDO> captor = ArgumentCaptor.forClass(ActionDO.class);
        verify(actions).insert(captor.capture());
        ActionDO row = captor.getValue();
        assertThat(row.getActorUserId()).isEqualTo(actorId);
        assertThat(row.getActionType()).isEqualTo(ActionType.STAGE_CHANGE);
        assertThat(row.getTargetType()).isEqualTo("kol");
        assertThat(row.getTargetId()).isEqualTo(targetId);
        JsonNode metadata = row.getMetadata();
        assertThat(metadata.get("field").asText()).isEqualTo("name");
        assertThat(metadata.get("to").asText()).isEqualTo("Bob");
    }
}
