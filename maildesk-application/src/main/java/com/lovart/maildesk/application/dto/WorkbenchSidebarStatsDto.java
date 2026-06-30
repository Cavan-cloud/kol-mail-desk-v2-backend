package com.lovart.maildesk.application.dto;

import java.util.Map;

public record WorkbenchSidebarStatsDto(
        int total,
        int unread,
        int unreplied,
        Map<String, Integer> stageCounts
) {
}
