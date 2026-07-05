package com.lovart.maildesk.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AiTranslateRequest(
        @NotBlank String text,
        String targetLang
) {
}
