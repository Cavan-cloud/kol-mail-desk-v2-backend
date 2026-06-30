package com.lovart.maildesk.application.dto;

public record BoardKpiDto(
        int totalKols,
        int activeKols,
        int publishedKols,
        int paidKols,
        float conversionRate
) {
}
