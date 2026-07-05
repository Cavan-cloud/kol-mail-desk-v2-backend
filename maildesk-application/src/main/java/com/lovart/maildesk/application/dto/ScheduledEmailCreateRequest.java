package com.lovart.maildesk.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ScheduledEmailCreateRequest(
        @NotNull UUID kolId,
        UUID templateId,
        @NotBlank String to,
        List<String> ccEmails,
        @NotBlank String subject,
        @NotBlank String englishBody,
        String englishBodyHtml,
        String chineseDraft,
        @NotNull OffsetDateTime scheduledAt
) {
}
