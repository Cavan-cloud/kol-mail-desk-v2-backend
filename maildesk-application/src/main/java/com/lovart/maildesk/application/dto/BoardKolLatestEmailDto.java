package com.lovart.maildesk.application.dto;

import java.time.OffsetDateTime;

public record BoardKolLatestEmailDto(
        String subject,
        String aiSummary,
        String aiPriority,
        String direction,
        OffsetDateTime sentAt
) {
}
