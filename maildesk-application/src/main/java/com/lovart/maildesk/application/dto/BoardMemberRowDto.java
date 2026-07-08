package com.lovart.maildesk.application.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BoardMemberRowDto(
        UUID memberId,
        String displayName,
        String role,
        List<UUID> coveredMemberIds,
        Map<String, Integer> stageCounts,
        int total,
        int unread,
        int unreplied
) {
}
