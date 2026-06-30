package com.lovart.maildesk.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkbenchKolDto(
        UUID id,
        String email,
        String name,
        String handle,
        String primaryPlatform,
        String type,
        String externalProfileUrl,
        String source,
        String feishuRecordId,
        OffsetDateTime feishuOutreachAt,
        String stage,
        String status,
        UUID ownerUserId,
        OffsetDateTime lastInboundAt,
        OffsetDateTime lastOutboundAt,
        BigDecimal agreedPrice,
        LocalDate agreedDeadline,
        String notes,
        boolean replyResolved,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String ownerName,
        EmailDto latestEmail,
        int unreadCount,
        boolean unreplied,
        boolean awaitingReply
) {
}
