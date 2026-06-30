package com.lovart.maildesk.application.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ScheduledEmailDto(
        UUID id,
        UUID kolId,
        String kolName,
        UUID templateId,
        String toEmail,
        List<String> ccEmails,
        String subject,
        String englishBody,
        String englishBodyHtml,
        String chineseDraft,
        OffsetDateTime scheduledAt,
        String status,
        int attemptCount,
        String lastError,
        OffsetDateTime createdAt
) {
}
