package com.lovart.maildesk.application.dto;

public record BoardFunnelStageDto(
        String stage,
        String label,
        int count
) {
}
