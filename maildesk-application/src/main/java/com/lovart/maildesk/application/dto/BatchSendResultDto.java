package com.lovart.maildesk.application.dto;

import java.util.List;

/**
 * Response body for {@code POST /api/v1/gmail/batch-send}.
 */
public record BatchSendResultDto(
        int successCount,
        int failedCount,
        List<SendEmailResultDto> results
) {}
