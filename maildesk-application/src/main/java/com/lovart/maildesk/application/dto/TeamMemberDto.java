package com.lovart.maildesk.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TeamMemberDto(
        UUID id,
        String displayName,
        String email,
        String role,
        String status,
        UUID mentorUserId,
        String feishuOperatorName,
        boolean gmailAuthorized,
        OffsetDateTime lastSyncedAt,
        OffsetDateTime createdAt,
        int ownedKolCount,
        int activeKolCount,
        int closedKolCount,
        int stalledKolCount
) {
}
