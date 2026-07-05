package com.lovart.maildesk.application.dto;

/**
 * Response body for {@code POST /api/v1/gmail/send}.
 */
public record SendEmailResultDto(
        String status,
        String messageId,
        String message
) {}
