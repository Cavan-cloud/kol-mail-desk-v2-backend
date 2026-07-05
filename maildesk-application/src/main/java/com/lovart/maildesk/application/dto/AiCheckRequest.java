package com.lovart.maildesk.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AiCheckRequest(
        @NotBlank String draft,
        String subject,
        String context
) {
}
