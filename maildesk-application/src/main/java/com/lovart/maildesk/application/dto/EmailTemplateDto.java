package com.lovart.maildesk.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EmailTemplateDto(
        UUID id,
        String name,
        String scenario,
        String subject,
        String body,
        int usedCount,
        OffsetDateTime lastUsedAt,
        UUID createdBy,
        OffsetDateTime createdAt
) {
}
