package com.lovart.maildesk.ai.classify;

import com.lovart.maildesk.common.enums.EmailDirection;

/**
 * Input for {@link com.lovart.maildesk.ai.AiService#classifyEmail(ClassifyEmailRequest)}.
 */
public record ClassifyEmailRequest(
        EmailDirection direction,
        String subject,
        String body,
        String history) {

    public ClassifyEmailRequest {
        if (direction == null) {
            throw new IllegalArgumentException("direction is required");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("body is required");
        }
    }
}
