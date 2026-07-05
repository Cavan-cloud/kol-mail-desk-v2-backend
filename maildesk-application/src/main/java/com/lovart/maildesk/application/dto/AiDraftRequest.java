package com.lovart.maildesk.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AiDraftRequest(
        @NotBlank String kolName,
        String senderName,
        @NotBlank String latestEmail,
        String history,
        String templateHint,
        String kolStage,
        String kolPlatform,
        Double kolAgreedPrice
) {
}
