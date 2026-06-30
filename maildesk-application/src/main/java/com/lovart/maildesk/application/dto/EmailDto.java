package com.lovart.maildesk.application.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EmailDto(
        UUID id,
        UUID kolId,
        UUID userId,
        String direction,
        String fromEmail,
        List<String> toEmails,
        List<String> ccEmails,
        String subject,
        String bodyText,
        String bodyHtml,
        String bodyZh,
        boolean hasAttachments,
        List<String> attachmentNames,
        OffsetDateTime sentAt,
        String aiStageSignal,
        String aiPriority,
        String aiSummary,
        String aiSuggestedAction,
        boolean isRead,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {
}
