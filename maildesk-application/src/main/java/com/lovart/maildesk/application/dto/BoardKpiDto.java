package com.lovart.maildesk.application.dto;

public record BoardKpiDto(
        int totalKols,
        int unrepliedKols,
        int unreadEmails,
        int cooperationKols,
        float conversionRate
) {
}
