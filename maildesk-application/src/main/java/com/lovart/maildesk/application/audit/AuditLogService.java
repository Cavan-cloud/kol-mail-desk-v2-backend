package com.lovart.maildesk.application.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovart.maildesk.common.context.UserContext;
import com.lovart.maildesk.common.enums.ActionType;
import com.lovart.maildesk.domain.audit.entity.ActionDO;
import com.lovart.maildesk.domain.audit.mapper.ActionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Append-only writer for the {@code actions} audit table.
 */
@Service
public class AuditLogService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ActionMapper actions;

    public AuditLogService(ActionMapper actions) {
        this.actions = actions;
    }

    @Transactional(rollbackFor = Exception.class)
    public void append(ActionType type, String targetType, UUID targetId, Map<String, ?> metadata) {
        ActionDO row = new ActionDO();
        row.setActorUserId(UserContext.getUserId());
        row.setActionType(type);
        row.setTargetType(targetType);
        row.setTargetId(targetId);
        if (metadata != null && !metadata.isEmpty()) {
            row.setMetadata(MAPPER.valueToTree(metadata));
        }
        actions.insert(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public void append(ActionType type, String targetType, UUID targetId) {
        append(type, targetType, targetId, null);
    }

    static JsonNode toJsonNode(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JsonNode node) {
            return node;
        }
        return MAPPER.valueToTree(value);
    }
}
