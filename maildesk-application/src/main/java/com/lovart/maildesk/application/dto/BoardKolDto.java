package com.lovart.maildesk.application.dto;

import java.util.UUID;

public record BoardKolDto(
        UUID id,
        String name,
        String email,
        String stage,
        String primaryPlatform,
        String type,
        int unreadCount,
        boolean unreplied,
        BoardKolLatestEmailDto latestEmail
) {
}
