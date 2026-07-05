package com.lovart.maildesk.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lovart.maildesk.common.enums.KolStage;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code PATCH /api/v1/kols/{kolId}}.
 * Send only the fields to update; at least one must be present.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KolUpdateRequest(
        @Size(min = 1, max = 120, message = "达人名长度需在 1～120 字之间")
        String name,
        KolStage stage,
        Boolean replyResolved
) {
}
